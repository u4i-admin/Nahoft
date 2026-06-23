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
import org.nahoft.nahoft.services.WSPRTransmitSessionService
import org.nahoft.nahoft.services.WSPRTransmitSessionState

import timber.log.Timber

/**
 * ViewModel for [TransmitRadioBottomSheetFragment].
 *
 * Scoped to the fragment — created via [Factory] which receives the message
 * and friend public key as constructor arguments. These never change for the
 * lifetime of the sheet.
 *
 * Responsibilities:
 * - Start and bind to [WSPRTransmitSessionService]
 * - Relay [WSPRTransmitSessionState] to the fragment
 * - Provide frequency preference access
 * - Expose Eden availability for pre-start validation
 *
 * Does not perform encryption or serial communication directly.
 * All TX logic lives in [WSPRTransmitSessionService].
 */
class WSPRTransmitRadioViewModel(
    application: Application,
    val message: String,
    val friendName: String,
    val friendPublicKey: ByteArray
) : AndroidViewModel(application)
{
    // ==================== Service Binding ====================

    private var transmitService: WSPRTransmitSessionService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection
    {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?)
        {
            Timber.d("TransmitSessionService connected")
            val localBinder = binder as WSPRTransmitSessionService.LocalBinder
            transmitService = localBinder.getService()
            serviceBound = true
            startServiceFlowRelay()
        }

        override fun onServiceDisconnected(name: ComponentName?)
        {
            Timber.d("TransmitSessionService disconnected")
            transmitService = null
            serviceBound = false
        }
    }

    // ==================== State ====================

    /**
     * Current transmit session state relayed from [WSPRTransmitSessionService].
     *
     * The fragment collects this single flow for its lifetime. Once the service
     * binds, [startServiceFlowRelay] pipes service state into this flow so
     * the fragment never needs to re-subscribe.
     */
    private val _transmitSessionState = MutableStateFlow<WSPRTransmitSessionState>(WSPRTransmitSessionState.Idle)
    val transmitSessionState: StateFlow<WSPRTransmitSessionState> = _transmitSessionState

    // ==================== Hardware Availability ====================

    /**
     * True when Eden serial hardware is connected.
     * Sourced from application scope — no service needed.
     */
    val isEdenConnected: StateFlow<Boolean> =
        (getApplication<Nahoft>()).eden
            .map { it != null }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ==================== Frequency Preferences ====================

    fun getTxFrequencyKHz(): Int =
        Persist.loadIntKey(Persist.sharedPrefTxFrequencyKHzKey, 14095)

    fun saveTxFrequencyKHz(frequencyKHz: Int) =
        Persist.saveIntKey(Persist.sharedPrefTxFrequencyKHzKey, frequencyKHz)

    // ==================== Session Control ====================

    /**
     * Saves the chosen frequency, starts the foreground service, then binds to it.
     *
     * The service owns the full pipeline from this point forward.
     * Call only once — the service guards against duplicate starts.
     *
     * @param isEncrypted Whether to encrypt the message before encoding.
     *                    Defaults to true. Pass false for unencrypted WSPR transmission.
     */
    fun startTransmission(frequencyKHz: Int, isEncrypted: Boolean = true)
    {
        saveTxFrequencyKHz(frequencyKHz)

        val context = getApplication<Application>()

        val startIntent = WSPRTransmitSessionService.createStartIntent(
            context = context,
            message = message,
            friendName = friendName,
            friendPublicKey = friendPublicKey,
            frequencyKHz = frequencyKHz,
            isEncrypted = isEncrypted
        )
        context.startForegroundService(startIntent)

        // Bind immediately after starting so we can observe state
        val bindIntent = Intent(context, WSPRTransmitSessionService::class.java)
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
            context.startService(WSPRTransmitSessionService.createStopIntent(context))
        }
    }

    // ==================== Flow Relay ====================

    /**
     * Pipes [WSPRTransmitSessionService.transmitSessionState] into [_transmitSessionState].
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
                Timber.w(e, "TransmitRadioViewModel: error unbinding from service")
            }
            serviceBound = false
            transmitService = null
        }
    }

    // ==================== Factory ====================

    /**
     * Creates [WSPRTransmitRadioViewModel] with the message and friend context
     * required for the TX pipeline.
     *
     * Usage in fragment:
     * ```
     * val viewModel: TransmitRadioViewModel by viewModels {
     *     TransmitRadioViewModel.Factory(
     *         message = args.message,
     *         friendName = args.friendName,
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
            return WSPRTransmitRadioViewModel(
                application,
                message,
                friendName,
                friendPublicKey
            ) as T
        }
    }
}