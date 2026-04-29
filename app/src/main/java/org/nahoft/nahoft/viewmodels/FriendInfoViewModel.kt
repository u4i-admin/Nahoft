package org.nahoft.nahoft.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.nahoft.nahoft.Eden
import org.nahoft.nahoft.Nahoft
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.models.DecryptedMessageRecord
import org.nahoft.nahoft.models.Friend
import org.nahoft.nahoft.models.FriendStatus
import org.nahoft.nahoft.models.WSPRSpotItem
import org.nahoft.nahoft.services.MFSKReceiveSessionService
import org.nahoft.nahoft.services.MFSKReceiveSessionState
import org.nahoft.nahoft.services.WSPRPacketRequirement
import org.nahoft.nahoft.services.WSPRReceiveSessionService
import org.nahoft.nahoft.services.WSPRReceiveSessionState
import org.operatorfoundation.audiocoder.mfsk.MFSKMode
import org.operatorfoundation.audiocoder.wspr.WSPRTimingCoordinator
import org.operatorfoundation.audiocoder.wspr.models.WSPRCycleInformation
import org.operatorfoundation.audiocoder.wspr.models.WSPRStationState
import org.operatorfoundation.signalbridge.UsbAudioDeviceMonitor
import org.operatorfoundation.signalbridge.models.AudioLevelInfo
import timber.log.Timber

class FriendInfoViewModel(application: Application) : AndroidViewModel(application)
{
    private val timingCoordinator = WSPRTimingCoordinator()

    fun getMillisUntilNextEvenMinute(): Long = timingCoordinator.getMillisUntilNextEvenMinute()

    // ── WSPR frequency preferences ────────────────────────────────────────────
    fun getTxFrequencyKHz(): Int  = Persist.loadIntKey(Persist.sharedPrefTxFrequencyKHzKey, 14095)
    fun saveTxFrequencyKHz(frequencyKHz: Int) = Persist.saveIntKey(Persist.sharedPrefTxFrequencyKHzKey, frequencyKHz)
    fun getRxFrequencyKHz(): Int  = Persist.loadIntKey(Persist.sharedPrefRxFrequencyKHzKey, 14095)
    fun saveRxFrequencyKHz(frequencyKHz: Int) = Persist.saveIntKey(Persist.sharedPrefRxFrequencyKHzKey, frequencyKHz)

    // ── MFSK frequency preferences ────────────────────────────────────────────
    fun getMfskBaseFrequencyHz(): Int = Persist.loadIntKey(Persist.sharedPrefMfskBaseFrequencyHzKey, 1500)
    fun saveMfskBaseFrequencyHz(frequencyHz: Int) = Persist.saveIntKey(Persist.sharedPrefMfskBaseFrequencyHzKey, frequencyHz)

    // ==================== WSPR Service Binding ====================

    private var wsprReceiveService: WSPRReceiveSessionService? = null
    private var wsprServiceBound = false

    private val _wsprServiceConnected = MutableStateFlow(false)
    val wsprServiceConnected: StateFlow<Boolean> = _wsprServiceConnected.asStateFlow()

    private val wsprServiceConnection = object : ServiceConnection
    {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?)
        {
            Timber.d("ReceiveSessionService connected")
            val localBinder = binder as WSPRReceiveSessionService.LocalBinder
            wsprReceiveService = localBinder.getService()
            wsprServiceBound = true
            _wsprServiceConnected.value = true
            startWsprServiceFlowRelay()
        }

