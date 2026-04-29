package org.nahoft.nahoft.fragments

import android.animation.ObjectAnimator
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.animation.ValueAnimator
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
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
import org.nahoft.nahoft.databinding.FragmentBottomSheetMfskTransmitRadioBinding
import org.nahoft.nahoft.services.MFSKTransmitSessionState
import org.nahoft.nahoft.viewmodels.MFSKTransmitRadioViewModel
import timber.log.Timber

/**
 * Bottom sheet for transmitting encrypted messages via MFSK-16 radio.
 *
 * Simpler than [WSPRTransmitRadioBottomSheetFragment]:
 * - No window-waiting step (MFSK transmits immediately after encoding)
 * - No spot progress dots (single continuous transmission)
 * - No encryption toggle (MFSK TX is always encrypted)
 * - Progress bar is time-based, driven by [MFSKTransmitSessionState.Transmitting.totalDurationMs]
 *
 * The full TX pipeline runs in [MFSKTransmitSessionService] via [MFSKTransmitRadioViewModel].
 * This fragment is purely a UI observer.
 */
class MFSKTransmitRadioBottomSheetFragment : BottomSheetDialogFragment()
{
    // ==================== View Binding ====================

    private var _binding: FragmentBottomSheetMfskTransmitRadioBinding? = null
    private val binding get() = _binding!!

    // ==================== ViewModel ====================

    private val viewModel: MFSKTransmitRadioViewModel by viewModels {
        MFSKTransmitRadioViewModel.Factory(
            message         = requireArguments().getString(ARG_MESSAGE)!!,
            friendName      = requireArguments().getString(ARG_FRIEND_NAME)!!,
            friendPublicKey = requireArguments().getByteArray(ARG_FRIEND_PUBLIC_KEY)!!
        )
    }

    // ==================== UI State ====================

    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentAnimator: ObjectAnimator? = null
    private var progressJob: Job? = null
    private var transmissionCompleted = false

    // Recorded once when Transmitting state first arrives — drives the progress bar.
    // transmitStartMs == 0L means no transmission has started yet.
    private var transmitStartMs = 0L
    private var totalDurationMs = 0L

    private enum class AnimationType { NONE, PULSE, ROTATE }

