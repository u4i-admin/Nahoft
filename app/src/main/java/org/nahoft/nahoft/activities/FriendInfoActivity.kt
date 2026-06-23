package org.nahoft.nahoft.activities

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import android.content.ClipboardManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import kotlinx.coroutines.*
import org.libsodium.jni.keys.PublicKey
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.codex.KeyOrMessage
import org.nahoft.nahoft.*
import org.nahoft.nahoft.databinding.ActivityFriendInfoBinding
import org.nahoft.nahoft.fragments.*
import org.nahoft.org.nahoft.swatch.Decoder
import org.nahoft.org.nahoft.swatch.Encoder
import org.nahoft.util.RequestCodes
import org.nahoft.util.ShareUtil
import org.nahoft.util.showAlert

import org.nahoft.nahoft.fragments.WSPRReceiveRadioBottomSheetFragment
import org.nahoft.nahoft.models.Friend
import org.nahoft.nahoft.models.FriendStatus
import org.nahoft.nahoft.models.Message
import org.nahoft.nahoft.models.slideNameChat
import org.nahoft.nahoft.services.MFSKReceiveSessionState
import org.nahoft.nahoft.services.WSPRReceiveSessionState
import org.nahoft.nahoft.viewmodels.FriendInfoViewModel
import org.nahoft.util.applySecureFlag

import timber.log.Timber

class FriendInfoActivity: AppCompatActivity()
{
    enum class ImportPurpose { IMPORT_KEY, IMPORT_MESSAGE }

    private lateinit var viewModel: FriendInfoViewModel
    private lateinit var binding: ActivityFriendInfoBinding
    private var decodePayload: ByteArray? = null
    private lateinit var thisFriend: Friend

    private val parentJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    private val menuFragmentTag = "MenuFragment"
    private var isShareImageButtonShow: Boolean = false
    private var indicatorAnimator: ObjectAnimator? = null

    // True when the user has tapped "receive via radio" and we're waiting for the
    // system permission dialog. The launcher callback uses this to decide whether
    // to continue the receive flow on grant.
    private var pendingReceiveRequest = false

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val wasPendingRequest = pendingReceiveRequest
        pendingReceiveRequest = false

        if (isGranted)
        {
            Timber.d("RECORD_AUDIO permission granted")
            if (wasPendingRequest) receiveViaRadioClicked()
        }
        else
        {
            Timber.w("RECORD_AUDIO permission denied")
            if (wasPendingRequest) showMicrophonePermissionDeniedDialog()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
    { isGranted ->

        if (isGranted)
        {
            Timber.d("POST_NOTIFICATIONS permission granted")
            showRadioModeSelector(RadioModeBottomSheetFragment.Purpose.RX)
        }
        else
        {
            Timber.d("POST_NOTIFICATIONS permission denied, proceeding anyway")
            showNotificationDeniedWarning()
        }
    }

    private fun showNotificationDeniedWarning()
    {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_AddFriendAlertDialog))

