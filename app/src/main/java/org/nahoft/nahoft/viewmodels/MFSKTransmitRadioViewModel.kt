package org.nahoft.nahoft.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.nahoft.nahoft.Nahoft
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.services.MFSKTransmitSessionService
import org.nahoft.nahoft.services.MFSKTransmitSessionState
import org.operatorfoundation.audiocoder.mfsk.MFSKMode
import timber.log.Timber

/**
 * ViewModel for the MFSK transmit bottom sheet fragment.
 *
 * Scoped to the fragment — created via [Factory] which receives the message
 * and friend public key as constructor arguments. These never change for the
 * lifetime of the sheet.
 *
 * Responsibilities:
 * - Start and bind to [MFSKTransmitSessionService]
 * - Relay [MFSKTransmitSessionState] to the fragment
 * - Provide MFSK frequency and mode preference access
 * - Expose Eden availability for pre-start validation
 *
 * Does not perform encryption or serial communication directly.
 * All TX logic lives in [MFSKTransmitSessionService].
 */
class MFSKTransmitRadioViewModel(
    application: Application,
    val message: String,
    val friendName: String,
    val friendPublicKey: ByteArray
) : AndroidViewModel(application)
{
    // ==================== Service Binding ====================

    private var transmitService: MFSKTransmitSessionService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection
    {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?)
        {
            Timber.d("MFSKTransmitSessionService connected")
            val localBinder = binder as MFSKTransmitSessionService.LocalBinder
            transmitService = localBinder.getService()
            serviceBound = true
            startServiceFlowRelay()
        }

        override fun onServiceDisconnected(name: ComponentName?)
        {
            Timber.d("MFSKTransmitSessionService disconnected")
            transmitService = null
            serviceBound = false
        }
    }

    // ==================== State ====================

    /**
     * Current transmit session state relayed from [MFSKTransmitSessionService].
     *
     * The fragment collects this single flow for its lifetime. Once the service
     * binds, [startServiceFlowRelay] pipes service state into this flow so
     * the fragment never needs to re-subscribe.
     */
    private val _transmitSessionState =
        MutableStateFlow<MFSKTransmitSessionState>(MFSKTransmitSessionState.Idle)
    val transmitSessionState: StateFlow<MFSKTransmitSessionState> = _transmitSessionState

    // ==================== Hardware Availability ====================

    /**
     * True when Eden serial hardware is connected.
     * Sourced from application scope — no service needed.
     */
    val isEdenConnected: StateFlow<Boolean> =
        (getApplication<Nahoft>()).eden
            .map { it != null }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ==================== Preferences ====================

    fun getMfskBaseFrequencyHz(): Int =
        Persist.loadIntKey(Persist.sharedPrefMfskBaseFrequencyHzKey, 1500)

    fun saveMfskBaseFrequencyHz(frequencyHz: Int) =
        Persist.saveIntKey(Persist.sharedPrefMfskBaseFrequencyHzKey, frequencyHz)

    // ==================== Session Control ====================

    /**
     * Saves the chosen frequency, starts the foreground service, then binds to it.
     *
     * The service owns the full pipeline from this point forward.
     * Call only once — the service guards against duplicate starts.
     *
     * Mode is fixed to [MFSKMode.MFSK16] for now. When additional modes are
     * supported, mode selection will be exposed here and passed through.
     *
     * @param baseFrequencyHz Audio base frequency in Hz (e.g. 1500).
     */
    fun startTransmission(baseFrequencyHz: Int)
    {
        saveMfskBaseFrequencyHz(baseFrequencyHz)

        val context = getApplication<Application>()
        val mode = MFSKMode.MFSK16  // TODO: expose mode selection when additional modes are supported

        val startIntent = MFSKTransmitSessionService.createStartIntent(
            context         = context,
            message         = message,
            friendName      = friendName,
            friendPublicKey = friendPublicKey,
            mode            = mode,
            baseFrequencyHz = baseFrequencyHz
        )
        context.startForegroundService(startIntent)

        // Bind immediately after starting so we can observe state
        val bindIntent = Intent(context, MFSKTransmitSessionService::class.java)
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Starts a debug transmission that bypasses encryption and Base64.
     *
     * Sends a hard-coded plaintext payload directly through MFSKEncoder so that
     * fldigi's RX window should display the same string character-for-character.
     * Used to verify TX pipeline correctness independent of the encryption layer.
     *
     * No message is saved on completion. Debug builds only — the calling fragment
     * gates this behind BuildConfig.DEBUG.
     *
     * @param baseFrequencyHz Audio base frequency in Hz (e.g. 1500).
     */
    fun startDebugTransmission(baseFrequencyHz: Int)
    {
        saveMfskBaseFrequencyHz(baseFrequencyHz)

        val context = getApplication<Application>()
        val mode = MFSKMode.MFSK16

        val startIntent = MFSKTransmitSessionService.createDebugStartIntent(
            context         = context,
            debugPlaintext  = DEBUG_TEST_MESSAGE,
            mode            = mode,
            baseFrequencyHz = baseFrequencyHz
        )
        context.startForegroundService(startIntent)

        val bindIntent = Intent(context, MFSKTransmitSessionService::class.java)
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Cancels an in-progress transmission immediately.
     * Delegates to the service if bound; sends a stop intent as fallback.
     */
    fun cancelTransmission()
    {
        if (serviceBound)
        {
            transmitService?.cancelTransmission()
        }
        else
        {
            // Service running but not yet bound — send stop intent
            val context = getApplication<Application>()
            context.startService(MFSKTransmitSessionService.createStopIntent(context))
        }
    }

    // ==================== Flow Relay ====================

    /**
     * Pipes [MFSKTransmitSessionService.transmitSessionState] into [_transmitSessionState].
     * Called once when service binding completes.
     * The fragment's existing collector receives all subsequent states automatically.
     */
    private fun startServiceFlowRelay()
    {
        val service = transmitService ?: return

        viewModelScope.launch {
            service.transmitSessionState.collect { state ->
                _transmitSessionState.value = state
            }
        }
    }

    // ==================== Cleanup ====================

    override fun onCleared()
    {
        super.onCleared()

        if (serviceBound)
        {
            try
            {
                getApplication<Application>().unbindService(serviceConnection)
            }
            catch (e: Exception)
            {
                Timber.w(e, "MFSKTransmitRadioViewModel: error unbinding from service")
            }
            serviceBound = false
            transmitService = null
        }
    }

    // ==================== Factory ====================

    /**
     * Creates [MFSKTransmitRadioViewModel] with the message and friend context
     * required for the TX pipeline.
     *
     * Usage in fragment:
     * ```
     * val viewModel: MFSKTransmitRadioViewModel by viewModels {
     *     MFSKTransmitRadioViewModel.Factory(
     *         message         = args.message,
     *         friendName      = args.friendName,
     *         friendPublicKey = args.friendPublicKey
     *     )
     * }
     * ```
     */
    class Factory(
        private val message: String,
        private val friendName: String,
        private val friendPublicKey: ByteArray
    ) : ViewModelProvider.AndroidViewModelFactory()
    {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(
            modelClass: Class<T>,
            extras: androidx.lifecycle.viewmodel.CreationExtras
        ): T
        {
            val application = extras[APPLICATION_KEY]!!
            return MFSKTransmitRadioViewModel(
                application,
                message,
                friendName,
                friendPublicKey
            ) as T
        }
    }

    companion object
    {
        /**
         * Hard-coded plaintext for debug transmissions. Repeating "HELLO WORLD"
         * gives fldigi an easy pattern to match even with bit errors, and exercises
         * both letters and the space character (which has a distinctive short
         * IZ8BLY Varicode code word).
         */
        private const val DEBUG_TEST_MESSAGE = "HELLO WORLD HELLO WORLD HELLO WORLD "
    }
}