    // ==================== Lifecycle ====================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        _binding = FragmentBottomSheetMfskTransmitRadioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = false

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isHideable = false
        }

        setupStaticUI()
        setupClickListeners()
        observeViewModel()
    }

    override fun onDismiss(dialog: android.content.DialogInterface)
    {
        super.onDismiss(dialog)
        if (transmissionCompleted)
        {
            setFragmentResult(RESULT_TX_COMPLETE, Bundle.EMPTY)
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        currentAnimator?.cancel()
        cancelProgress()
        uiScope.cancel()
        _binding = null
    }

    // ==================== Setup ====================

    private fun setupStaticUI()
    {
        binding.tvMessagePreview.text = requireArguments().getString(ARG_MESSAGE)
        binding.etMfskFrequency.setText(viewModel.getMfskBaseFrequencyHz().toString())
        binding.progressTransmission.max = PROGRESS_MAX
    }

    private fun setupClickListeners()
    {
        binding.btnMfskFreqMinus.setOnClickListener {
            binding.etMfskFrequency.setText((currentFrequencyInput() - 1).toString())
        }

        binding.btnMfskFreqPlus.setOnClickListener {
            binding.etMfskFrequency.setText((currentFrequencyInput() + 1).toString())
        }

        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }

        // Initial click listener for Idle state
        setStartClickListener()
    }

    // ==================== ViewModel Observation ====================

    private fun observeViewModel()
    {
        uiScope.launch {
            viewModel.transmitSessionState.collect { state -> updateUI(state) }
        }

        // Eden availability controls Start button in Idle state only
        uiScope.launch {
            viewModel.isEdenConnected.collect { connected ->
                if (viewModel.transmitSessionState.value == MFSKTransmitSessionState.Idle)
                {
                    binding.btnAction.isEnabled = connected
                    if (!connected) updateStatus(getString(R.string.alert_text_serial_not_connected))
                }
            }
        }
    }

    // ==================== State-Driven UI ====================

    private fun updateUI(state: MFSKTransmitSessionState)
    {
        if (_binding == null) return

        when (state)
        {
            is MFSKTransmitSessionState.Idle        -> showIdleState()
            is MFSKTransmitSessionState.Preparing   -> showPreparingState()
            is MFSKTransmitSessionState.Transmitting -> showTransmittingState(state)
            is MFSKTransmitSessionState.Complete    -> showCompleteState()
            is MFSKTransmitSessionState.Failed      -> showFailedState(state)
            is MFSKTransmitSessionState.Cancelled   -> showCancelledState()
        }
    }

    private fun showIdleState()
    {
        cancelProgress()
        updateStateIcon(R.drawable.ic_nahoft_radio_transmit, R.color.tangerine, AnimationType.NONE)
        updateStatus(getString(R.string.tap_start_to_transmit))
        showFrequencyEditable()
        hideProgressSection()
        hideTerminalSection()
        setStartClickListener()
        binding.btnAction.isEnabled = viewModel.isEdenConnected.value
        binding.btnAction.text = getString(R.string.start_transmission)
        binding.btnClose.visibility = View.GONE
    }

    private fun showPreparingState()
    {
        cancelProgress()
        updateStateIcon(R.drawable.ic_sync, R.color.tangerine, AnimationType.ROTATE)
        updateStatus(getString(R.string.tx_preparing))
        showFrequencyReadOnly()
        hideProgressSection()
        hideTerminalSection()
        setStopClickListener()
        binding.btnAction.isEnabled = true
        binding.btnAction.text = getString(R.string.stop_session)
        binding.btnClose.visibility = View.GONE
    }

    private fun showTransmittingState(state: MFSKTransmitSessionState.Transmitting)
    {
        updateStateIcon(R.drawable.ic_nahoft_radio_transmit, R.color.tangerine, AnimationType.PULSE)
        updateStatus(getString(R.string.tx_transmitting))
        showFrequencyReadOnly()
        showProgressSection()
        hideTerminalSection()
        setStopClickListener()
        binding.btnAction.isEnabled = true
        binding.btnAction.text = getString(R.string.stop_session)
        binding.btnClose.visibility = View.GONE

        // Start progress only on first emission — Transmitting is emitted exactly once
        if (transmitStartMs == 0L)
        {
            transmitStartMs = SystemClock.elapsedRealtime()
            totalDurationMs = state.totalDurationMs
            startProgressUpdates()
        }
    }

    private fun showCompleteState()
    {
        cancelProgress()
        updateStateIcon(R.drawable.ic_success, R.color.caribbeanGreen, AnimationType.NONE)
        updateStatus(getString(R.string.tx_complete_title))
        showProgressSection()

        // Fill bar to 100% regardless of where the timer was
        binding.progressTransmission.progress = PROGRESS_MAX
        val totalSec = (totalDurationMs / 1000).toInt()
        binding.tvTimeElapsed.text = getString(R.string.mfsk_tx_time_elapsed_format, totalSec, totalSec)

        transmissionCompleted = true

        showTerminalSection(
            title      = getString(R.string.tx_complete_title),
            detail     = getString(R.string.mfsk_tx_complete_detail),
            titleColor = R.color.caribbeanGreen
        )

        binding.btnAction.text = getString(R.string.dismiss)
        binding.btnAction.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnClose.visibility = View.VISIBLE
    }

    private fun showFailedState(state: MFSKTransmitSessionState.Failed)
    {
        cancelProgress()
        updateStateIcon(R.drawable.ic_error, R.color.madderLake, AnimationType.NONE)
        updateStatus(getString(R.string.tx_failed_title))
        hideProgressSection()

        showTerminalSection(
            title      = getString(R.string.tx_failed_title),
            detail     = state.reason,
            titleColor = R.color.madderLake
        )

        binding.btnAction.text = getString(R.string.dismiss)
        binding.btnAction.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnClose.visibility = View.VISIBLE
    }

    private fun showCancelledState()
    {
        cancelProgress()
        updateStateIcon(R.drawable.ic_nahoft_radio_transmit, R.color.coolGrey, AnimationType.NONE)
        updateStatus(getString(R.string.tx_cancelled_title))
        hideProgressSection()

        showTerminalSection(
            title      = getString(R.string.tx_cancelled_title),
            detail     = getString(R.string.tx_cancelled_detail),
            titleColor = R.color.coolGrey
        )

        binding.btnAction.text = getString(R.string.dismiss)
        binding.btnAction.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnClose.visibility = View.VISIBLE
    }

    // ==================== Progress ====================

    /**
     * Updates [binding.progressTransmission] and [binding.tvTimeElapsed] every 100ms
     * based on wall-clock elapsed time since [transmitStartMs].
     *
     * Purely cosmetic — the service owns actual transmission timing. If the service
     * completes before the timer reaches 100%, [showCompleteState] fills the bar directly.
     */
    private fun startProgressUpdates()
    {
        progressJob?.cancel()
        progressJob = uiScope.launch {
            while (isActive)
            {
                val elapsed    = SystemClock.elapsedRealtime() - transmitStartMs
                val progress   = if (totalDurationMs > 0)
                    ((elapsed.toDouble() / totalDurationMs) * PROGRESS_MAX)
                        .toInt().coerceIn(0, PROGRESS_MAX)
                else PROGRESS_MAX

                val elapsedSec = (elapsed / 1000).toInt()
                val totalSec   = (totalDurationMs / 1000).toInt()

                if (_binding != null)
                {
                    binding.progressTransmission.progress = progress
                    binding.tvTimeElapsed.text = getString(
                        R.string.mfsk_tx_time_elapsed_format, elapsedSec, totalSec
                    )
                }

                delay(100)
            }
        }
    }

    private fun cancelProgress()
    {
        progressJob?.cancel()
        progressJob    = null
        transmitStartMs = 0L
        totalDurationMs = 0L
    }

    // ==================== Click Listeners ====================

    private fun setStartClickListener()
    {
        binding.btnAction.setOnClickListener {
            viewModel.startTransmission(currentFrequencyInput())
        }
    }

    private fun setStopClickListener()
    {
        binding.btnAction.setOnClickListener {
            Timber.d("MFSKTransmitRadioBottomSheetFragment: user stopped transmission")
            viewModel.cancelTransmission()
        }
    }

    // ==================== Frequency ====================

    private fun showFrequencyEditable()
    {
        binding.etMfskFrequency.isEnabled  = true
        binding.btnMfskFreqMinus.isEnabled = true
        binding.btnMfskFreqPlus.isEnabled  = true
    }

    private fun showFrequencyReadOnly()
    {
        binding.etMfskFrequency.isEnabled  = false
        binding.btnMfskFreqMinus.isEnabled = false
        binding.btnMfskFreqPlus.isEnabled  = false
    }

    private fun currentFrequencyInput(): Int =
        binding.etMfskFrequency.text.toString().toIntOrNull()
            ?: viewModel.getMfskBaseFrequencyHz()

    // ==================== Progress Section ====================

    private fun showProgressSection() { binding.progressSection.visibility = View.VISIBLE }
    private fun hideProgressSection() { binding.progressSection.visibility = View.GONE }

    // ==================== Terminal Section ====================

    private fun showTerminalSection(title: String, detail: String, titleColor: Int)
    {
        binding.terminalStateSection.visibility = View.VISIBLE
        binding.tvTerminalTitle.text = title
        binding.tvTerminalTitle.setTextColor(ContextCompat.getColor(requireContext(), titleColor))
        binding.tvTerminalDetail.text = detail
    }

    private fun hideTerminalSection()
    {
        binding.terminalStateSection.visibility = View.GONE
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
        private const val ARG_MESSAGE           = "arg_mfsk_tx_message"
        private const val ARG_FRIEND_NAME       = "arg_mfsk_tx_friend_name"
        private const val ARG_FRIEND_PUBLIC_KEY = "arg_mfsk_tx_friend_public_key"

        /** Fragment result key — register in the host Activity to refresh UI after TX. */
        const val RESULT_TX_COMPLETE = "mfsk_tx_complete"

        /**
         * Progress bar max value. Higher value = smoother visual animation.
         * Not tied to symbol count — purely a UI scaling factor.
         */
        private const val PROGRESS_MAX = 1000

        fun newInstance(
            message: String,
            friendName: String,
            friendPublicKey: ByteArray
        ): MFSKTransmitRadioBottomSheetFragment =
            MFSKTransmitRadioBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                    putString(ARG_FRIEND_NAME, friendName)
                    putByteArray(ARG_FRIEND_PUBLIC_KEY, friendPublicKey)
                }
            }
    }
}