        val title = SpannableString(getString(R.string.notification_denied_title))
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 20)

        val textView = TextView(this)
        textView.text = getString(R.string.notification_denied_warning)
        textView.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        container.addView(textView)

        builder.setView(container)

        builder.setPositiveButton(getString(R.string.continue_anyway)) { dialog, _ ->
            dialog.dismiss()
            showRadioModeSelector(RadioModeBottomSheetFragment.Purpose.RX)
        }

        builder.setNeutralButton(getString(R.string.stop_button)) { dialog, _ ->
            dialog.cancel()
        }

        builder.create().show()
    }

    // Image picker for saving encoded image locally
    @OptIn(ExperimentalUnsignedTypes::class)
    private val pickImageForSavingLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { handlePickedImageForEncoding(it, saveImage = true) }
        }

    // Image picker for sharing encoded image
    @OptIn(ExperimentalUnsignedTypes::class)
    private val pickImageForSharingLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { handlePickedImageForEncoding(it, saveImage = false) }
        }

    // Image picker for importing/decoding
    @OptIn(ExperimentalUnsignedTypes::class)
    private val pickImageForImportLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { decodeImage(it) }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityFriendInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.relativeLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            binding.headerSection.updatePadding(top = systemBars.top)
            view.updatePadding(bottom = maxOf(systemBars.bottom, ime.bottom))

            insets
        }

        viewModel = ViewModelProvider(this)[FriendInfoViewModel::class.java]
        window.applySecureFlag()

        if (!Persist.accessIsAllowed()) { sendToLogin() }

        // Get our pending friend
        val maybeFriend = intent.getSerializableExtra(RequestCodes.friendExtraTaskDescription, Friend::class.java)

        if (maybeFriend == null)
        { // this should never happen, get out of this activity.
            Timber.e("Attempted to open FriendInfoActivity, but Friend was null.")
            return
        }
        else
        {
            thisFriend = maybeFriend
            viewModel.initializeFriend(thisFriend)
        }

        setClickListeners()
        receivedSharedMessage()
        setupConnectionObservers()

        // Refresh message list when a TX session completes and the sheet is dismissed.
        supportFragmentManager.setFragmentResultListener(
            WSPRTransmitRadioBottomSheetFragment.RESULT_TX_COMPLETE,
            this
        ) { _, _ ->
            binding.messageEditText.text?.clear()
            setupViewByStatus()
        }

        // Refresh UI when an MFSK TX session completes and the sheet is dismissed
        supportFragmentManager.setFragmentResultListener(
            MFSKTransmitRadioBottomSheetFragment.RESULT_TX_COMPLETE,
            this
        ) { _, _ ->
            binding.messageEditText.text?.clear()
            setupViewByStatus()
        }

        // Radio mode chosen for TX — launch the appropriate TX sheet
        supportFragmentManager.setFragmentResultListener(
            RadioModeBottomSheetFragment.RESULT_KEY_TX,
            this
        ) { _, bundle ->
            val mode = RadioModeBottomSheetFragment.RadioMode.valueOf(
                bundle.getString(RadioModeBottomSheetFragment.EXTRA_MODE)!!
            )
            val message   = binding.messageEditText.text.toString()
            val publicKey = thisFriend.publicKeyEncoded ?: return@setFragmentResultListener

            when (mode)
            {
                RadioModeBottomSheetFragment.RadioMode.WSPR ->
                    WSPRTransmitRadioBottomSheetFragment
                        .newInstance(message, thisFriend.name, publicKey)
                        .show(supportFragmentManager, "WSPRTransmitRadioBottomSheet")

                RadioModeBottomSheetFragment.RadioMode.MFSK ->
                    MFSKTransmitRadioBottomSheetFragment
                        .newInstance(message, thisFriend.name, publicKey)
                        .show(supportFragmentManager, "MFSKTransmitRadioBottomSheet")
            }
        }

        // Radio mode chosen for RX — launch the appropriate RX sheet
        supportFragmentManager.setFragmentResultListener(
            RadioModeBottomSheetFragment.RESULT_KEY_RX,
            this
        ) { _, bundle ->
            val mode = RadioModeBottomSheetFragment.RadioMode.valueOf(
                bundle.getString(RadioModeBottomSheetFragment.EXTRA_MODE)!!
            )
            when (mode)
            {
                RadioModeBottomSheetFragment.RadioMode.WSPR -> showWsprReceiveBottomSheet()
                RadioModeBottomSheetFragment.RadioMode.MFSK -> showMfskReceiveBottomSheet()
            }
        }

        // Start audio device discovery (doesn't require permission)
        viewModel.startAudioDeviceDiscovery()
    }

    override fun onResume()
    {
        super.onResume()

        setupViewByStatus()
        updateByteCounters() // Syncs counter with any text already in the field
    }

    override fun onStart()
    {
        super.onStart()

        // Bind to service if it's running (restores UI state)
        viewModel.bindToWsprServiceIfRunning()
        viewModel.bindToMfskServiceIfRunning()
    }

    override fun onStop()
    {
        super.onStop()

        // Unbind from service (service continues running)
        viewModel.unbindFromWsprService()
        viewModel.unbindFromMfskService()
    }

    /**
     * Sets up observers for connection state from ViewModel.
     */
    private fun setupConnectionObservers()
    {
        // Observer Eden connected state
        coroutineScope.launch {
            viewModel.isEdenConnected.collect { connected ->
                // Show/hide the radio counter and separator
                val radioVisibility = if (connected) View.VISIBLE else View.GONE
                binding.tvRadioByteCounter.visibility = radioVisibility
                binding.tvByteCounterSeparator.visibility = radioVisibility

                if (!connected) binding.serialStatusContainer.visibility = View.GONE

                // Refresh counters immediately
                updateByteCounters()
            }
        }

        // Observe USB audio connection for receive button visibility
        coroutineScope.launch {
            viewModel.canReceiveRadio.collect { canReceive ->
                binding.btnReceiveRadio.visibility = if (canReceive) View.VISIBLE else View.GONE
            }
        }

        // Observe serial connection for send button visibility
        coroutineScope.launch {
            viewModel.canSendViaSerial.collect { canSend ->
                binding.sendViaRadio.visibility = if (canSend) View.VISIBLE else View.GONE
            }
        }

        // Observe receive session state for indicator
        coroutineScope.launch {
            viewModel.wsprReceiveSessionState.collect { state ->
                updateReceiveButtonState(state)
            }
        }

        // Observe received messages and save them
        coroutineScope.launch {
            viewModel.wsprLastReceivedMessage.collect { _ ->
                // Message already saved by service, just refresh UI
                setupViewByStatus()
            }
        }

        // Observe MFSK receive session state for receive button indicator
        coroutineScope.launch {
            viewModel.mfskSessionState.collect { state ->
                updateMfskReceiveButtonState(state)
            }
        }

        // Observe MFSK received messages and refresh message list
        coroutineScope.launch {
            viewModel.mfskLastReceivedMessage.collect { _ ->
                setupViewByStatus()
            }
        }
    }

    /**
     * Updates the receive button appearance based on session state.
     * Animates the button icon when a session is active.
     */
    private fun updateReceiveButtonState(state: WSPRReceiveSessionState)
    {
        when (state) {
            is WSPRReceiveSessionState.Running,
            is WSPRReceiveSessionState.WaitingForWindow -> {
                // Tint green and start pulse animation
                binding.btnReceiveRadio.drawable?.setTint(
                    ContextCompat.getColor(this, R.color.caribbeanGreen)
                )
                startButtonPulseAnimation()
            }

            is WSPRReceiveSessionState.TimedOut -> {
                // Reset button state
                stopButtonAnimation()
                binding.btnReceiveRadio.drawable?.setTint(
                    ContextCompat.getColor(this, R.color.white)
                )
                // Show timeout dialog
                showSessionTimeoutDialog(state.spotsReceived, state.messagesDecrypted)
            }

            is WSPRReceiveSessionState.Idle,
            is WSPRReceiveSessionState.Stopped -> {
                // Reset to white and stop animation
                stopButtonAnimation()
                binding.btnReceiveRadio.drawable?.setTint(
                    ContextCompat.getColor(this, R.color.white)
                )
            }
        }
    }

    /**
     * Starts a pulse animation on the receive button.
     */
    private fun startButtonPulseAnimation()
    {
        // Cancel any existing animation
        indicatorAnimator?.cancel()

        indicatorAnimator = ObjectAnimator.ofFloat(
            binding.btnReceiveRadio,
            "alpha",
            1f, 0.4f, 1f
        ).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            start()
        }
    }

    /**
     * Stops the button animation and resets alpha.
     */
    private fun stopButtonAnimation()
    {
        indicatorAnimator?.cancel()
        indicatorAnimator = null
        binding.btnReceiveRadio.alpha = 1f
    }

    private fun receiveViaRadioClicked()
    {
        // If a sheet of either mode is already open, don't open another
        if (supportFragmentManager.findFragmentByTag("WSPRReceiveRadioBottomSheet") != null) return
        if (supportFragmentManager.findFragmentByTag("MFSKReceiveRadioBottomSheet") != null) return

        // If a session is already active, re-open its sheet directly
        if (viewModel.isWsprSessionActive())
        {
            showWsprReceiveBottomSheet()
            return
        }
        if (viewModel.isMfskSessionActive())
        {
            showMfskReceiveBottomSheet()
            return
        }

        // No active session — validate prerequisites before showing mode selector
        if (!viewModel.usbAudioAvailable.value)
        {
            showAlert(getString(R.string.usb_audio_not_connected))
            return
        }

        if (thisFriend.publicKeyEncoded == null)
        {
            showAlert(getString(R.string.alert_text_verified_friends_only))
            return
        }

        // RECORD_AUDIO is required by the foreground service when it starts.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED)
        {
            pendingReceiveRequest = true
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED)
        {
            showNotificationPermissionExplanation()
            return
        }

        showRadioModeSelector(RadioModeBottomSheetFragment.Purpose.RX)
    }

    private fun showWsprReceiveBottomSheet()
    {
        val bottomSheet = WSPRReceiveRadioBottomSheetFragment()
        bottomSheet.show(supportFragmentManager, "WSPRReceiveRadioBottomSheet")
    }

    private fun showMfskReceiveBottomSheet()
    {
        MFSKReceiveRadioBottomSheetFragment.newInstance()
            .show(supportFragmentManager, "MFSKReceiveRadioBottomSheet")
    }

    private fun showRadioModeSelector(purpose: RadioModeBottomSheetFragment.Purpose)
    {
        RadioModeBottomSheetFragment.newInstance(purpose)
            .show(supportFragmentManager, "RadioModeBottomSheet")
    }

    /**
     * Updates the receive button appearance based on MFSK session state.
     * Mirrors [updateReceiveButtonState] for the WSPR session.
     * Mutual exclusion ensures only one of the two will ever show an active state.
     */
    private fun updateMfskReceiveButtonState(state: MFSKReceiveSessionState)
    {
        when (state)
        {
            is MFSKReceiveSessionState.Starting,
            is MFSKReceiveSessionState.Running ->
            {
                binding.btnReceiveRadio.drawable?.setTint(
                    ContextCompat.getColor(this, R.color.caribbeanGreen)
                )
                startButtonPulseAnimation()
            }

            is MFSKReceiveSessionState.Idle,
            is MFSKReceiveSessionState.Stopped,
            is MFSKReceiveSessionState.Failed ->
            {
                stopButtonAnimation()
                binding.btnReceiveRadio.drawable?.setTint(
                    ContextCompat.getColor(this, R.color.white)
                )
            }
        }
    }

    private fun showNotificationPermissionExplanation()
    {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_AddFriendAlertDialog))

        val title = SpannableString(getString(R.string.radio_session_notification_title))
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 20)

        val textView = TextView(this)
        textView.text = getString(R.string.radio_session_notification_explanation)
        textView.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        container.addView(textView)

        builder.setView(container)

        builder.setPositiveButton(getString(R.string.enable_notification)) { _, _ ->
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        builder.setNeutralButton(getString(R.string.continue_without)) { dialog, _ ->
            dialog.dismiss()
            showRadioModeSelector(RadioModeBottomSheetFragment.Purpose.RX)
        }

        builder.create().show()
    }

    private fun showMicrophonePermissionDeniedDialog()
    {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.microphone_permission_required_title))
            .setMessage(getString(R.string.alert_microphone_permission_required))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.button_label_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppSettings()
    {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun showSessionTimeoutDialog(spotsReceived: Int, messagesDecrypted: Int)
    {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_AddFriendAlertDialog))

        val title = SpannableString(getString(R.string.session_timeout_title))
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 20)

        val summaryText = when
        {
            messagesDecrypted > 0 -> getString(R.string.timeout_with_messages, messagesDecrypted)
            spotsReceived > 0 -> getString(R.string.timeout_with_packets, spotsReceived)
            else -> getString(R.string.timeout_no_signals)
        }

        val textView = TextView(this)
        textView.text = "$summaryText\n\n${getString(R.string.session_timeout_explanation)}"
        textView.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        container.addView(textView)

        builder.setView(container)

        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
            viewModel.resetWsprSession()
        }

        builder.create().show()
    }

    @ExperimentalUnsignedTypes
    private fun receivedSharedMessage()
    {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let{
            //Attempt to decode the message
            decodeStringMessage(it)
        }

        intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java)?.let {
            try
            {
                // See if we received an image message
                val extraStream = intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java)
                if (extraStream != null)
                {
                    (extraStream as? Uri)?.let {
                        decodeImage(it)
                    }
                }
            }
            catch (_:Exception)
            {
                showAlert(getString(R.string.alert_text_unable_to_process_request))
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("MissingSuperCall")
    override fun onBackPressed()
    {
        returnButtonPressed()
    }

    private fun setClickListeners()
    {
        binding.btnResendInvite.setOnClickListener {
            inviteClicked()
        }

        binding.buttonBack.setOnClickListener {
            returnButtonPressed()
        }

        binding.btnReceiveRadio.setOnClickListener {
            receiveViaRadioClicked()
        }

        binding.sendAsText.setOnClickListener {
            if (binding.messageEditText.text.isNotEmpty())
            {
                if (binding.messageEditText.text.toString().toByteArray(Charsets.UTF_8).size > MAX_MESSAGE_BYTES)
                {
                    showAlert(getString(R.string.alert_text_message_too_long))
                }
                else
                {
                    val decodeResult = Codex().decode(binding.messageEditText.text.toString())

                    if (decodeResult != null) showConfirmationForImport()
                    else trySendingOrSavingMessage(isImage = false, saveImage = false)
                }
            }
            else
            {
                showAlert(getString(R.string.alert_text_write_a_message_to_send))
            }
        }

        binding.sendViaRadio.setOnClickListener {
            if (viewModel.isWsprSessionActive() || viewModel.isMfskSessionActive()) {
                showAlert(getString(R.string.alert_transmit_blocked_by_receive))
                return@setOnClickListener
            }

            val message = binding.messageEditText.text.toString()

            if (message.isEmpty()) {
                showAlert(getString(R.string.alert_text_write_a_message_to_send))
                return@setOnClickListener
            }

            if (message.toByteArray(Charsets.UTF_8).size > Eden.MAX_RADIO_MESSAGE_BYTES) {
                showAlert(getString(R.string.alert_text_message_too_long_for_radio))
                return@setOnClickListener
            }

            if (thisFriend.publicKeyEncoded == null) {
                showAlert(getString(R.string.alert_text_verified_friends_only))
                return@setOnClickListener
            }

            // Claim the serial device if available but not yet claimed.
            // No-op if already claimed; triggers USB permission flow if not.
            viewModel.claimSerialDevice()

            showRadioModeSelector(RadioModeBottomSheetFragment.Purpose.TX)
        }

        binding.sendAsImage.setOnClickListener {
            showHideShareImageButtons()
        }

        binding.saveAsImage.setOnClickListener {

            Timber.d("saveAsImage tapped")

            // Show consent dialog before proceeding with image save
            if (hasImageSaveConsentBeenShown())
            {
                trySendingOrSavingMessage(isImage = true, saveImage = true)
            }
            else
            {
                // Only show if the user hasn't opted out
                showImageSaveConsentDialog {
                    trySendingOrSavingMessage(isImage = true, saveImage = true)
                }
            }
        }

        binding.shareAsImage.setOnClickListener {
            Timber.d("shareAsImage tapped")
            trySendingOrSavingMessage(isImage = true, saveImage = false)
        }

        binding.btnImportText.setOnClickListener {
            importInvitationClicked(ImportPurpose.IMPORT_MESSAGE)
        }

        binding.btnImportImage.setOnClickListener {
            handleImageImport()
        }

        binding.btnHelp.setOnClickListener {
            val slideActivity = Intent(this, SlideActivity::class.java)
            slideActivity.putExtra(Intent.EXTRA_TEXT, slideNameChat)
            startActivity(slideActivity)
        }

        binding.profilePicture.setOnClickListener {
            showMenuFragment()
        }

        binding.tvFriendName.setOnClickListener {
            showMenuFragment()
        }

        // Update byte counters on every keystroke.
        binding.messageEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { updateByteCounters() }
        })
    }

    private fun showConfirmationForImport()
    {
        val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_AddFriendAlertDialog))
        val title = SpannableString(getString(R.string.import_text))

        // alert dialog title align center
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)

        val alertDialogContent = LinearLayout(this)
        alertDialogContent.orientation = LinearLayout.VERTICAL

        // Set the input - EditText
        val textView = TextView(this)
        textView.setBackgroundResource(R.drawable.btn_bkgd_light_grey_outline_8)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        textView.text = getString(R.string.confirmation_for_import)
        textView.compoundDrawablePadding = 10
        textView.setPadding(25)
        textView.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        alertDialogContent.addView(textView)

        builder.setView(alertDialogContent)

        // Set the Add and Cancel Buttons
        builder.setPositiveButton(resources.getString(R.string.yes))
        { _, _->
            decodeStringMessage(binding.messageEditText.text.toString())
            binding.messageEditText.setText("")
        }

        builder.setNeutralButton(resources.getString(R.string.no))
        { dialog, _->
            trySendingOrSavingMessage(isImage = false, saveImage = false)
            dialog.cancel()
        }
            .create()
            .show()
    }

    /**
     * Shows an informed consent dialog before saving an encoded image to shared storage.
     * Explains that the image will be visible in the gallery and accessible to other apps.
     * Includes a "Don't show again" option.
     */
    private fun showImageSaveConsentDialog(onConsent: () -> Unit)
    {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_AddFriendAlertDialog))
        val title = SpannableString("Save to Gallery?")

        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)

        // Create container for message and checkbox
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 20)

        // Message text
        val message = """
        The encoded image will be saved to your device's Pictures folder where:
        
        • It will appear in your gallery app
        • Other apps can access it
        • It remains visible after uninstalling Nahoft
        
        The hidden message is encrypted and can only be decoded with your keys.
        """.trimIndent()

        val textView = TextView(this)
        textView.text = message
        textView.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        container.addView(textView)

        // "Don't show again" checkbox
        val checkBox = android.widget.CheckBox(this)
        checkBox.text = "Don't show this again"
        checkBox.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        checkBox.setPadding(0, 30, 0, 0)
        container.addView(checkBox)

        builder.setView(container)

        builder.setPositiveButton("Save to Gallery") { _, _ ->
            // Save preference if checkbox is checked
            if (checkBox.isChecked) {
                markImageSaveConsentShown()
            }
            onConsent()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.create().show()
    }

    /**
     * Checks if the user has already seen and accepted the image save consent dialog.
     */
    private fun hasImageSaveConsentBeenShown(): Boolean
    {
        return Persist.loadBooleanKey(Persist.sharedPrefImageSaveConsentShownKey)
    }

    /**
     * Marks that the user has seen the image save consent dialog.
     */
    private fun markImageSaveConsentShown()
    {
        Persist.saveBooleanKey(Persist.sharedPrefImageSaveConsentShownKey, true)
    }

    private fun showHideShareImageButtons()
    {
        val revealing = !isShareImageButtonShow

        if (revealing) {
            binding.shareAsImage.isVisible = true
            binding.saveAsImage.isVisible = true
        }

        binding.shareAsImage.animate().apply {
            duration = 500
            translationY(if (revealing) 0F else 175F)
        }.withEndAction {
            if (!revealing) binding.shareAsImage.isInvisible = true
        }

        binding.saveAsImage.animate().apply {
            duration = 500
            translationY(if (revealing) 0F else 175F)
        }.withEndAction {
            if (!revealing) binding.saveAsImage.isInvisible = true
        }

        isShareImageButtonShow = !isShareImageButtonShow
    }

    private fun returnButtonPressed()
    {
        val lastFragment = supportFragmentManager.fragments.last()
        if (lastFragment.tag == menuFragmentTag) setupViewByStatus()
        else finish()
    }

    private fun showMenuFragment() {
        val ft = supportFragmentManager.beginTransaction()
        val codex = Codex()
        val friendCode =
            if (thisFriend.status == FriendStatus.Approved || thisFriend.status == FriendStatus.Verified) {
                codex.encodeKey(PublicKey(thisFriend.publicKeyEncoded).toBytes())
            }
            else ""

        val userCode = codex.encodeKey(Encryption().ensureKeysExist().toBytes())
        ft.replace(
            R.id.frame_placeholder,
            MenuFragment.newInstance(thisFriend, userCode, friendCode),
            menuFragmentTag
        )
        ft.commit()
        binding.btnImportImage.isVisible = false
        binding.btnImportText.isVisible = false
        binding.btnResendInvite.isVisible = false
        binding.sendMessageContainer.isVisible = false
        if (isShareImageButtonShow) showHideShareImageButtons()
    }

    private fun setupViewByStatus()
    {
        binding.tvFriendName.text =
            if (thisFriend.name.length <= 10) thisFriend.name
            else thisFriend.name.take(8) + "..."

        binding.profilePicture.text = thisFriend.name.take(1)

        val ft = supportFragmentManager.beginTransaction()

        when (thisFriend.status)
        {
            FriendStatus.Default -> {
                binding.statusIconImageView.setImageResource(FriendStatus.Default.getIcon())
                ft.replace(R.id.frame_placeholder, DefaultStatusFragment.newInstance(thisFriend))
                ft.commit()
                binding.btnImportImage.isVisible = false
                binding.btnImportText.isVisible = false
                binding.btnResendInvite.isVisible = false
                binding.compositionArea.isVisible = false
            }

            FriendStatus.Requested -> {
                binding.statusIconImageView.setImageResource(FriendStatus.Requested.getIcon())
                ft.replace(R.id.frame_placeholder, RequestedStatusFragment.newInstance(thisFriend))
                ft.commit()
                binding.btnImportImage.isVisible = false
                binding.btnImportText.isVisible = false
                binding.btnResendInvite.isVisible = false
                binding.compositionArea.isVisible = false
            }

            FriendStatus.Invited -> {
                binding.statusIconImageView.setImageResource(FriendStatus.Invited.getIcon())
                ft.replace(R.id.frame_placeholder, InvitedStatusFragment.newInstance(thisFriend))
                ft.commit()
                binding.btnImportImage.isVisible = false
                binding.btnImportText.isVisible = false
                binding.btnResendInvite.isVisible = true
                binding.compositionArea.isVisible = false
            }

            FriendStatus.Verified -> {
                binding.statusIconImageView.setImageResource(FriendStatus.Verified.getIcon())
                ft.replace(R.id.frame_placeholder, VerifiedStatusFragment.newInstance(thisFriend))
                ft.commit()
                binding.btnImportImage.isVisible = true
                binding.btnImportText.isVisible = true
                binding.btnResendInvite.isVisible = false
                binding.compositionArea.isVisible = true
                binding.verifiedStatusIconImageView.isVisible = true
            }

            FriendStatus.Approved -> {
                binding.statusIconImageView.setImageResource(FriendStatus.Approved.getIcon())
                ft.replace(R.id.frame_placeholder, VerifiedStatusFragment.newInstance(thisFriend))
                ft.commit()
                binding.btnImportImage.isVisible = true
                binding.btnImportText.isVisible = true
                binding.btnResendInvite.isVisible = false
                binding.compositionArea.isVisible = true
            }
        }
    }

    fun inviteClicked()
    {
        // Get user's public key to send to contact
        val userPublicKey = Encryption().ensureKeysExist()
        val keyBytes = userPublicKey.toBytes()
        ShareUtil.shareKey(this, keyBytes)

        // Share the key
//        if (Persist.loadBooleanKey(Persist.sharedPrefUseSmsAsDefaultKey)) { // && (thisFriend.phone?.isNotEmpty() == true)) {
//            try {
//                val codex = Codex()
//                val encodedKey = codex.encodeKey(keyBytes)
//                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
//                    this.getSystemService(SmsManager::class.java)
//                } else {
//                    SmsManager.getDefault()
//                }
//                val parts = smsManager.divideMessage(encodedKey)
//                smsManager.sendMultipartTextMessage(
//                    thisFriend.phone,
//                    null,
//                    parts,
//                    null,
//                    null
//                )
//            } catch (e: Exception) {
//                this.showAlert(getString(R.string.unable_to_send_sms))
//                return
//            }
//        } else {
//          ShareUtil.shareKey(this, keyBytes)
//        }

        if (thisFriend.status == FriendStatus.Requested)
        {
            // We have already received an invitation from this friend.
            // Set friend status to approved.
            thisFriend.status = FriendStatus.Approved
            Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Approved)
            setupViewByStatus()
        }
        else
        {
            // We have not received an invitation from this friend.
            // Set friend status to Invited
            thisFriend.status = FriendStatus.Invited
            Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Invited)
            setupViewByStatus()
        }
    }

    fun importInvitationClicked(purpose: ImportPurpose)
    {
        val builder = AlertDialog.Builder(
            ContextThemeWrapper(this, R.style.AppTheme_AddFriendAlertDialog)
        )

        // Title and hint are chosen by purpose.
        val titleResId = when (purpose) {
            ImportPurpose.IMPORT_KEY     -> R.string.dialog_title_import_key
            ImportPurpose.IMPORT_MESSAGE -> R.string.dialog_title_import_message
        }

        val hintResId = when (purpose) {
            ImportPurpose.IMPORT_KEY     -> R.string.import_dialog_hint_key
            ImportPurpose.IMPORT_MESSAGE -> R.string.import_dialog_hint_message
        }

        val title = SpannableString(getString(titleResId))
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0, title.length, 0
        )
        builder.setTitle(title)

        val dialogView = layoutInflater.inflate(R.layout.dialog_import, null)
        val inputEditText = dialogView.findViewById<EditText>(R.id.import_edit_text)
        val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.button_cancel)
        val buttonPaste  = dialogView.findViewById<MaterialButton>(R.id.button_paste)
        val buttonImport = dialogView.findViewById<MaterialButton>(R.id.button_import)

        inputEditText.hint = getString(hintResId)
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        // Cancel: dismiss without acting on input.
        buttonCancel.setOnClickListener {
            dialog.cancel()
        }

        // Paste: fill the field from clipboard, leaving the dialog open so the user
        // can review the pasted content before tapping Import.
        buttonPaste.setOnClickListener {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboardManager.primaryClip

            if (clip == null || clip.itemCount == 0) {
                showAlert(getString(R.string.alert_text_clipboard_empty))
                return@setOnClickListener
            }

            val pastedText = clip.getItemAt(0).coerceToText(this).toString()

            if (pastedText.isEmpty()) {
                showAlert(getString(R.string.alert_text_clipboard_empty))
                return@setOnClickListener
            }

            inputEditText.setText(pastedText)
        }

        // Import: validate the field before decoding. Dismiss only on success so
        // the user can correct an empty/invalid field without re-opening the dialog.
        buttonImport.setOnClickListener {
            if (inputEditText.text.isEmpty()) {
                showAlert(getString(R.string.alert_text_paste_key_before_import))
                return@setOnClickListener
            }
            decodeStringMessage(inputEditText.text.toString())
            dialog.dismiss()
        }
    }

    private fun trySendingOrSavingMessage(isImage: Boolean, saveImage: Boolean)
    {
        // Make sure there is a message to send
        val message = binding.messageEditText.text.toString()

        if (message.isBlank()) {
            showAlert(getString(R.string.alert_text_write_a_message_to_send))
            return
        }

        if (message.toByteArray(Charsets.UTF_8).size > MAX_MESSAGE_BYTES) {
            showAlert(getString(R.string.alert_text_message_too_long))
            return
        }

        if (isImage)
        {
            // If the message is sent as an image
            ActivityCompat.requestPermissions(
                this@FriendInfoActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
            pickImageFromGallery(saveImage)
        }
        else
        {
            // If the message is sent as text
            if (thisFriend.publicKeyEncoded != null)
            {
                val encryptedMessage = Encryption().encrypt(thisFriend.publicKeyEncoded!!, message)
                ShareUtil.shareText(this, message, thisFriend.publicKeyEncoded!!)
                saveMessage(encryptedMessage, thisFriend, true)

                binding.messageEditText.text?.clear()
            }
            else
            {
                this.showAlert(getString(R.string.alert_text_verified_friends_only))
                return
            }
        }
    }

    private fun pickImageFromGallery(saveImage: Boolean)
    {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        if (saveImage) pickImageForSavingLauncher.launch(request)
        else pickImageForSharingLauncher.launch(request)
    }

    private fun handleImageImport()
    {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        pickImageForImportLauncher.launch(request)
    }

    @ExperimentalUnsignedTypes
    private fun handlePickedImageForEncoding(imageUri: Uri, saveImage: Boolean)
    {
        // We can only share/save an image if a recipient with a public key has been selected
        thisFriend.publicKeyEncoded?.let { publicKey ->
            val message = binding.messageEditText.text.toString()
            binding.imageImportProgressBar.visibility = View.VISIBLE
            shareOrSaveAsImage(imageUri, message, publicKey, saveImage)
            binding.messageEditText.text?.clear()
        }
    }

    @ExperimentalUnsignedTypes
    private fun shareOrSaveAsImage(imageUri: Uri, message: String, encodedFriendPublicKey: ByteArray, saveImage: Boolean)
    {
        try
        {
            // Encrypt the message
            val encryptedMessage = Encryption().encrypt(encodedFriendPublicKey, message)
            makeWait()

            // Encode the image off the main thread
            val newUri: Deferred<Uri?> =
                coroutineScope.async(Dispatchers.IO) {
                    val swatch = Encoder()
                    swatch.encode(
                        applicationContext,
                        encryptedMessage,
                        imageUri,
                        saveImage
                    )
                }

            coroutineScope.launch(Dispatchers.Main) {
                try
                {
                    val maybeUri = newUri.await()
                    noMoreWaiting()

                    if (maybeUri != null)
                    {
                        if (saveImage)
                        {
                            showAlert(getString(R.string.alert_text_image_saved))
                        }
                        else
                        {
                            ShareUtil.shareImage(applicationContext, maybeUri)
                        }

                        saveMessage(encryptedMessage, thisFriend, true)
                    }
                    else
                    {
                        showAlert(getString(R.string.alert_text_unable_to_process_request))
                    }
                }
                catch (e: Exception)
                {
                    Timber.e(e, "shareOrSaveAsImage: encode coroutine failed")
                    noMoreWaiting()
                    showAlert(getString(R.string.alert_text_unable_to_process_request))
                }
            }

        }
        catch (e: SecurityException)
        {
            Timber.e(e, "shareOrSaveAsImage: encrypt failed")
            noMoreWaiting()
            showAlert(getString(R.string.alert_text_unable_to_process_request))
        }
    }

    @ExperimentalUnsignedTypes
    private fun decodeImage(imageUri: Uri)
    {
        makeWait()

        val decodeResult: Deferred<ByteArray?> =
            coroutineScope.async(Dispatchers.IO) {
                val swatch = Decoder()
                return@async swatch.decode(applicationContext, imageUri)
            }

        coroutineScope.launch(Dispatchers.Main) {
            try
            {
                val maybeDecodeResult = decodeResult.await()
                noMoreWaiting()

                if (maybeDecodeResult != null)
                {
                    decodePayload = maybeDecodeResult
                    handleImageDecodeResult()
                }
                else
                {
                    showAlert(getString(R.string.alert_text_unable_to_decode_message))
                }
            }
            catch (_: Exception)
            {
                noMoreWaiting()
                showAlert(getString(R.string.alert_text_unable_to_decode_message))
            }
        }
    }

    private fun handleImageDecodeResult() {
        if (decodePayload == null)
        {
            showAlert(getString(R.string.alert_text_unable_to_decode_message))
            return
        }
        saveMessage(decodePayload!!, thisFriend, false)
    }

    private fun saveMessage(cipherBytes: ByteArray, messageSender: Friend, fromMe: Boolean) {
        val newMessage = Message(cipherBytes, messageSender, fromMe)
        newMessage.save(this)

        // Add to messages
        setupViewByStatus()
    }

    private fun makeWait() {
        binding.imageImportProgressBar.visibility = View.VISIBLE
        binding.btnImportImage.isEnabled = false
        binding.btnImportImage.isClickable = false
    }

    private fun noMoreWaiting() {
        binding.imageImportProgressBar.visibility = View.INVISIBLE
        binding.btnImportImage.isEnabled = true
        binding.btnImportImage.isClickable = true
    }

    private fun decodeStringMessage(messageString: String) {
        // Update UI to reflect text being shared
        val decodeResult = Codex().decode(messageString)

        if (decodeResult != null)
        {
            when (decodeResult.type)
            {
                KeyOrMessage.EncryptedMessage ->
                {
                    // Check for message type if user is not approved
                    if (thisFriend.status == FriendStatus.Invited) {
                        this.showAlert("The input was a message. You have to import your friend's public key.")
                        return
                    }

                    // Create Message Instance
                    val newMessage = Message(decodeResult.payload, thisFriend, false)
                    newMessage.save(this)

                    // Add to messages
                    setupViewByStatus()
                }
                KeyOrMessage.Key ->
                {
                    updateKeyAndStatus(decodeResult.payload)
                }
            }
        }
        else
        {
            this.showAlert(getString(R.string.alert_text_unable_to_decode_message))
        }
    }

    private fun updateKeyAndStatus(keyData: ByteArray)
    {
        // Reject if the user is trying to add their own public key as a contact's key.
        if (isOwnPublicKey(keyData))
        {
            showOwnKeyRejectedDialog()
            return
        }

        when (thisFriend.status)
        {
            FriendStatus.Default ->
            {
                Persist.updateFriend(
                    context = this,
                    friendToUpdate = thisFriend,
                    newStatus = FriendStatus.Requested,
                    encodedPublicKey = keyData
                )

                thisFriend.status = FriendStatus.Requested
                thisFriend.publicKeyEncoded = keyData
                setupViewByStatus()
            }
            FriendStatus.Invited ->
            {
                Persist.updateFriend(
                    context = this,
                    friendToUpdate = thisFriend,
                    newStatus = FriendStatus.Approved,
                    encodedPublicKey = keyData
                )

                thisFriend.status = FriendStatus.Approved
                thisFriend.publicKeyEncoded = keyData
                setupViewByStatus()
            }
            else ->
                this.showAlert(getString(R.string.alert_text_unable_to_update_friend_status))
        }
    }

    /**
     * Returns true if [keyData] matches the user's own public key.
     * Used to prevent users from adding their own key as a contact's key.
     */
    private fun isOwnPublicKey(keyData: ByteArray): Boolean
    {
        val ownKey = Encryption().ensureKeysExist().toBytes()
        return keyData.contentEquals(ownKey)
    }

    private fun showOwnKeyRejectedDialog()
    {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.own_key_rejected_title))
            .setMessage(getString(R.string.own_key_rejected_message))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun approveVerifyFriend()
    {
        Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Verified,
            encodedPublicKey = thisFriend.publicKeyEncoded)
        thisFriend.status = FriendStatus.Verified
        setupViewByStatus()
    }

    fun declineVerifyFriend() {
        thisFriend.publicKeyEncoded = null
        Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Default)
        thisFriend.status = FriendStatus.Default
        setupViewByStatus()
    }

    private fun sendToLogin() {
        // If the status is not either NotRequired, or Logged in, request login
        this.showAlert(getString(R.string.alert_text_passcode_required_to_proceed))
        // Send user to the Login Activity
        val loginIntent = Intent(applicationContext, LogInActivity::class.java)
        startActivity(loginIntent)
        finish()
    }

    fun changeFriendsName(newName: String)
    {
        Persist.updateFriend(this, thisFriend, newName)
        thisFriend.name = if (newName.length <= 10) newName else newName.take(8) + "..."
        binding.tvFriendName.text = thisFriend.name
        binding.profilePicture.text = thisFriend.name.take(1)
        showAlert("New name saved")
    }

    fun Activity.hideSoftKeyboard(editText: EditText) {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(editText.windowToken, 0)
        }
    }

    /**
     * Returns a color resource for the byte counter based on how
     * close the current count is to the limit.
     *   < 80%  → coolGrey (normal)
     *   80–99% → amberWarning (approaching limit)
     *   ≥ 100% → madderLake (at or over limit)
     */
    private fun counterColor(byteCount: Int, limit: Int): Int
    {
        val ratio = byteCount.toFloat() / limit

        return when
        {
            ratio >= 1f   -> R.color.madderLake
            ratio >= 0.8f -> R.color.amberWarning
            else          -> R.color.coolGrey
        }
    }

    /**
     * Recomputes the UTF-8 byte length of the current message input and updates
     * both counter TextViews with the current count and appropriate color.
     * The radio counter is only updated when it's visible (Eden connected).
     */
    private fun updateByteCounters()
    {
        val byteCount = binding.messageEditText.text
            .toString()
            .toByteArray(Charsets.UTF_8)
            .size

        // Message counter is always visible
        binding.tvMessageByteCounter.text = getString(
            R.string.byte_counter_message_format, byteCount, MAX_MESSAGE_BYTES
        )
        binding.tvMessageByteCounter.setTextColor(
            ContextCompat.getColor(this, counterColor(byteCount, MAX_MESSAGE_BYTES))
        )

        // Radio counter only needs updating when Eden is connected
        if (binding.tvRadioByteCounter.isVisible)
        {
            binding.tvRadioByteCounter.text = getString(
                R.string.byte_counter_radio_format, byteCount, Eden.MAX_RADIO_MESSAGE_BYTES
            )
            binding.tvRadioByteCounter.setTextColor(
                ContextCompat.getColor(this, counterColor(byteCount, Eden.MAX_RADIO_MESSAGE_BYTES))
            )
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()

        // Note: Do NOT call viewModel.stopReceiveSession() here
        // The service should continue running when activity is destroyed
        stopButtonAnimation()
        parentJob.cancel()
    }
}
