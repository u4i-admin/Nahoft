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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.Nahoft
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity
import org.nahoft.nahoft.models.Message
import org.operatorfoundation.audiocoder.wspr.WSPREncoder
import org.operatorfoundation.audiocoder.wspr.WSPRTimingCoordinator
import org.operatorfoundation.codex.WSPRMessageFields
import org.operatorfoundation.codex.encodeDataToWSPRMessages
import org.operatorfoundation.codex.encodeUnencryptedPayload
import timber.log.Timber

/**
 * Foreground service managing WSPR radio transmit sessions.
 *
 * Owns the full TX pipeline:
 *   encrypt → encode to WSPR fields → wait for even UTC minute → transmit symbols
 *
 * Survives Activity lifecycle. The user can navigate away while spots are
 * transmitting — the notification shows current progress and provides a Stop action.
 *
 * Serial communication is entirely delegated to [Eden]. This service calls Eden
 * methods and trusts their boolean return values. No serial reads happen here.
 *
 * Usage:
 * - Start with ACTION_START_SESSION and required extras
 * - Bind to observe [transmitSessionState]
 * - Stop early with ACTION_STOP_SESSION or call [cancelTransmission] via binder
 */
class TransmitSessionService : Service()
{
    companion object
    {
        const val ACTION_START_SESSION = "org.nahoft.nahoft.action.TX_START_SESSION"
        const val ACTION_STOP_SESSION  = "org.nahoft.nahoft.action.TX_STOP_SESSION"

        const val EXTRA_MESSAGE        = "tx_message"
        const val EXTRA_FRIEND_NAME    = "tx_friend_name"
        const val EXTRA_FRIEND_PUBLIC_KEY = "tx_friend_public_key"
        const val EXTRA_FREQUENCY_KHZ  = "tx_frequency_khz"
        const val EXTRA_IS_ENCRYPTED  = "tx_is_encrypted"

        private const val NOTIFICATION_CHANNEL_ID = "nahoft_transmit_session"
        private const val NOTIFICATION_ID = 1003

        fun createStartIntent(
            context: Context,
            message: String,
            friendName: String,
            friendPublicKey: ByteArray,
            frequencyKHz: Int,
            isEncrypted: Boolean = true
        ): Intent = Intent(context, TransmitSessionService::class.java).apply {
            action = ACTION_START_SESSION
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_FRIEND_NAME, friendName)
            putExtra(EXTRA_FRIEND_PUBLIC_KEY, friendPublicKey)
            putExtra(EXTRA_FREQUENCY_KHZ, frequencyKHz)
            putExtra(EXTRA_IS_ENCRYPTED, isEncrypted)
        }

