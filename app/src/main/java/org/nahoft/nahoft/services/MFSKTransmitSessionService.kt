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
import org.operatorfoundation.audiocoder.mfsk.MFSKEncoder
import org.operatorfoundation.audiocoder.mfsk.MFSKMode
import timber.log.Timber

/**
 * Foreground service managing MFSK radio transmit sessions.
 *
 * Owns the full TX pipeline:
 *   encrypt → frame → encode to symbol indices → convert to centihertz → transmit
 *
 * Unlike [WSPRTransmitSessionService], there is no window-waiting step — MFSK
 * transmission begins immediately after encoding.
 *
 * Note: MFSK transmission is encrypted only. Unencrypted MFSK TX is not
 * currently supported.
 *
 * Serial communication is entirely delegated to [Eden]. This service calls
 * Eden methods and trusts their boolean return values.
 *
 * Usage:
 * - Start with ACTION_START_SESSION and required extras
 * - Bind to observe [transmitSessionState]
 * - Stop early with ACTION_STOP_SESSION or call [cancelTransmission] via binder
 */
class MFSKTransmitSessionService : Service()
{
    companion object
    {
        const val ACTION_START_SESSION = "org.nahoft.nahoft.action.MFSK_TX_START_SESSION"
        const val ACTION_STOP_SESSION  = "org.nahoft.nahoft.action.MFSK_TX_STOP_SESSION"

        const val EXTRA_MESSAGE           = "mfsk_tx_message"
        const val EXTRA_FRIEND_NAME       = "mfsk_tx_friend_name"
        const val EXTRA_FRIEND_PUBLIC_KEY = "mfsk_tx_friend_public_key"
        const val EXTRA_MODE_LABEL        = "mfsk_tx_mode_label"
        const val EXTRA_BASE_FREQUENCY_HZ = "mfsk_tx_base_frequency_hz"
        const val EXTRA_DEBUG_BYPASS_ENCRYPTION = "mfsk_tx_debug_bypass_encryption"

        private const val NOTIFICATION_CHANNEL_ID = "nahoft_mfsk_transmit_session"
        private const val NOTIFICATION_ID = 1004

        fun createStartIntent(
            context: Context,
            message: String,
            friendName: String,
            friendPublicKey: ByteArray,
            mode: MFSKMode,
            baseFrequencyHz: Int
        ): Intent = Intent(context, MFSKTransmitSessionService::class.java).apply {
            action = ACTION_START_SESSION
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_FRIEND_NAME, friendName)
            putExtra(EXTRA_FRIEND_PUBLIC_KEY, friendPublicKey)
            putExtra(EXTRA_MODE_LABEL, mode.label)
            putExtra(EXTRA_BASE_FREQUENCY_HZ, baseFrequencyHz)
        }

        /**
         * Creates a start intent that bypasses encryption and Base64 encoding.
         *
         * The [debugPlaintext] is sent through MFSKEncoder.encodeToSymbols() directly,
         * making fldigi's decoded text directly comparable to [debugPlaintext]. Used
         * only by the debug-build test button — production callers must use
         * [createStartIntent].
         *
         * No message is saved on success; nothing is decrypted on the receiving end
         * because nothing was encrypted.
         */
        fun createDebugStartIntent(
            context: Context,
            debugPlaintext: String,
            mode: MFSKMode,
            baseFrequencyHz: Int
        ): Intent = Intent(context, MFSKTransmitSessionService::class.java).apply {
            action = ACTION_START_SESSION
            putExtra(EXTRA_MESSAGE, debugPlaintext)
            putExtra(EXTRA_FRIEND_NAME, "")              // unused in debug path
            putExtra(EXTRA_FRIEND_PUBLIC_KEY, ByteArray(0))  // unused in debug path
            putExtra(EXTRA_MODE_LABEL, mode.label)
            putExtra(EXTRA_BASE_FREQUENCY_HZ, baseFrequencyHz)
            putExtra(EXTRA_DEBUG_BYPASS_ENCRYPTION, true)
        }

