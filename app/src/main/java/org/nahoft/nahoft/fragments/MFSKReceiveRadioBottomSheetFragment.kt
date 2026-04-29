package org.nahoft.nahoft.fragments

import android.animation.ObjectAnimator
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.animation.ValueAnimator
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.nahoft.nahoft.R
import org.nahoft.nahoft.databinding.FragmentBottomSheetMfskReceiveRadioBinding
import org.nahoft.nahoft.models.DecryptedMessageRecord
import org.nahoft.nahoft.services.MFSKReceiveSessionState
import org.nahoft.nahoft.viewmodels.FriendInfoViewModel
import org.operatorfoundation.signalbridge.models.AudioLevelInfo

/**
 * Bottom sheet for receiving encrypted messages via MFSK-16 radio.
 *
 * Simpler than [ReceiveRadioBottomSheetFragment]:
 * - No cycle progress bar (MFSK has no fixed timing windows)
 * - No spots card or packet requirement tracking (MFSK delivers complete messages)
 * - No encryption toggle (MFSK RX always expects encrypted payloads)
 * - [MFSKReceiveSessionState.Failed] covers both errors and session timeout
 *
 * Uses the activity-scoped [FriendInfoViewModel] for service binding and state relay.
 * Session is started via [FriendInfoViewModel.startMfskReceiveSession].
 */
class MFSKReceiveRadioBottomSheetFragment : BottomSheetDialogFragment()
{
    // ==================== View Binding ====================

    private var _binding: FragmentBottomSheetMfskReceiveRadioBinding? = null
    private val binding get() = _binding!!

    // ==================== ViewModel ====================

    private val viewModel: FriendInfoViewModel by activityViewModels()

    // ==================== UI State ====================

    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentAnimator: ObjectAnimator? = null
    private var elapsedTimeJob: Job? = null

    private enum class AnimationType { NONE, PULSE, ROTATE }