        override fun onServiceDisconnected(name: ComponentName?)
        {
            Timber.d("ReceiveSessionService disconnected")
            wsprReceiveService = null
            wsprServiceBound = false
            _wsprServiceConnected.value = false
        }
    }

    // ==================== WSPR Receive Session State (Relayed from Service) ====================

    private val _wsprReceiveSessionState =
        MutableStateFlow<WSPRReceiveSessionState>(WSPRReceiveSessionState.Idle)
    val wsprReceiveSessionState: StateFlow<WSPRReceiveSessionState> =
        _wsprReceiveSessionState.asStateFlow()

    private val _wsprReceivedSpots = MutableStateFlow<List<WSPRSpotItem>>(emptyList())
    val wsprReceivedSpots: StateFlow<List<WSPRSpotItem>> = _wsprReceivedSpots.asStateFlow()

    private val _wsprStationState = MutableStateFlow<WSPRStationState?>(null)
    val wsprStationState: StateFlow<WSPRStationState?> = _wsprStationState.asStateFlow()

    private val _wsprCycleInformation = MutableStateFlow<WSPRCycleInformation?>(null)
    val wsprCycleInformation: StateFlow<WSPRCycleInformation?> =
        _wsprCycleInformation.asStateFlow()

    private val _wsprAudioLevel = MutableStateFlow<AudioLevelInfo?>(null)
    val wsprAudioLevel: StateFlow<AudioLevelInfo?> = _wsprAudioLevel.asStateFlow()

    private val _wsprMessageJustReceived = MutableStateFlow(false)
    val wsprMessageJustReceived: StateFlow<Boolean> = _wsprMessageJustReceived.asStateFlow()

    private val _wsprLastReceivedMessage = MutableSharedFlow<ByteArray>(replay = 0)
    val wsprLastReceivedMessage: SharedFlow<ByteArray> = _wsprLastReceivedMessage.asSharedFlow()

    private val _wsprDecryptedMessageRecords =
        MutableStateFlow<List<DecryptedMessageRecord>>(emptyList())
    val wsprDecryptedMessageRecords: StateFlow<List<DecryptedMessageRecord>> =
        _wsprDecryptedMessageRecords.asStateFlow()

    private val _wsprPacketRequirement = MutableStateFlow<WSPRPacketRequirement>(
        WSPRPacketRequirement.Fixed(WSPRReceiveSessionService.MIN_SPOTS_FOR_DECRYPTION)
    )
    val wsprPacketRequirement: StateFlow<WSPRPacketRequirement> = _wsprPacketRequirement.asStateFlow()

    val wsprReceivedMessageCount: Int
        get() = wsprReceiveService?.receivedMessageCount ?: 0

    private var wsprFlowRelayJob: Job? = null

    // ==================== MFSK Service Binding ====================

    private var mfskReceiveService: MFSKReceiveSessionService? = null
    private var mfskServiceBound = false

    private val _mfskServiceConnected = MutableStateFlow(false)
    val mfskServiceConnected: StateFlow<Boolean> = _mfskServiceConnected.asStateFlow()

    private val mfskServiceConnection = object : ServiceConnection
    {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?)
        {
            Timber.d("MFSKReceiveSessionService connected")
            val localBinder = binder as MFSKReceiveSessionService.LocalBinder
            mfskReceiveService = localBinder.getService()
            mfskServiceBound = true
            _mfskServiceConnected.value = true
            startMfskServiceFlowRelay()
        }

        override fun onServiceDisconnected(name: ComponentName?)
        {
            Timber.d("MFSKReceiveSessionService disconnected")
            mfskReceiveService = null
            mfskServiceBound = false
            _mfskServiceConnected.value = false
        }
    }

    // ==================== MFSK Receive Session State (Relayed from Service) ====================

    private val _mfskSessionState =
        MutableStateFlow<MFSKReceiveSessionState>(MFSKReceiveSessionState.Idle)
    val mfskSessionState: StateFlow<MFSKReceiveSessionState> = _mfskSessionState.asStateFlow()

    private val _mfskAudioLevel = MutableStateFlow<AudioLevelInfo?>(null)
    val mfskAudioLevel: StateFlow<AudioLevelInfo?> = _mfskAudioLevel.asStateFlow()

    private val _mfskMessageJustReceived = MutableStateFlow(false)
    val mfskMessageJustReceived: StateFlow<Boolean> = _mfskMessageJustReceived.asStateFlow()

    private val _mfskDecryptedMessageRecords =
        MutableStateFlow<List<DecryptedMessageRecord>>(emptyList())
    val mfskDecryptedMessageRecords: StateFlow<List<DecryptedMessageRecord>> =
        _mfskDecryptedMessageRecords.asStateFlow()

    private val _mfskLastReceivedMessage = MutableSharedFlow<ByteArray>(replay = 0)
    val mfskLastReceivedMessage: SharedFlow<ByteArray> = _mfskLastReceivedMessage.asSharedFlow()

    private var mfskFlowRelayJob: Job? = null

    // ==================== Friend State ====================

    private val _friend = MutableStateFlow<Friend?>(null)
    val friend: StateFlow<Friend?> = _friend.asStateFlow()

    fun initializeFriend(friend: Friend)
    {
        if (_friend.value == null) _friend.value = friend
    }

    // ==================== Serial Connection ====================

    private val appEden: StateFlow<Eden?> = (getApplication<Nahoft>()).eden

    val isEdenConnected: StateFlow<Boolean> = appEden
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ==================== USB Audio Connection ====================

    private val _usbAudioAvailable = MutableStateFlow(false)
    val usbAudioAvailable: StateFlow<Boolean> = _usbAudioAvailable.asStateFlow()

    fun startAudioDeviceDiscovery()
    {
        viewModelScope.launch {
            UsbAudioDeviceMonitor.observeAvailability(
                context = getApplication(),
                requireInput = true
            ).collect { available ->
                Timber.d("USB audio input available: $available")
                _usbAudioAvailable.value = available
            }
        }
    }

    // ==================== Service Lifecycle ====================

    /**
     * Binds to [WSPRReceiveSessionService] if it is running.
     * Call from Activity.onStart().
     */
    fun bindToWsprServiceIfRunning()
    {
        val context = getApplication<Application>()
        val bound = context.bindService(
            Intent(context, WSPRReceiveSessionService::class.java),
            wsprServiceConnection,
            0
        )
        Timber.d("Attempted to bind to WSPR service: $bound")
    }

    /**
     * Unbinds from [WSPRReceiveSessionService].
     * Call from Activity.onStop().
     */
    fun unbindFromWsprService()
    {
        if (wsprServiceBound)
        {
            wsprFlowRelayJob?.cancel()
            wsprFlowRelayJob = null
            try { getApplication<Application>().unbindService(wsprServiceConnection) }
            catch (e: Exception) { Timber.w(e, "Error unbinding from WSPR service") }
            wsprServiceBound = false
            _wsprServiceConnected.value = false
            wsprReceiveService = null
        }
    }

    /**
     * Binds to [MFSKReceiveSessionService] if it is running.
     * Call from Activity.onStart().
     */
    fun bindToMfskServiceIfRunning()
    {
        val context = getApplication<Application>()
        val bound = context.bindService(
            Intent(context, MFSKReceiveSessionService::class.java),
            mfskServiceConnection,
            0
        )
        Timber.d("Attempted to bind to MFSK service: $bound")
    }

    /**
     * Unbinds from [MFSKReceiveSessionService].
     * Call from Activity.onStop().
     */
    fun unbindFromMfskService()
    {
        if (mfskServiceBound)
        {
            mfskFlowRelayJob?.cancel()
            mfskFlowRelayJob = null
            try { getApplication<Application>().unbindService(mfskServiceConnection) }
            catch (e: Exception) { Timber.w(e, "Error unbinding from MFSK service") }
            mfskServiceBound = false
            _mfskServiceConnected.value = false
            mfskReceiveService = null
        }
    }

    // ==================== WSPR Session Control ====================

    /**
     * Starts a WSPR receive session via the foreground service.
     *
     * Refuses to start if an MFSK session is already active — the two modes
     * are mutually exclusive since both claim USB audio.
     *
     * @param isEncrypted Whether to expect encrypted or unencrypted WSPR payloads.
     */
    fun startWsprReceiveSession(isEncrypted: Boolean = true)
    {
        if (isMfskSessionActive())
        {
            Timber.w("Cannot start WSPR receive session: MFSK session already active")
            return
        }

        val currentFriend = _friend.value ?: run {
            Timber.w("Cannot start WSPR session: no friend initialized")
            return
        }

        val publicKey = currentFriend.publicKeyEncoded ?: run {
            Timber.w("Cannot start WSPR session: friend has no public key")
            return
        }

        if (!_usbAudioAvailable.value)
        {
            Timber.w("Cannot start WSPR session: no USB audio available")
            return
        }

        wsprReceiveService?.let { service ->
            if (service.isSessionActive())
            {
                Timber.d(
                    if (service.currentFriendName.value == currentFriend.name)
                        "WSPR session already active for this friend"
                    else
                        "WSPR session active for different friend (${service.currentFriendName.value})"
                )
                return
            }
        }

        val context = getApplication<Application>()

        context.startForegroundService(
            WSPRReceiveSessionService.createStartIntent(
                context        = context,
                friendName     = currentFriend.name,
                friendPublicKey = publicKey,
                isEncrypted    = isEncrypted
            )
        )

        viewModelScope.launch {
            delay(100)
            context.bindService(
                Intent(context, WSPRReceiveSessionService::class.java),
                wsprServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    fun updateWsprEncryptionMode(isEncrypted: Boolean)
    {
        wsprReceiveService?.updateEncryptionMode(isEncrypted)
    }

    fun stopWsprReceiveSession()
    {
        wsprReceiveService?.stopSession()
        getApplication<Application>().startService(
            WSPRReceiveSessionService.createStopIntent(getApplication())
        )
    }

    fun resetWsprSession()
    {
        wsprReceiveService?.resetSession()
        _wsprReceiveSessionState.value = WSPRReceiveSessionState.Idle
        _wsprReceivedSpots.value = emptyList()
        _wsprMessageJustReceived.value = false
    }

    fun isWsprSessionActive(): Boolean = wsprReceiveService?.isSessionActive() ?: false
    fun getWsprSessionElapsedMs(): Long = wsprReceiveService?.getSessionElapsedMs() ?: 0L
    fun getWsprDecryptionAttempts(): Int = wsprReceiveService?.getDecryptionAttempts() ?: 0

    fun clearWsprMessageReceivedFlag()
    {
        wsprReceiveService?.clearMessageReceivedFlag()
        _wsprMessageJustReceived.value = false
    }

    // ==================== MFSK Session Control ====================

    /**
     * Starts an MFSK receive session via the foreground service.
     *
     * Refuses to start if a WSPR session is already active.
     * Mode is fixed to [MFSKMode.MFSK16] until multi-mode selection is added.
     */
    fun startMfskReceiveSession()
    {
        if (isWsprSessionActive())
        {
            Timber.w("Cannot start MFSK receive session: WSPR session already active")
            return
        }

        val currentFriend = _friend.value ?: run {
            Timber.w("Cannot start MFSK session: no friend initialized")
            return
        }

        val publicKey = currentFriend.publicKeyEncoded ?: run {
            Timber.w("Cannot start MFSK session: friend has no public key")
            return
        }

        if (!_usbAudioAvailable.value)
        {
            Timber.w("Cannot start MFSK session: no USB audio available")
            return
        }

        val context = getApplication<Application>()
        val mode = MFSKMode.MFSK16  // TODO: expose mode selection when additional modes are supported

        context.startForegroundService(
            MFSKReceiveSessionService.createStartIntent(
                context         = context,
                friendName      = currentFriend.name,
                friendPublicKey = publicKey,
                mode            = mode,
                baseFrequencyHz = getMfskBaseFrequencyHz()
            )
        )

        viewModelScope.launch {
            delay(100)
            context.bindService(
                Intent(context, MFSKReceiveSessionService::class.java),
                mfskServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    fun stopMfskReceiveSession()
    {
        mfskReceiveService?.stopSession()
        getApplication<Application>().startService(
            MFSKReceiveSessionService.createStopIntent(getApplication())
        )
    }

    fun resetMfskSession()
    {
        mfskReceiveService?.resetSession()
        _mfskSessionState.value = MFSKReceiveSessionState.Idle
        _mfskMessageJustReceived.value = false
    }

    fun isMfskSessionActive(): Boolean = mfskReceiveService?.isSessionActive() ?: false
    fun getMfskSessionElapsedMs(): Long = mfskReceiveService?.getSessionElapsedMs() ?: 0L

    fun clearMfskMessageReceivedFlag()
    {
        mfskReceiveService?.clearMessageReceivedFlag()
        _mfskMessageJustReceived.value = false
    }

    // ==================== WSPR Flow Relay ====================

    private fun startWsprServiceFlowRelay()
    {
        wsprFlowRelayJob?.cancel()
        val service = wsprReceiveService ?: return

        wsprFlowRelayJob = viewModelScope.launch {
            launch { service.receiveSessionState.collect    { _wsprReceiveSessionState.value = it } }
            launch { service.receivedSpots.collect          { _wsprReceivedSpots.value = it } }
            launch { service.stationState.collect           { _wsprStationState.value = it } }
            launch { service.cycleInformation.collect       { _wsprCycleInformation.value = it } }
            launch { service.audioLevel.collect             { _wsprAudioLevel.value = it } }
            launch { service.messageJustReceived.collect    { _wsprMessageJustReceived.value = it } }
            launch { service.lastReceivedMessage.collect    { _wsprLastReceivedMessage.emit(it) } }
            launch { service.decryptedMessageRecords.collect { _wsprDecryptedMessageRecords.value = it } }
            launch { service.wsprPacketRequirement.collect      { _wsprPacketRequirement.value = it } }
        }
    }

    // ==================== MFSK Flow Relay ====================

    private fun startMfskServiceFlowRelay()
    {
        mfskFlowRelayJob?.cancel()
        val service = mfskReceiveService ?: return

        mfskFlowRelayJob = viewModelScope.launch {
            launch { service.sessionState.collect             { _mfskSessionState.value = it } }
            launch { service.audioLevel.collect               { _mfskAudioLevel.value = it } }
            launch { service.messageJustReceived.collect      { _mfskMessageJustReceived.value = it } }
            launch { service.decryptedMessageRecords.collect  { _mfskDecryptedMessageRecords.value = it } }
            launch { service.lastReceivedMessage.collect      { _mfskLastReceivedMessage.emit(it) } }
        }
    }

    // ==================== Derived State ====================

    /**
     * Whether the send via serial (radio) button should be visible.
     * True when Eden is connected AND friend is Verified or Approved.
     */
    val canSendViaSerial: Flow<Boolean> = combine(appEden, _friend) { edenInstance, friend ->
        val hasValidStatus =
            friend?.status == FriendStatus.Verified || friend?.status == FriendStatus.Approved
        edenInstance != null && hasValidStatus
    }

    /**
     * Whether the receive via radio button should be visible.
     * True when USB audio is connected AND friend is Verified or Approved.
     */
    val canReceiveRadio: Flow<Boolean> = combine(_usbAudioAvailable, _friend)
    { audioAvailable, friend ->
        val hasValidStatus =
            friend?.status == FriendStatus.Verified || friend?.status == FriendStatus.Approved
        audioAvailable && hasValidStatus
    }

    // ==================== Cleanup ====================

    override fun onCleared()
    {
        super.onCleared()
        unbindFromWsprService()
        unbindFromMfskService()
    }
}