        fun createStopIntent(context: Context): Intent =
            Intent(context, MFSKTransmitSessionService::class.java).apply {
                action = ACTION_STOP_SESSION
            }
    }

    // ==================== Binder ====================

    inner class LocalBinder : Binder()
    {
        fun getService(): MFSKTransmitSessionService = this@MFSKTransmitSessionService
    }

    private val binder = LocalBinder()

    // ==================== State ====================

    private val _transmitSessionState =
        MutableStateFlow<MFSKTransmitSessionState>(MFSKTransmitSessionState.Idle)
    val transmitSessionState: StateFlow<MFSKTransmitSessionState> =
        _transmitSessionState.asStateFlow()

    // ==================== Internal ====================

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sessionJob: Job? = null
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
            ACTION_START_SESSION ->
            {
                val message       = intent.getStringExtra(EXTRA_MESSAGE)
                val name          = intent.getStringExtra(EXTRA_FRIEND_NAME)
                val publicKey     = intent.getByteArrayExtra(EXTRA_FRIEND_PUBLIC_KEY)
                val modeLabel     = intent.getStringExtra(EXTRA_MODE_LABEL)
                val baseFreqHz    = intent.getIntExtra(EXTRA_BASE_FREQUENCY_HZ, 1500)

                if (message == null || name == null || publicKey == null || modeLabel == null)
                {
                    Timber.e("MFSKTransmitSessionService: missing required extras")
                    stopSelf()
                    return START_NOT_STICKY
                }

                val mode = MFSKMode.fromLabel(modeLabel)
                if (mode == null)
                {
                    Timber.e("MFSKTransmitSessionService: unknown mode label '$modeLabel'")
                    stopSelf()
                    return START_NOT_STICKY
                }

                val bypassEncryption = intent.getBooleanExtra(EXTRA_DEBUG_BYPASS_ENCRYPTION, false)
                startSession(message, name, publicKey, mode, baseFreqHz, bypassEncryption)
            }

            ACTION_STOP_SESSION -> cancelTransmission()
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
        mode: MFSKMode,
        baseFrequencyHz: Int,
        bypassEncryption: Boolean
    )
    {
        if (sessionJob?.isActive == true)
        {
            Timber.w("MFSKTransmitSessionService: session already active, ignoring start")
            return
        }

        friendName = name
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )

        sessionJob = serviceScope.launch {
            try
            {
                runTxPipeline(message, name, publicKey, mode, baseFrequencyHz, bypassEncryption)
            }
            finally
            {
                stopForeground(STOP_FOREGROUND_DETACH)
                releaseWakeLock()
                delay(500)
                stopSelf()
            }
        }
    }

    /**
     * Cancels an in-progress transmission immediately.
     *
     * Eden.transmitMFSK() shares the same finally block as transmitWSPR() —
     * it sends CONTROL_OFF and CONTROL_RX when its coroutine is cancelled,
     * so we do not need to send those commands here.
     */
    fun cancelTransmission()
    {
        if (sessionJob?.isActive == true)
        {
            Timber.d("MFSKTransmitSessionService: cancelling transmission")
            sessionJob?.cancel()
            _transmitSessionState.value = MFSKTransmitSessionState.Cancelled
        }
    }

    // ==================== TX Pipeline ====================

    /**
     * Full transmit pipeline. Runs entirely inside [sessionJob].
     *
     * Steps:
     * 1. Validate Eden is available
     * 2. Encrypt the message (Preparing)
     * 3. Frame and encode to symbol indices
     * 4. Convert symbol indices to centihertz frequencies
     * 5. Emit Transmitting(totalDurationMs) and call Eden.transmitMFSK()
     * 6. On success: save message and emit Complete
     * 7. On failure: emit Failed
     *
     * Note: Unlike WSPR, there is no window-waiting step. Transmission starts immediately.
     */
    private suspend fun runTxPipeline(
        message: String,
        friendName: String,
        publicKey: ByteArray,
        mode: MFSKMode,
        baseFrequencyHz: Int,
        bypassEncryption: Boolean
    )
    {
        // ── 1. Validate Eden ──────────────────────────────────────────────────

        val eden = (application as Nahoft).eden.value
        if (eden == null)
        {
            Timber.e("MFSKTransmitSessionService: Eden not connected")
            _transmitSessionState.value =
                MFSKTransmitSessionState.Failed("Eden device not connected")
            return
        }

        // ── 2. Encrypt ────────────────────────────────────────────────────────

        _transmitSessionState.value = MFSKTransmitSessionState.Preparing
        updateNotification()

        val payloadBytes: ByteArray
        val symbolFrequenciesCHz: LongArray
        val totalDurationMs: Long
        val symbolDurationMs: Long

        try
        {
            val result = withContext(Dispatchers.Default) {
                if (bypassEncryption)
                {
                    Timber.w("MFSKTransmitSessionService: DEBUG BYPASS — sending plaintext, no encryption")
                    encodePlaintextForDebug(message, mode, baseFrequencyHz)
                }
                else
                {
                    encryptAndEncode(message, publicKey, mode, baseFrequencyHz)
                }
            }
            payloadBytes       = result.first
            symbolFrequenciesCHz = result.second

            symbolDurationMs = (mode.symbolDurationSeconds * 1000).toLong()
            totalDurationMs  = symbolFrequenciesCHz.size * symbolDurationMs
        }
        catch (e: Exception)
        {
            Timber.e(e, "MFSKTransmitSessionService: encrypt/encode failed")
            _transmitSessionState.value =
                MFSKTransmitSessionState.Failed("Failed to encode message. It may be too large.")
            return
        }

        // ── 3. Transmit ───────────────────────────────────────────────────────

        Timber.d("MFSKTransmitSessionService: encoded to ${symbolFrequenciesCHz.size} symbols, " +
                "estimated duration ${totalDurationMs}ms")

        _transmitSessionState.value = MFSKTransmitSessionState.Transmitting(totalDurationMs)
        updateNotification()

        val success = eden.transmitMFSK(symbolFrequenciesCHz, symbolDurationMs)

        // ── 4. Complete or fail ───────────────────────────────────────────────

        if (!success)
        {
            Timber.e("MFSKTransmitSessionService: transmission failed")
            _transmitSessionState.value =
                MFSKTransmitSessionState.Failed("Transmission failed")
            return
        }

        if (!bypassEncryption)
        {
            saveMessage(payloadBytes, friendName)
        }

        _transmitSessionState.value = MFSKTransmitSessionState.Complete
        updateNotification()

        Timber.i("MFSKTransmitSessionService: transmission complete — " +
                "${symbolFrequenciesCHz.size} symbols sent")
    }

    // ==================== Encrypt and Encode ================================


    /**
     * Encrypts [message] with [publicKey], Base64-encodes the ciphertext, then
     * encodes the Base64 string as a complete MFSK symbol sequence.
     *
     * Returns a pair of (encryptedBytes, symbolFrequenciesCHz).
     * Throws on any failure.
     *
     * Symbol index → frequency conversion:
     *   `(baseFrequencyHz + toneIndex × mode.toneSpacingHz) × 100` cHz
     */
    private fun encryptAndEncode(
        message: String,
        publicKey: ByteArray,
        mode: MFSKMode,
        baseFrequencyHz: Int
    ): Pair<ByteArray, LongArray>
    {
        val encryptedBytes = Encryption().encrypt(publicKey, message)

        // Base64-encode the ciphertext so it is pure ASCII.
        // NO_WRAP prevents line breaks being inserted into the Base64 output,
        // which would otherwise be decoded as separate characters by the receiver.
        val base64Ciphertext = android.util.Base64.encodeToString(
            encryptedBytes,
            android.util.Base64.NO_WRAP
        )

        val symbolIndices = MFSKEncoder.encodeToSymbols(base64Ciphertext, mode)

        // Convert tone indices to centihertz frequencies for Eden.
        val symbolFrequenciesCHz = symbolIndices.map { toneIndex ->
            ((baseFrequencyHz + toneIndex * mode.toneSpacingHz) * 100).toLong()
        }.toLongArray()

        Timber.d("MFSKTransmitSessionService: encoded ${base64Ciphertext.length} base64 chars → ${symbolIndices.size} symbols")
        Timber.d("MFSKTransmitSessionService: payload = \"$base64Ciphertext\"")

        return Pair(encryptedBytes, symbolFrequenciesCHz)
    }

    /**
     * Debug-only encoding path. Skips encryption and Base64.
     * The plaintext goes directly into MFSKEncoder, so fldigi's RX window
     * should display [plaintext] character-for-character if TX is correct.
     *
     * Returns (emptyBytes, symbolFrequenciesCHz). The empty ByteArray
     * stands in for the encrypted ciphertext that the production path returns —
     * the caller must skip saveMessage() when bypassing encryption.
     */
    private fun encodePlaintextForDebug(
        plaintext: String,
        mode: MFSKMode,
        baseFrequencyHz: Int
    ): Pair<ByteArray, LongArray>
    {
        Timber.d("MFSKTransmitSessionService: DEBUG payload = \"$plaintext\"")

        val symbolIndices = MFSKEncoder.encodeToSymbols(plaintext, mode)

        val symbolFrequenciesCHz = symbolIndices.map { toneIndex ->
            ((baseFrequencyHz + toneIndex * mode.toneSpacingHz) * 100).toLong()
        }.toLongArray()

        Timber.d("MFSKTransmitSessionService: DEBUG encoded ${plaintext.length} chars → ${symbolIndices.size} symbols")

        return Pair(ByteArray(0), symbolFrequenciesCHz)
    }

    // ==================== Save Message ======================================

    /**
     * Saves the transmitted ciphertext to persistent storage.
     * Looks up the friend by name from [Persist.friendList].
     * Non-fatal if it fails — transmission already succeeded.
     */
    private fun saveMessage(encryptedBytes: ByteArray, friendName: String)
    {
        try
        {
            val friend = Persist.friendList.find { it.name == friendName }
            if (friend == null)
            {
                Timber.e("MFSKTransmitSessionService.saveMessage: could not find " +
                        "friend '$friendName' in Persist.friendList")
                return
            }

            val savedMessage = Message(encryptedBytes, friend, fromMe = true, isEncrypted = true)
            savedMessage.save(applicationContext)
            Timber.i("MFSKTransmitSessionService.saveMessage: saved for '$friendName'")
        }
        catch (e: Exception)
        {
            Timber.e(e, "MFSKTransmitSessionService.saveMessage: failed — message not saved")
        }
    }

    // ==================== Notification ======================================

    private fun createNotificationChannel()
    {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "MFSK Transmit Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress of MFSK radio message transmission"
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
            is MFSKTransmitSessionState.Idle        -> "Starting…"
            is MFSKTransmitSessionState.Preparing   -> "Encoding message…"
            is MFSKTransmitSessionState.Transmitting -> "Transmitting…"
            is MFSKTransmitSessionState.Complete    -> "Transmission complete"
            is MFSKTransmitSessionState.Failed      -> "Transmission failed"
            is MFSKTransmitSessionState.Cancelled   -> "Transmission cancelled"
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radio)
            .setContentTitle("Transmitting (MFSK) to ${friendName ?: "friend"}")
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
            "Nahoft:MFSKTransmitSessionWakeLock"
        ).apply {
            // Maximum plausible TX duration: generous upper bound for a long MFSK message
            acquire(10 * 60 * 1000L)
        }
        Timber.d("MFSKTransmitSessionService: wake lock acquired")
    }

    private fun releaseWakeLock()
    {
        wakeLock?.let {
            if (it.isHeld) it.release()
            Timber.d("MFSKTransmitSessionService: wake lock released")
        }
        wakeLock = null
    }
}