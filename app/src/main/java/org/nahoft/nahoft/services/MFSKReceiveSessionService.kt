package org.nahoft.nahoft.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity
import org.nahoft.nahoft.models.DecryptedMessageRecord
import org.nahoft.nahoft.models.Message
import org.operatorfoundation.audiocoder.mfsk.MFSKConfiguration
import org.operatorfoundation.audiocoder.mfsk.MFSKMode
import org.operatorfoundation.audiocoder.mfsk.MFSKStation
import org.operatorfoundation.signalbridge.UsbAudioConnection
import org.operatorfoundation.signalbridge.UsbAudioDeviceMonitor
import org.operatorfoundation.signalbridge.UsbAudioManager
import org.operatorfoundation.signalbridge.asAudioFlow
import org.operatorfoundation.signalbridge.models.AudioLevelInfo
import timber.log.Timber

/**
 * Foreground service managing MFSK radio receive sessions.
 *
 * Survives Activity lifecycle, maintains USB audio connection and MFSK decoding
 * across configuration changes and backgrounding. Shows persistent notification
 * with session progress.
 *
 * Receive pipeline per decoded message:
 *   MFSKStation.messages → decrypt with friend public key → save → emit
 *
 * Decryption failures (corrupted audio, noise) are logged and silently skipped —
 * [MFSKStation] only emits when its framing is complete, so a decryption failure
 * means the payload is not a valid Nahoft ciphertext rather than a partial receive.
 *
 * The session-level timeout ([SESSION_TIMEOUT_MS]) is distinct from [MFSKConfiguration.timeoutMs],
 * which controls per-message decode timeouts inside [MFSKStation]. The session timeout
 * only runs while the app is backgrounded.
 *
 * Usage:
 * - Start with ACTION_START_SESSION and required extras
 * - Bind to observe StateFlows
 * - Stop with ACTION_STOP_SESSION
 */
class MFSKReceiveSessionService : Service()
{
    companion object
    {
        const val ACTION_START_SESSION  = "org.nahoft.nahoft.action.MFSK_RX_START_SESSION"
        const val ACTION_STOP_SESSION   = "org.nahoft.nahoft.action.MFSK_RX_STOP_SESSION"
        const val ACTION_EXTEND_SESSION = "org.nahoft.nahoft.action.MFSK_RX_EXTEND_SESSION"

        const val EXTRA_FRIEND_NAME       = "mfsk_rx_friend_name"
        const val EXTRA_FRIEND_PUBLIC_KEY = "mfsk_rx_friend_public_key"
        const val EXTRA_MODE_LABEL        = "mfsk_rx_mode_label"
        const val EXTRA_BASE_FREQUENCY_HZ = "mfsk_rx_base_frequency_hz"

        /**
         * How long the service runs while backgrounded before auto-stopping.
         * Distinct from [MFSKConfiguration.timeoutMs], which is the per-message
         * decode timeout inside [MFSKStation].
         */
        private const val SESSION_TIMEOUT_MS     = 20 * 60 * 1000L  // 20 minutes
        private const val TIMEOUT_WARNING_MS      = 3 * 60 * 1000L   // 3 minutes before timeout
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1000L

        private const val NOTIFICATION_CHANNEL_ID = "nahoft_mfsk_receive_session"
        private const val WARNING_CHANNEL_ID       = "nahoft_mfsk_session_warning"
        private const val NOTIFICATION_ID          = 1005
        private const val WARNING_NOTIFICATION_ID  = 1006

        fun createStartIntent(
            context: Context,
            friendName: String,
            friendPublicKey: ByteArray,
            mode: MFSKMode,
            baseFrequencyHz: Int
        ): Intent = Intent(context, MFSKReceiveSessionService::class.java).apply {
            action = ACTION_START_SESSION
            putExtra(EXTRA_FRIEND_NAME, friendName)
            putExtra(EXTRA_FRIEND_PUBLIC_KEY, friendPublicKey)
            putExtra(EXTRA_MODE_LABEL, mode.label)
            putExtra(EXTRA_BASE_FREQUENCY_HZ, baseFrequencyHz)
        }

        fun createStopIntent(context: Context): Intent =
            Intent(context, MFSKReceiveSessionService::class.java).apply {
                action = ACTION_STOP_SESSION
            }

        fun createExtendIntent(context: Context): Intent =
            Intent(context, MFSKReceiveSessionService::class.java).apply {
                action = ACTION_EXTEND_SESSION
            }
    }