    // ==================== Lifecycle ====================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        _binding = FragmentBottomSheetMfskReceiveRadioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = false

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isHideable = true
        }

        setupClickListeners()
        observeViewModel()
        startElapsedTimeUpdates()

        // Restore correct UI state if session is already active when sheet opens
        if (viewModel.isMfskSessionActive())
            showFrequencyReadOnly()
        else
            showFrequencyInput()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        currentAnimator?.cancel()
        elapsedTimeJob?.cancel()
        uiScope.cancel()
        _binding = null
    }

    // ==================== Setup ====================

    private fun setupClickListeners()
    {
        // Minimize — keeps session running
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }

        // Pre-fill frequency from saved preference
        binding.etMfskRxFrequency.setText(viewModel.getMfskBaseFrequencyHz().toString())

        binding.btnMfskRxFreqMinus.setOnClickListener {
            val current = currentFrequencyInput()
            binding.etMfskRxFrequency.setText((current - 1).toString())
        }

        binding.btnMfskRxFreqPlus.setOnClickListener {
            val current = currentFrequencyInput()
            binding.etMfskRxFrequency.setText((current + 1).toString())
        }

        // Messages card — opens received messages dialog
        binding.cardMessages.setOnClickListener { showReceivedMessagesDialog() }
    }

    // ==================== ViewModel Observation ====================

    private fun observeViewModel()
    {
        uiScope.launch {
            viewModel.mfskSessionState.collect { state ->
                updateSessionStateUI(state)
            }
        }

        uiScope.launch {
            viewModel.mfskAudioLevel.collect { info ->
                info?.let { updateAudioLevel(it) }
            }
        }

        uiScope.launch {
            viewModel.mfskMessageJustReceived.collect { received ->
                if (received)
                {
                    showMessageReceivedCelebration()
                    viewModel.clearMfskMessageReceivedFlag()
                }
            }
        }

        uiScope.launch {
            viewModel.mfskDecryptedMessageRecords.collect { records ->
                updateMessagesCard(records)
            }
        }

        // Enable/disable Start button based on USB audio availability
        uiScope.launch {
            viewModel.usbAudioAvailable.collect { available ->
                if (viewModel.mfskSessionState.value == MFSKReceiveSessionState.Idle)
                {
                    binding.btnStop.isEnabled = available
                    updateStatus(
                        if (available) getString(R.string.tap_start_to_listen)
                        else getString(R.string.usb_audio_not_connected)
                    )
                }
            }
        }
    }

    // ==================== Session State UI ====================

    private fun updateSessionStateUI(state: MFSKReceiveSessionState)
    {
        if (_binding == null) return

        // Error label is hidden unless state is Failed
        binding.tvError.visibility = View.GONE

        when (state)
        {
            is MFSKReceiveSessionState.Idle ->
            {
                updateStateIcon(R.drawable.ic_radio, R.color.coolGrey, AnimationType.NONE)
                updateStatus(getString(R.string.tap_start_to_listen))
                showFrequencyInput()
                binding.audioLevelSection.visibility = View.GONE
                binding.vuMeter.reset()
            }

            is MFSKReceiveSessionState.Starting ->
            {
                updateStateIcon(R.drawable.ic_sync, R.color.tangerine, AnimationType.ROTATE)
                updateStatus(getString(R.string.mfsk_rx_starting))
                showFrequencyReadOnly()
            }

            is MFSKReceiveSessionState.Running ->
            {
                updateStateIcon(R.drawable.ic_radio, R.color.caribbeanGreen, AnimationType.PULSE)
                updateStatus(getString(R.string.mfsk_listening_for_signals))
                showFrequencyReadOnly()
                binding.audioLevelSection.visibility = View.VISIBLE
            }

            is MFSKReceiveSessionState.Stopped ->
            {
                updateStateIcon(R.drawable.ic_sync, R.color.coolGrey, AnimationType.NONE)
                updateStatus(getString(R.string.session_stopped))
                showFrequencyInput()
                binding.audioLevelSection.visibility = View.GONE
                binding.vuMeter.reset()
            }

            is MFSKReceiveSessionState.Failed ->
            {
                updateStateIcon(R.drawable.ic_error, R.color.madderLake, AnimationType.NONE)
                updateStatus(getString(R.string.mfsk_rx_session_failed))
                binding.tvError.text = state.reason
                binding.tvError.visibility = View.VISIBLE
                showFrequencyInput()
                binding.audioLevelSection.visibility = View.GONE
                binding.vuMeter.reset()
            }
        }
    }

    private fun showMessageReceivedCelebration()
    {
        if (_binding == null || !isAdded) return
        updateStatus(getString(R.string.message_received))
        updateStateIcon(R.drawable.ic_success, R.color.caribbeanGreen, AnimationType.NONE)
    }

    // ==================== Frequency Section ====================

    /**
     * Shows the frequency stepper in editable mode and wires the Start button.
     * The session does not start until the user confirms.
     */
    private fun showFrequencyInput()
    {
        binding.rxFrequencySection.visibility  = View.VISIBLE
        binding.etMfskRxFrequency.isEnabled    = true
        binding.btnMfskRxFreqMinus.isEnabled   = true
        binding.btnMfskRxFreqPlus.isEnabled    = true

        binding.btnClose.visibility = View.GONE
        binding.btnStop.text = getString(R.string.start_session)

        val usbAvailable = viewModel.usbAudioAvailable.value
        binding.btnStop.isEnabled = usbAvailable
        updateStatus(
            if (usbAvailable) getString(R.string.tap_start_to_listen)
            else getString(R.string.usb_audio_not_connected)
        )

        binding.btnStop.setOnClickListener {
            val freqHz = currentFrequencyInput()
            viewModel.saveMfskBaseFrequencyHz(freqHz)
            viewModel.startMfskReceiveSession()
            showFrequencyReadOnly()
        }
    }

    /**
     * Shows the frequency as a read-only label during an active session.
     */
    private fun showFrequencyReadOnly()
    {
        binding.rxFrequencySection.visibility  = View.VISIBLE
        binding.etMfskRxFrequency.isEnabled    = false
        binding.btnMfskRxFreqMinus.isEnabled   = false
        binding.btnMfskRxFreqPlus.isEnabled    = false

        binding.btnClose.visibility = View.VISIBLE
        binding.btnStop.isEnabled   = true
        binding.btnStop.text = getString(R.string.stop_session)

        binding.btnStop.setOnClickListener {
            viewModel.stopMfskReceiveSession()
            viewModel.resetMfskSession()
            dismissAllowingStateLoss()
        }
    }

    private fun currentFrequencyInput(): Int =
        binding.etMfskRxFrequency.text.toString().toIntOrNull()
            ?: viewModel.getMfskBaseFrequencyHz()

    // ==================== Cards / Dialogs ====================

    private fun updateMessagesCard(records: List<DecryptedMessageRecord>)
    {
        if (_binding == null) return
        binding.tvMessagesReceived.text = records.size.toString()
    }

    private fun showReceivedMessagesDialog()
    {
        // Note: ReceivedMessagesDialogFragment looks up messages from Persist.messageList
        // by friend name and timestamp. MFSK messages are saved identically to WSPR messages,
        // so this dialog works for both modes without modification.
        // Verify in the wiring step that the dialog's DecryptedMessageRecord source
        // is switched to mfskDecryptedMessageRecords when opened from this sheet.
        ReceivedMessagesDialogFragment.newInstance(isMfsk = true)
            .show(childFragmentManager, "MFSKReceivedMessagesDialog")
    }

    // ==================== Audio Level ====================

    private fun updateAudioLevel(info: AudioLevelInfo)
    {
        if (_binding == null) return
        binding.vuMeter.update(info)
    }

    // ==================== Elapsed Time ====================

    private fun startElapsedTimeUpdates()
    {
        elapsedTimeJob = uiScope.launch {
            while (isActive)
            {
                val elapsedMs = viewModel.getMfskSessionElapsedMs()
                val minutes   = (elapsedMs / 60000).toInt()
                val seconds   = ((elapsedMs % 60000) / 1000).toInt()

                if (_binding != null)
                {
                    binding.tvElapsedTime.text = String.format("%d:%02d", minutes, seconds)
                }

                delay(1000)
            }
        }
    }

    // ==================== Status / Icon ====================

    private fun updateStatus(message: String)
    {
        if (_binding != null && isAdded) binding.tvStatus.text = message
    }

    private fun updateStateIcon(iconRes: Int, colorRes: Int, animationType: AnimationType)
    {
        if (_binding == null) return

        currentAnimator?.cancel()
        currentAnimator = null
        binding.ivStateIcon.rotation = 0f
        binding.ivStateIcon.scaleX   = 1f
        binding.ivStateIcon.scaleY   = 1f
        binding.ivStateIcon.alpha    = 1f

        binding.ivStateIcon.setImageResource(iconRes)
        binding.ivStateIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), colorRes),
            PorterDuff.Mode.SRC_IN
        )

        when (animationType)
        {
            AnimationType.PULSE  -> startPulseAnimation()
            AnimationType.ROTATE -> startRotateAnimation()
            AnimationType.NONE   -> {}
        }
    }

    private fun startPulseAnimation()
    {
        currentAnimator = ObjectAnimator.ofFloat(
            binding.ivStateIcon, "alpha", 1f, 0.4f, 1f
        ).apply {
            duration    = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode  = android.animation.ValueAnimator.RESTART
            start()
        }
    }

    private fun startRotateAnimation()
    {
        currentAnimator = ObjectAnimator.ofFloat(
            binding.ivStateIcon, "rotation", 0f, 360f
        ).apply {
            duration     = 1500
            repeatCount  = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    // ==================== Companion ====================

    companion object
    {
        fun newInstance(): MFSKReceiveRadioBottomSheetFragment =
            MFSKReceiveRadioBottomSheetFragment()
    }
}