        fun createStopIntent(context: Context): Intent =
            Intent(context, TransmitSessionService::class.java).apply {
                action = ACTION_STOP_SESSION
            }
    }

    // ==================== Binder ====================

    inner class LocalBinder : Binder()
    {
        fun getService(): TransmitSessionService = this@TransmitSessionService
    }

    private val binder = LocalBinder()

    // ==================== State ====================

    private val _transmitSessionState = MutableStateFlow<TransmitSessionState>(TransmitSessionState.Idle)
    val transmitSessionState: StateFlow<TransmitSessionState> = _transmitSessionState.asStateFlow()

    // ==================== Internal ====================

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sessionJob: Job? = null
    private val timingCoordinator = WSPRTimingCoordinator()
    private lateinit var notificationManager: NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null

    // Stored for notification display only
    private var friendName: String? = null

    // ==================== Service Lifecycle ====================

    override fun onCreate()
    {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        when (intent?.action)
        {
            ACTION_START_SESSION -> {
                val message    = intent.getStringExtra(EXTRA_MESSAGE)
                val name       = intent.getStringExtra(EXTRA_FRIEND_NAME)
                val publicKey  = intent.getByteArrayExtra(EXTRA_FRIEND_PUBLIC_KEY)
                val freqKHz    = intent.getIntExtra(EXTRA_FREQUENCY_KHZ, 14095)
                val isEncrypted = intent.getBooleanExtra(EXTRA_IS_ENCRYPTED, true)

                if (message == null || name == null || publicKey == null)
                {
                    Timber.e("TransmitSessionService: missing required extras")
                    stopSelf()
                    return START_NOT_STICKY
                }

                startSession(message, name, publicKey, freqKHz, isEncrypted)
            }

            ACTION_STOP_SESSION -> {
                cancelTransmission()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy()
    {
        sessionJob?.cancel()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ==================== Session ====================

    private fun startSession(
        message: String,
        name: String,
        publicKey: ByteArray,
        frequencyKHz: Int,
        isEncrypted: Boolean
    )
    {
        if (sessionJob?.isActive == true)
        {
            Timber.w("TransmitSessionService: session already active, ignoring start")
            return
        }

        friendName = name
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)

        sessionJob = serviceScope.launch {
            try
            {
                runTxPipeline(message, name, publicKey, frequencyKHz, isEncrypted)
            }
            finally
            {
                // Ensure we clean up foreground state regardless of how the session ends
                stopForeground(STOP_FOREGROUND_DETACH)
                releaseWakeLock()

                // Brief delay so the final notification state is visible
                delay(500)
                stopSelf()
            }
        }
    }

    /**
     * Cancels an in-progress transmission immediately.
     *
     * Eden.transmitWSPR() has its own finally block that sends CONTROL_OFF
     * and CONTROL_RX when its coroutine is cancelled, so we do not need to
     * send those commands here. We just cancel the job and update state.
     */
    fun cancelTransmission()
    {
        if (sessionJob?.isActive == true)
        {
            Timber.d("TransmitSessionService: cancelling transmission")
            sessionJob?.cancel()
            _transmitSessionState.value = TransmitSessionState.Cancelled
        }
    }

    // ==================== TX Pipeline ====================

    /**
     * Full transmit pipeline. Runs entirely inside [sessionJob].
     *
     * Steps:
     * 1. Validate Eden is available
     * 2. Encrypt and encode (Preparing)
     * 3. For each spot: wait for window, transmit, switch to RX
     * 4. Save message and emit Complete
     *
     * Any failure emits [TransmitSessionState.Failed] with a descriptive reason.
     * Eden serial communication is fully delegated — no serial reads here.
     */
    private suspend fun runTxPipeline(
        message: String,
        friendName: String,
        publicKey: ByteArray,
        frequencyKHz: Int,
        isEncrypted: Boolean
    )
    {
        // ── 1. Validate Eden ──────────────────────────────────────────────────

        val eden = (application as Nahoft).eden.value
        if (eden == null)
        {
            Timber.e("TransmitSessionService: Eden not connected")
            _transmitSessionState.value = TransmitSessionState.Failed("Eden device not connected")
            return
        }

        // ── 2. Encrypt and encode ─────────────────────────────────────────────

        _transmitSessionState.value = TransmitSessionState.Preparing
        updateNotification()

        // payloadBytes holds cipher bytes (encrypted) or raw UTF-8 (unencrypted).
        val payloadBytes: ByteArray
        val symbolArrays: List<LongArray>

        try
        {
            val result = withContext(Dispatchers.Default) {
                if (isEncrypted)
                    encryptAndEncode(message, publicKey, frequencyKHz)
                else
                    encodeUnencryptedForTx(message, frequencyKHz)
            }

            payloadBytes = result.first
            symbolArrays = result.second
        }
        catch (e: Exception)
        {
            Timber.e(e, "TransmitSessionService: encrypt/encode failed")
            _transmitSessionState.value = TransmitSessionState.Failed(
                "Failed to encode message. It may be too large."
            )
            return
        }

        if (symbolArrays.isEmpty())
        {
            _transmitSessionState.value = TransmitSessionState.Failed(
                "Failed to encode message. It may be too large."
            )
            return
        }

        val totalSpots = symbolArrays.size
        Timber.d("TransmitSessionService: encoded to $totalSpots WSPR spot(s)")

        // ── 3. Transmit each spot ─────────────────────────────────────────────

        for ((spotIndex, symbolFrequencies) in symbolArrays.withIndex())
        {
            // Wait for the next even UTC minute, updating countdown every second
            val windowMs = timingCoordinator.getMillisUntilNextEvenMinute()
            var elapsed = 0L
            while (elapsed < windowMs) {
                _transmitSessionState.value = TransmitSessionState.WaitingForWindow(
                    spotIndex, totalSpots, windowMs - elapsed
                )
                updateNotification()
                val step = minOf(1000L, windowMs - elapsed)
                delay(step)
                elapsed += step
            }

            // Transmit this spot — symbol callback updates state per symbol
            val success = eden.transmitWSPR(symbolFrequencies) { symbolIndex, _ ->
                _transmitSessionState.value = TransmitSessionState.TransmittingSpot(
                    spotIndex, totalSpots, symbolIndex
                )
            }

            if (!success)
            {
                Timber.e("TransmitSessionService: transmission failed on spot ${spotIndex + 1}")
                _transmitSessionState.value = TransmitSessionState.Failed(
                    "Transmission failed on spot ${spotIndex + 1} of $totalSpots"
                )
                return
            }

            // Eden.transmitWSPR() already sent CONTROL_OFF + CONTROL_RX at end of spot.
            // Briefly show switching state before the next waiting-for-window countdown.
            _transmitSessionState.value = TransmitSessionState.SwitchingToRx(spotIndex, totalSpots)
            updateNotification()
        }

        // ── 4. Complete ───────────────────────────────────────────────────────

        saveMessage(payloadBytes, friendName, isEncrypted)
        _transmitSessionState.value = TransmitSessionState.Complete(totalSpots)
        updateNotification()

        Timber.i("TransmitSessionService: transmission complete — $totalSpots spot(s) sent")
    }

    // ==================== Encrypt and Encode ================================

    /**
     * Encrypts [message] and encodes to WSPR symbol arrays.
     * Returns a pair of (encryptedBytes, symbolArrays).
     * Throws on any failure — caller handles the exception.
     */
    private fun encryptAndEncode(
        message: String,
        publicKey: ByteArray,
        frequencyKHz: Int
    ): Pair<ByteArray, List<LongArray>>
    {
        val encryptedBytes = Encryption().encrypt(publicKey, message)

        val wsprFields: List<WSPRMessageFields> = encodeDataToWSPRMessages(encryptedBytes)
            ?: throw IllegalStateException("encodeDataToWSPRMessages returned null — message may be too large")

        val symbolArrays = wsprFields.map { fields ->
            WSPREncoder.encodeToFrequencies(
                WSPREncoder.WSPREncodeRequest(
                    fields.callsign,
                    fields.gridSquare,
                    fields.powerDbm,
                    frequencyKHz * 1000
                )
            )
        }

        return Pair(encryptedBytes, symbolArrays)
    }

    /**
     * Encodes a plaintext message for unencrypted WSPR transmission.
     *
     * No encryption step — raw UTF-8 bytes are passed directly to
     * encodeUnencryptedPayload(), which embeds the spot count header in spot 0.
     *
     * Returns a pair of (plaintextBytes, symbolArrays).
     * Throws on any failure — caller handles the exception.
     */
    private fun encodeUnencryptedForTx(
        message: String,
        frequencyKHz: Int
    ): Pair<ByteArray, List<LongArray>>
    {
        val plaintextBytes = message.toByteArray(Charsets.UTF_8)

        val wsprFields = encodeUnencryptedPayload(plaintextBytes)
            ?: throw IllegalStateException("encodeUnencryptedPayload returned null — message may be too large")

        val symbolArrays = wsprFields.map { fields ->
            WSPREncoder.encodeToFrequencies(
                WSPREncoder.WSPREncodeRequest(
                    fields.callsign,
                    fields.gridSquare,
                    fields.powerDbm,
                    frequencyKHz * 1000
                )
            )
        }

        return Pair(plaintextBytes, symbolArrays)
    }

    // ==================== Save Message ======================================

    /**
     * Saves the transmitted message ciphertext to persistent storage.
     * Looks up the friend by name from [Persist.friendList].
     * Non-fatal if it fails — transmission already succeeded.
     */
    private fun saveMessage(payloadBytes: ByteArray, friendName: String, isEncrypted: Boolean)
    {
        try
        {
            val friend = Persist.friendList.find { it.name == friendName }
            if (friend == null)
            {
                Timber.e("saveMessage: could not find friend '$friendName' in Persist.friendList")
                return
            }

            val savedMessage = Message(payloadBytes, friend, fromMe = true, isEncrypted = isEncrypted)
            savedMessage.save(applicationContext)
            Timber.i("saveMessage: saved transmitted message for '$friendName'")
        }
        catch (e: Exception)
        {
            Timber.e(e, "saveMessage: failed — message not saved")
        }
    }

    // ==================== Notification ======================================

    private fun createNotificationChannel()
    {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "WSPR Transmit Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress of WSPR radio message transmission"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
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
            this, 0, createStopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (val state = _transmitSessionState.value)
        {
            is TransmitSessionState.Idle          -> "Starting…"
            is TransmitSessionState.Preparing     -> "Encoding message…"
            is TransmitSessionState.WaitingForWindow -> {
                val secondsRemaining = (state.msRemaining / 1000).toInt()
                "Spot ${state.spotIndex + 1}/${state.totalSpots} — TX in ${secondsRemaining}s"
            }
            is TransmitSessionState.TransmittingSpot ->
                "Transmitting spot ${state.spotIndex + 1}/${state.totalSpots} — symbol ${state.symbolIndex + 1}/162"
            is TransmitSessionState.SwitchingToRx ->
                "Spot ${state.spotIndex + 1}/${state.totalSpots} complete"
            is TransmitSessionState.Complete      -> "Transmission complete"
            is TransmitSessionState.Failed        -> "Transmission failed"
            is TransmitSessionState.Cancelled     -> "Transmission cancelled"
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radio)
            .setContentTitle("Transmitting to ${friendName ?: "friend"}")
            .setContentText(statusText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.btn_close_small_24, "Stop", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification()
    {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    // ==================== Wake Lock =========================================

    private fun acquireWakeLock()
    {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Nahoft:TransmitSessionWakeLock"
        ).apply {
            // Maximum plausible TX duration: 8 spots minimum × 2 min + buffer
            acquire(30 * 60 * 1000L)
        }
        Timber.d("TransmitSessionService: wake lock acquired")
    }

    private fun releaseWakeLock()
    {
        wakeLock?.let {
            if (it.isHeld) it.release()
            Timber.d("TransmitSessionService: wake lock released")
        }
        wakeLock = null
    }
}