    // ==================== Binder ====================

    inner class LocalBinder : Binder()
    {
        fun getService(): MFSKReceiveSessionService = this@MFSKReceiveSessionService
    }

    private val binder = LocalBinder()

    // ==================== Observable State ====================

    private val _sessionState =
        MutableStateFlow<MFSKReceiveSessionState>(MFSKReceiveSessionState.Idle)
    val sessionState: StateFlow<MFSKReceiveSessionState> = _sessionState.asStateFlow()

    private val _audioLevel = MutableStateFlow<AudioLevelInfo?>(null)
    val audioLevel: StateFlow<AudioLevelInfo?> = _audioLevel.asStateFlow()

    private val _messageJustReceived = MutableStateFlow(false)
    val messageJustReceived: StateFlow<Boolean> = _messageJustReceived.asStateFlow()

    private val _decryptedMessageRecords =
        MutableStateFlow<List<DecryptedMessageRecord>>(emptyList())
    val decryptedMessageRecords: StateFlow<List<DecryptedMessageRecord>> =
        _decryptedMessageRecords.asStateFlow()

    /**
     * Emits the raw ciphertext bytes of each successfully decrypted message.
     * Consumers (ViewModel → Activity) use this to refresh the message list UI.
     * No plaintext is ever emitted — ciphertext only.
     */
    private val _lastReceivedMessage = MutableSharedFlow<ByteArray>(replay = 0)
    val lastReceivedMessage: SharedFlow<ByteArray> = _lastReceivedMessage.asSharedFlow()

    private val _currentFriendName = MutableStateFlow<String?>(null)
    val currentFriendName: StateFlow<String?> = _currentFriendName.asStateFlow()

    // ==================== Internal State ====================

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mfskStation: MFSKStation? = null
    private lateinit var usbAudioManager: UsbAudioManager
    private var usbAudioConnection: UsbAudioConnection? = null

    // Friend context — set for the duration of a session, cleared on stop
    private var friendName: String? = null
    private var friendPublicKey: ByteArray? = null

    // Timing
    private var sessionStartTimeMs = 0L
    private var timeoutStartTimeMs = 0L

    // Jobs
    private var sessionJob: Job? = null
    private var timeoutJob: Job? = null
    private var warningJob: Job? = null
    private var notificationUpdateJob: Job? = null

    // Foreground tracking — timeout only runs while backgrounded
    private var isBound = false

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notificationManager: NotificationManager

    // ==================== Service Lifecycle ====================

    override fun onCreate()
    {
        super.onCreate()
        usbAudioManager = UsbAudioManager.create(applicationContext)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        when (intent?.action)
        {
            ACTION_START_SESSION ->
            {
                val name       = intent.getStringExtra(EXTRA_FRIEND_NAME)
                val key        = intent.getByteArrayExtra(EXTRA_FRIEND_PUBLIC_KEY)
                val modeLabel  = intent.getStringExtra(EXTRA_MODE_LABEL)
                val baseFreqHz = intent.getIntExtra(EXTRA_BASE_FREQUENCY_HZ, 1500)

                if (name == null || key == null || modeLabel == null)
                {
                    Timber.e("MFSKReceiveSessionService: missing required extras")
                    stopSelf()
                    return START_NOT_STICKY
                }

                val mode = MFSKMode.fromLabel(modeLabel)
                if (mode == null)
                {
                    Timber.e("MFSKReceiveSessionService: unknown mode label '$modeLabel'")
                    stopSelf()
                    return START_NOT_STICKY
                }

                startSession(name, key, mode, baseFreqHz)
            }

            ACTION_STOP_SESSION ->
            {
                stopSession()
                stopSelf()
            }

            ACTION_EXTEND_SESSION -> extendSession()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder
    {
        isBound = true
        cancelTimeoutIfRunning()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean
    {
        isBound = false
        startTimeoutIfBackgrounded()
        return true  // Receive onRebind()
    }

    override fun onRebind(intent: Intent?)
    {
        isBound = true
        cancelTimeoutIfRunning()
    }

    override fun onDestroy()
    {
        stopSession()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ==================== Session Management ====================

    private fun startSession(
        name: String,
        key: ByteArray,
        mode: MFSKMode,
        baseFrequencyHz: Int
    )
    {
        if (isSessionActive())
        {
            Timber.w("MFSKReceiveSessionService: session already active for '${friendName}', ignoring")
            return
        }

        Timber.d("MFSKReceiveSessionService: starting session for '$name'")

        friendName    = name
        friendPublicKey = key
        _currentFriendName.value = name
        sessionStartTimeMs = System.currentTimeMillis()

        _messageJustReceived.value = false
        _decryptedMessageRecords.value = emptyList()

        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        _sessionState.value = MFSKReceiveSessionState.Starting

        sessionJob = serviceScope.launch {
            try
            {
                runReceivePipeline(name, key, mode, baseFrequencyHz)
            }
            catch (e: CancellationException)
            {
                // Normal shutdown — state already set by stopSession() or timeout handler
                throw e
            }
            catch (e: Exception)
            {
                Timber.e(e, "MFSKReceiveSessionService: unhandled error in receive pipeline")
                _sessionState.value = MFSKReceiveSessionState.Failed(
                    e.message ?: "Unknown error"
                )
            }
            finally
            {
                cleanupAudioResources()
            }
        }

        if (!isBound) startTimeoutMonitor()
        startNotificationUpdates()
    }

    fun stopSession()
    {
        Timber.d("MFSKReceiveSessionService: stopping session")

        timeoutJob?.cancel()
        warningJob?.cancel()
        notificationUpdateJob?.cancel()
        sessionJob?.cancel()

        notificationManager.cancel(WARNING_NOTIFICATION_ID)

        _sessionState.value = MFSKReceiveSessionState.Stopped
        friendName = null
        friendPublicKey = null
        _currentFriendName.value = null
    }

    fun resetSession()
    {
        _sessionState.value = MFSKReceiveSessionState.Idle
        _messageJustReceived.value = false
        _decryptedMessageRecords.value = emptyList()
        sessionStartTimeMs = 0L
    }

    fun isSessionActive(): Boolean =
        _sessionState.value == MFSKReceiveSessionState.Running ||
                _sessionState.value == MFSKReceiveSessionState.Starting

    fun getSessionElapsedMs(): Long =
        if (sessionStartTimeMs > 0) System.currentTimeMillis() - sessionStartTimeMs else 0L

    fun clearMessageReceivedFlag()
    {
        _messageJustReceived.value = false
    }

    // ==================== Receive Pipeline ====================

    private suspend fun runReceivePipeline(
        name: String,
        key: ByteArray,
        mode: MFSKMode,
        baseFrequencyHz: Int
    )
    {
        // ── 1. Connect to USB audio ───────────────────────────────────────────
        // (unchanged from original)

        val devices = usbAudioManager.discoverDevices().first()
        if (devices.isEmpty())
        {
            Timber.e("MFSKReceiveSessionService: no USB audio devices found")
            _sessionState.value = MFSKReceiveSessionState.Failed("No USB audio device found")
            return
        }

        val connectResult = usbAudioManager.connectToDevice(devices.first())
        if (connectResult.isFailure)
        {
            Timber.e("MFSKReceiveSessionService: USB audio connection failed")
            _sessionState.value =
                MFSKReceiveSessionState.Failed("Failed to connect to USB audio device")
            return
        }

        val connection = connectResult.getOrThrow()
        usbAudioConnection = connection
        Timber.i("MFSKReceiveSessionService: USB audio connected — ${devices.first().displayName}")

        serviceScope.launch { observeUsbDisconnect() }

        // ── 2. Build configuration and audio stream ───────────────────────────
        // Configuration is created here so its sampleRate can be passed to asAudioFlow().

        val configuration = MFSKConfiguration(
            mode            = mode,
            baseFrequencyHz = baseFrequencyHz.toDouble()
            // sampleRate, amplitude, and timeoutMs use MFSKConstants defaults
        )

        val audioStream = connection.asAudioFlow(configuration.sampleRate)

        serviceScope.launch { observeAudioLevels(connection) }

        // ── 3. Start MFSKStation ──────────────────────────────────────────────

        mfskStation = MFSKStation(audioStream, configuration)
        val startResult = mfskStation!!.start()

        if (startResult.isFailure)
        {
            Timber.e("MFSKReceiveSessionService: station start failed — " +
                    "${startResult.exceptionOrNull()?.message}")
            _sessionState.value =
                MFSKReceiveSessionState.Failed("Failed to start MFSK station")
            return
        }

        _sessionState.value = MFSKReceiveSessionState.Running
        updateNotification()

        // ── 4. Collect and decrypt messages ───────────────────────────────────

        mfskStation!!.receivedMessages.collect { mfskMessage ->
            processReceivedMessage(mfskMessage.text, name, key)
            updateNotification()
        }
    }

    /**
     * Base64-decodes and decrypts a received MFSK text payload.
     *
     * A Base64 decode failure or decryption failure is expected when noise causes
     * [MFSKStation] to emit a complete-but-garbage frame. Log and continue.
     *
     * @param receivedText  The text string from [MFSKMessage.text], containing
     *                      Base64 ciphertext with surrounding CR characters.
     * @param name          Friend's display name, for saving the message.
     * @param key           Friend's public key bytes, for decryption.
     */
    private fun processReceivedMessage(receivedText: String, name: String, key: ByteArray)
    {
        try
        {
            val ciphertextBytes = android.util.Base64.decode(
                receivedText.trim(),
                android.util.Base64.DEFAULT
            )

            val friendPubKey = PublicKey(key)

            // Validate decryption — throws SecurityException if ciphertext is invalid.
            Encryption().decrypt(friendPubKey, ciphertextBytes)

            Timber.i("MFSKReceiveSessionService: successfully decrypted message from '$name'")

            saveReceivedMessage(ciphertextBytes, name)

            _messageJustReceived.value = true

            _decryptedMessageRecords.value += DecryptedMessageRecord(
                                    timestamp = System.currentTimeMillis(),
                                    spotCount = 1
                                )

            serviceScope.launch {
                _lastReceivedMessage.emit(ciphertextBytes)
            }
        }
        catch (e: SecurityException)
        {
            // Invalid ciphertext — most likely noise or a spurious frame decode.
            // Continue listening.
            Timber.d("MFSKReceiveSessionService: decryption failed (likely noise) — " +
                    "${receivedText.length} chars received, continuing")
        }
        catch (e: Exception)
        {
            Timber.w(e, "MFSKReceiveSessionService: unexpected error processing message")
        }
    }


    private fun saveReceivedMessage(ciphertextBytes: ByteArray, name: String)
    {
        try
        {
            val friend = Persist.friendList.find { it.name == name }
            if (friend == null)
            {
                Timber.e("MFSKReceiveSessionService.saveReceivedMessage: " +
                        "friend '$name' not found in Persist.friendList")
                return
            }

            Message(ciphertextBytes, friend, fromMe = false, isEncrypted = true)
                .save(applicationContext)

            Timber.i("MFSKReceiveSessionService: saved received message for '$name'")
        }
        catch (e: Exception)
        {
            Timber.e(e, "MFSKReceiveSessionService.saveReceivedMessage: save failed")
        }
    }

    // ==================== USB Monitoring ====================

    private suspend fun observeUsbDisconnect()
    {
        UsbAudioDeviceMonitor
            .observeAvailability(applicationContext)
            .debounce(500L)
            .filter { available -> !available }
            .first()

        Timber.w("MFSKReceiveSessionService: USB audio disconnected — stopping session")
        _sessionState.value = MFSKReceiveSessionState.Failed("USB audio device disconnected")
        stopSession()
        stopSelf()
    }

    private suspend fun observeAudioLevels(connection: UsbAudioConnection)
    {
        connection.getAudioLevel().collect { levelInfo ->
            _audioLevel.value = levelInfo
        }
    }

    private suspend fun cleanupAudioResources()
    {
        try
        {
            mfskStation?.stop()
            usbAudioConnection?.disconnect()
        }
        catch (e: Exception)
        {
            Timber.w(e, "MFSKReceiveSessionService: error during audio cleanup")
        }
        finally
        {
            mfskStation        = null
            usbAudioConnection = null
        }
    }

    // ==================== Timeout ====================

    private fun startTimeoutIfBackgrounded()
    {
        if (isSessionActive() && timeoutJob?.isActive != true)
        {
            Timber.d("MFSKReceiveSessionService: backgrounded — starting " +
                    "${SESSION_TIMEOUT_MS / 60000} minute timeout")
            startTimeoutMonitor()
        }
    }

    private fun cancelTimeoutIfRunning()
    {
        timeoutJob?.let {
            if (it.isActive)
            {
                it.cancel()
                timeoutJob = null
                timeoutStartTimeMs = 0L
                Timber.d("MFSKReceiveSessionService: timeout cancelled — app in foreground")
            }
        }
        warningJob?.cancel()
        warningJob = null
        notificationManager.cancel(WARNING_NOTIFICATION_ID)
        updateNotification()
    }

    private fun extendSession()
    {
        if (!isSessionActive()) return
        Timber.d("MFSKReceiveSessionService: session extended")
        timeoutJob?.cancel()
        timeoutJob = null
        warningJob?.cancel()
        warningJob = null
        timeoutStartTimeMs = 0L
        notificationManager.cancel(WARNING_NOTIFICATION_ID)
        if (!isBound) startTimeoutMonitor()
        updateNotification()
    }

    private fun startTimeoutMonitor()
    {
        timeoutJob?.cancel()
        warningJob?.cancel()
        timeoutStartTimeMs = System.currentTimeMillis()

        warningJob = serviceScope.launch {
            delay(SESSION_TIMEOUT_MS - TIMEOUT_WARNING_MS)
            if (isSessionActive() && !isBound) showTimeoutWarningNotification()
        }

        timeoutJob = serviceScope.launch {
            delay(SESSION_TIMEOUT_MS)
            if (isSessionActive())
            {
                Timber.d("MFSKReceiveSessionService: session timeout reached")
                _sessionState.value = MFSKReceiveSessionState.Failed("Session timed out")
                stopSession()
                delay(500)
                stopSelf()
            }
        }
    }

    private fun getRemainingTimeoutMs(): Long?
    {
        if (timeoutStartTimeMs == 0L || timeoutJob?.isActive != true) return null
        val remaining = SESSION_TIMEOUT_MS - (System.currentTimeMillis() - timeoutStartTimeMs)
        return if (remaining > 0) remaining else null
    }

    // ==================== Notification ====================

    private fun createNotificationChannels()
    {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MFSK Receive Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of MFSK radio message reception"
                setShowBadge(false)
            }
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                WARNING_CHANNEL_ID,
                "MFSK Session Timeout Warning",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts before an MFSK session times out"
                enableVibration(true)
                setShowBadge(true)
            }
        )
    }

    private fun buildNotification(): Notification
    {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, FriendInfoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this, 1, createStopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val elapsedMs = getSessionElapsedMs()
        val elapsedText = String.format(
            "%d:%02d",
            (elapsedMs / 60000).toInt(),
            ((elapsedMs % 60000) / 1000).toInt()
        )

        val messagesReceived = _decryptedMessageRecords.value.size

        val statusText = when (_sessionState.value)
        {
            is MFSKReceiveSessionState.Running  -> "Listening for MFSK signals"
            is MFSKReceiveSessionState.Starting -> "Starting…"
            else                                -> "Session active"
        }

        val remainingMs = getRemainingTimeoutMs()
        val contentText = if (remainingMs != null)
        {
            val remainingMin = (remainingMs / 60000).toInt() + 1
            "$statusText • $messagesReceived messages • $elapsedText • Timeout in ${remainingMin}m"
        }
        else
        {
            "$statusText • $messagesReceived messages • $elapsedText"
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radio)
            .setContentTitle("Receiving (MFSK) from ${friendName ?: "friend"}")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.btn_close_small_24, "Stop", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (remainingMs != null)
        {
            val extendPendingIntent = PendingIntent.getService(
                this, 2, createExtendIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_radio, "Extend", extendPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification()
    {
        if (isSessionActive())
        {
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun showTimeoutWarningNotification()
    {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, FriendInfoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val extendPendingIntent = PendingIntent.getService(
            this, 2, createExtendIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this, 1, createStopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val warningMinutes = (TIMEOUT_WARNING_MS / 60000).toInt()

        notificationManager.notify(
            WARNING_NOTIFICATION_ID,
            NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_radio)
                .setContentTitle("Session ending soon")
                .setContentText("MFSK receive will timeout in $warningMinutes minutes")
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_radio, "Extend", extendPendingIntent)
                .addAction(R.drawable.btn_close_small_24, "Stop", stopPendingIntent)
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    private fun startNotificationUpdates()
    {
        notificationUpdateJob = serviceScope.launch {
            while (isActive && isSessionActive())
            {
                updateNotification()
                delay(NOTIFICATION_UPDATE_INTERVAL_MS)
            }
        }
    }

    // ==================== Wake Lock ====================

    private fun acquireWakeLock()
    {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Nahoft:MFSKReceiveSessionWakeLock"
        ).apply {
            acquire(SESSION_TIMEOUT_MS + 60_000)
        }
        Timber.d("MFSKReceiveSessionService: wake lock acquired")
    }

    private fun releaseWakeLock()
    {
        wakeLock?.let {
            if (it.isHeld) it.release()
            Timber.d("MFSKReceiveSessionService: wake lock released")
        }
        wakeLock = null
    }
}