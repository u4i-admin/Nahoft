package org.nahoft.nahoft.fragments

import android.animation.ObjectAnimator
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.animation.ValueAnimator
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
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
import org.nahoft.nahoft.databinding.FragmentBottomSheetTransmitRadioBinding
import org.nahoft.nahoft.services.TransmitSessionState
import org.nahoft.nahoft.viewmodels.FriendInfoViewModel
import org.nahoft.nahoft.viewmodels.TransmitRadioViewModel
import org.operatorfoundation.audiocoder.wspr.WSPRConstants
import timber.log.Timber

/**
 * Bottom sheet for transmitting encrypted messages via WSPR radio.
 *
 * Receives the message and friend public key as arguments — the full TX
 * pipeline runs inside [TransmitSessionService] via [TransmitRadioViewModel].
 * This fragment is purely a UI observer.
 *
 * Not cancelable by outside tap. The user must explicitly press Stop or Dismiss.
 */
class TransmitRadioBottomSheetFragment : BottomSheetDialogFragment()
{
    // ==================== View Binding ====================

    private var _binding: FragmentBottomSheetTransmitRadioBinding? = null
    private val binding get() = _binding!!

    // ==================== ViewModels ====================

    /**
     * Fragment-scoped ViewModel created with the Factory.
     * Arguments are required — fragment will throw if missing.
     */
    private val viewModel: TransmitRadioViewModel by viewModels {
        TransmitRadioViewModel.Factory(
            message = requireArguments().getString(ARG_MESSAGE)!!,
            friendName = requireArguments().getString(ARG_FRIEND_NAME)!!,
            friendPublicKey = requireArguments().getByteArray(ARG_FRIEND_PUBLIC_KEY)!!
        )
    }

    /** Activity-scoped ViewModel — used only for friend name display. */
    private val friendInfoViewModel: FriendInfoViewModel by activityViewModels()

    // ==================== UI State ====================

    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentAnimator: ObjectAnimator? = null
    private var countdownJob: Job? = null
    private var transmissionCompleted = false

    /**
     * Tracks how many spot dots have been built into [binding.llSpotDots].
     * Dots are created once when totalSpots is first known, then only recolored.
     */
    private var builtDotCount = 0

    // ==================== Animation Types ====================

    private enum class AnimationType { NONE, PULSE, ROTATE }


    // ==================== Lifecycle ====================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        _binding = FragmentBottomSheetTransmitRadioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = false

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isHideable = false // Prevents swipe-to-dismiss
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

    // ==================== Setup ====================

    /**
     * Populates views that do not change with session state.
     */
    private fun setupStaticUI()
    {
        // Message preview — truncated by layout to 2 lines
        binding.tvMessagePreview.text = requireArguments().getString(ARG_MESSAGE)

        // Pre-fill frequency from saved preference
        binding.etTxFrequency.setText(viewModel.getTxFrequencyKHz().toString())

        binding.progressSymbols.max = WSPRConstants.SYMBOLS_PER_MESSAGE
    }

    private fun setupClickListeners()
    {
        setupEncryptionToggle()

        // Frequency stepper
        binding.btnTxFreqMinus.setOnClickListener {
            val current = currentFrequencyInput()
            binding.etTxFrequency.setText((current - 1).toString())
        }

        binding.btnTxFreqPlus.setOnClickListener {
            val current = currentFrequencyInput()
            binding.etTxFrequency.setText((current + 1).toString())
        }

        // Close button — only visible in terminal states
        binding.btnClose.setOnClickListener {
            dismissAllowingStateLoss()
        }

        // Primary action button — label and behavior set by updateActionButton()
        // Initial click listener set here for the Idle (Start) state
        setStartClickListener()
    }

    // ==================== Encryption Mode ====================

    // Returns whether the current session should use encryption.
    private fun currentEncryptionMode(): Boolean = !binding.cbDisableEncryption.isChecked

    /**
     * Wires the encryption checkbox to show/hide the warning icon.
     */
    private fun setupEncryptionToggle()
    {
        binding.cbDisableEncryption.setOnCheckedChangeListener { _, isChecked ->
            // Warning icon visible only when encryption is disabled
            binding.ivEncryptionWarning.visibility =
                if (isChecked) View.VISIBLE else View.GONE
        }
    }

    // ==================== ViewModel Observation ====================

    private fun observeViewModel()
    {
        uiScope.launch {
            viewModel.transmitSessionState.collect { state ->
                updateUI(state)
            }
        }

        // Eden availability controls Start button in Idle state
        uiScope.launch {
            viewModel.isEdenConnected.collect { connected ->
                if (viewModel.transmitSessionState.value == TransmitSessionState.Idle)
                {
                    binding.btnAction.isEnabled = connected
                    if (!connected)
                    {
                        updateStatus(getString(R.string.alert_text_serial_not_connected))
                    }
                }
            }
        }
    }

    // ==================== State-Driven UI ====================

    /**
     * Central dispatcher — routes each state to its specific UI update.
     */
    private fun updateUI(state: TransmitSessionState)
    {
        if (_binding == null) return

        when (state)
        {
            is TransmitSessionState.Idle -> showIdleState()

            is TransmitSessionState.Preparing -> showPreparingState()

            is TransmitSessionState.WaitingForWindow -> showWaitingForWindowState(state)

            is TransmitSessionState.TransmittingSpot -> showTransmittingState(state)

            is TransmitSessionState.SwitchingToRx -> showSwitchingToRxState(state)

            is TransmitSessionState.Complete -> showCompleteState(state)

            is TransmitSessionState.Failed -> showFailedState(state)

            is TransmitSessionState.Cancelled -> showCancelledState()
        }
    }

    // ── Individual state handlers ─────────────────────────────────────────

    private fun showIdleState()
    {
        cancelCountdown()
        updateStateIcon(R.drawable.ic_nahoft_radio_transmit, R.color.tangerine, AnimationType.NONE)
        updateStatus(getString(R.string.tap_start_to_transmit))
        showFrequencyEditable()
        hideProgressSection()
        hideTerminalSection()
        setStartClickListener()
        binding.btnAction.isEnabled = viewModel.isEdenConnected.value
        binding.btnAction.text = getString(R.string.start_transmission)
        binding.btnClose.visibility = View.GONE
        binding.cbDisableEncryption.isEnabled = true
    }

    private fun showPreparingState()
    {
        cancelCountdown()
        updateStateIcon(R.drawable.ic_sync, R.color.tangerine, AnimationType.ROTATE)
        updateStatus(getString(R.string.tx_preparing))
        showFrequencyReadOnly()
        hideProgressSection()
        hideTerminalSection()
        setStopClickListener()
        binding.btnAction.isEnabled = true
        binding.btnAction.text = getString(R.string.stop_session)
        binding.btnClose.visibility = View.GONE
        binding.cbDisableEncryption.isEnabled = false
    }

    private fun showWaitingForWindowState(state: TransmitSessionState.WaitingForWindow)
    {
        updateStateIcon(R.drawable.ic_access_time, R.color.tangerine, AnimationType.PULSE)
        showFrequencyReadOnly()
        showProgressSection()
        hideTerminalSection()
        setStopClickListener()
        binding.btnAction.isEnabled = true
        binding.btnAction.text = getString(R.string.stop_session)
        binding.btnClose.visibility = View.GONE

        ensureSpotDots(state.totalSpots)
        updateSpotDots(state.spotIndex, completed = false)
        updateSpotProgressLabel(state.spotIndex, state.totalSpots)
        updateStatus(getString(R.string.waiting_for_next_window))

        // Symbol bar reset — we are between spots
        binding.progressSymbols.progress = 0
        binding.tvSymbolProgress.text = "0 / ${WSPRConstants.SYMBOLS_PER_MESSAGE}"

        // Live countdown — restarts each time this state is emitted
        startCountdown(state.msRemaining)
    }

    private fun showTransmittingState(state: TransmitSessionState.TransmittingSpot)
    {
        cancelCountdown()
        updateStateIcon(R.drawable.ic_nahoft_radio_transmit, R.color.tangerine, AnimationType.PULSE)
        updateStatus(getString(R.string.tx_transmitting))
        showFrequencyReadOnly()
        showProgressSection()
        hideTerminalSection()
        setStopClickListener()
        binding.btnAction.isEnabled = true
        binding.btnAction.text = getString(R.string.stop_session)
        binding.btnClose.visibility = View.GONE

        ensureSpotDots(state.totalSpots)
        updateSpotDots(state.spotIndex, completed = false)
        updateSpotProgressLabel(state.spotIndex, state.totalSpots)

        // Symbol progress
        val symbolsTotal = WSPRConstants.SYMBOLS_PER_MESSAGE
        binding.progressSymbols.progress = state.symbolIndex + 1
        binding.tvSymbolProgress.text = "${state.symbolIndex + 1} / $symbolsTotal"

        // Hide countdown — we are actively transmitting
        binding.countdownRow.visibility = View.GONE
    }

    private fun showSwitchingToRxState(state: TransmitSessionState.SwitchingToRx)
    {
        cancelCountdown()
        updateStateIcon(R.drawable.ic_sync, R.color.coolGrey, AnimationType.NONE)
        updateStatus(getString(R.string.tx_switching_rx))
        showProgressSection()
        hideTerminalSection()
        setStopClickListener()

        // Mark this spot as completed in dots
        ensureSpotDots(state.totalSpots)
        updateSpotDots(state.spotIndex, completed = true)
        updateSpotProgressLabel(state.spotIndex, state.totalSpots)

        // Symbol bar filled — spot finished
        binding.progressSymbols.progress = WSPRConstants.SYMBOLS_PER_MESSAGE
        binding.tvSymbolProgress.text = "${WSPRConstants.SYMBOLS_PER_MESSAGE} / ${WSPRConstants.SYMBOLS_PER_MESSAGE}"
        binding.countdownRow.visibility = View.GONE
    }

    private fun showCompleteState(state: TransmitSessionState.Complete)
    {
        cancelCountdown()
        updateStateIcon(R.drawable.ic_success, R.color.caribbeanGreen, AnimationType.NONE)
        updateStatus(getString(R.string.tx_complete_title))
        showProgressSection()

        transmissionCompleted = true

        // All dots completed
        ensureSpotDots(state.totalSpots)
        updateAllDotsCompleted(state.totalSpots)
        updateSpotProgressLabel(state.totalSpots - 1, state.totalSpots)
        binding.progressSymbols.progress = WSPRConstants.SYMBOLS_PER_MESSAGE
        binding.tvSymbolProgress.text = "${WSPRConstants.SYMBOLS_PER_MESSAGE} / ${WSPRConstants.SYMBOLS_PER_MESSAGE}"
        binding.countdownRow.visibility = View.GONE

        // Terminal card
        showTerminalSection(
            title = getString(R.string.tx_complete_title),
            detail = getString(R.string.tx_complete_detail, state.totalSpots),
            titleColor = R.color.caribbeanGreen
        )

        // Dismiss becomes the only action
        binding.btnAction.text = getString(R.string.dismiss)
        binding.btnAction.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnClose.visibility = View.VISIBLE
    }

    private fun showFailedState(state: TransmitSessionState.Failed)
    {
        cancelCountdown()
        updateStateIcon(R.drawable.ic_error, R.color.madderLake, AnimationType.NONE)
        updateStatus(getString(R.string.tx_failed_title))
        hideProgressSection()

        showTerminalSection(
            title = getString(R.string.tx_failed_title),
            detail = state.reason,
            titleColor = R.color.madderLake
        )

        binding.btnAction.text = getString(R.string.dismiss)
        binding.btnAction.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnClose.visibility = View.VISIBLE
    }

    private fun showCancelledState()
    {
        cancelCountdown()
        updateStateIcon(R.drawable.ic_nahoft_radio_transmit, R.color.coolGrey, AnimationType.NONE)
        updateStatus(getString(R.string.tx_cancelled_title))
        hideProgressSection()

        showTerminalSection(
            title = getString(R.string.tx_cancelled_title),
            detail = getString(R.string.tx_cancelled_detail),
            titleColor = R.color.coolGrey
        )

        binding.btnAction.text = getString(R.string.dismiss)
        binding.btnAction.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnClose.visibility = View.VISIBLE
    }

    // ==================== Click Listeners ====================

    private fun setStartClickListener()
    {
        binding.btnAction.setOnClickListener {
            val freqKHz = currentFrequencyInput()
            viewModel.startTransmission(freqKHz, currentEncryptionMode())
        }
    }

    private fun setStopClickListener()
    {
        binding.btnAction.setOnClickListener {
            Timber.d("TransmitRadioBottomSheetFragment: user stopped transmission")
            viewModel.cancelTransmission()
        }
    }

    // ==================== Frequency Section ====================

    private fun showFrequencyEditable()
    {
        binding.etTxFrequency.isEnabled = true
        binding.btnTxFreqMinus.isEnabled = true
        binding.btnTxFreqPlus.isEnabled = true
    }

    private fun showFrequencyReadOnly()
    {
        binding.etTxFrequency.isEnabled = false
        binding.btnTxFreqMinus.isEnabled = false
        binding.btnTxFreqPlus.isEnabled = false
    }

    private fun currentFrequencyInput(): Int =
        binding.etTxFrequency.text.toString().toIntOrNull()
            ?: viewModel.getTxFrequencyKHz()

    // ==================== Progress Section ====================

    private fun showProgressSection()
    {
        binding.progressSection.visibility = View.VISIBLE
    }

    private fun hideProgressSection()
    {
        binding.progressSection.visibility = View.GONE
    }

    private fun updateSpotProgressLabel(spotIndex: Int, totalSpots: Int)
    {
        // Display is 1-based for readability
        binding.tvSpotProgress.text = "${spotIndex + 1} / $totalSpots"
    }

    // ==================== Spot Dot Indicators ====================

    /**
     * Builds the dot row if it hasn't been built yet for this [totalSpots] count.
     * Each dot is a small filled circle — 10dp diameter, 6dp spacing.
     * Called before any dot color update to ensure dots exist.
     */
    private fun ensureSpotDots(totalSpots: Int)
    {
        if (builtDotCount == totalSpots) return

        binding.llSpotDots.removeAllViews()
        builtDotCount = 0

        val ctx = requireContext()
        val sizePx = (10 * resources.displayMetrics.density).toInt()
        val marginPx = (6 * resources.displayMetrics.density).toInt()

        repeat(totalSpots) {
            val dot = View(ctx).apply {
                layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply {
                    marginEnd = marginPx
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(ctx, R.color.coolGrey))
                }
            }
            binding.llSpotDots.addView(dot)
            builtDotCount++
        }
    }

    /**
     * Colors dots based on current transmission position.
     *
     * - Dots before [currentSpotIndex]: white (completed)
     * - Dot at [currentSpotIndex]: tangerine if [completed] is false, white if true
     * - Dots after [currentSpotIndex]: coolGrey (pending)
     */
    private fun updateSpotDots(currentSpotIndex: Int, completed: Boolean)
    {
        val ctx = requireContext()

        for (i in 0 until binding.llSpotDots.childCount)
        {
            val dot = binding.llSpotDots.getChildAt(i)
            val color = when
            {
                i < currentSpotIndex -> R.color.white
                i == currentSpotIndex -> if (completed) R.color.white else R.color.tangerine
                else -> R.color.coolGrey
            }
            (dot.background as? GradientDrawable)?.setColor(
                ContextCompat.getColor(ctx, color)
            )
        }
    }

    /**
     * Colors all dots white — used when transmission completes successfully.
     */
    private fun updateAllDotsCompleted(totalSpots: Int)
    {
        val ctx = requireContext()
        for (i in 0 until binding.llSpotDots.childCount)
        {
            (binding.llSpotDots.getChildAt(i).background as? GradientDrawable)?.setColor(
                ContextCompat.getColor(ctx, R.color.caribbeanGreen)
            )
        }
    }

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

    // ==================== Status Text ====================

    private fun updateStatus(message: String)
    {
        if (_binding != null && isAdded)
        {
            binding.tvStatus.text = message
        }
    }

    // ==================== State Icon ====================

    private fun updateStateIcon(iconRes: Int, colorRes: Int, animationType: AnimationType)
    {
        if (_binding == null) return

        currentAnimator?.cancel()
        currentAnimator = null

        binding.ivStateIcon.rotation = 0f
        binding.ivStateIcon.alpha = 1f
        binding.ivStateIcon.setImageResource(iconRes)
        binding.ivStateIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), colorRes),
            PorterDuff.Mode.SRC_IN
        )

        when (animationType)
        {
            AnimationType.PULSE -> startPulseAnimation()
            AnimationType.ROTATE -> startRotateAnimation()
            AnimationType.NONE -> {}
        }
    }

    private fun startPulseAnimation()
    {
        currentAnimator = ObjectAnimator.ofFloat(
            binding.ivStateIcon, "alpha", 1f, 0.4f, 1f
        ).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.RESTART
            start()
        }
    }

    private fun startRotateAnimation()
    {
        currentAnimator = ObjectAnimator.ofFloat(
            binding.ivStateIcon, "rotation", 0f, 360f
        ).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    // ==================== Countdown ====================

    /**
     * Starts a coroutine that counts down from [initialMs] and updates
     * [binding.tvCountdown] every second. Replaces any existing countdown.
     *
     * The countdown is purely cosmetic — actual window detection is in the service.
     */
    private fun startCountdown(initialMs: Long)
    {
        cancelCountdown()
        binding.countdownRow.visibility = View.VISIBLE

        countdownJob = uiScope.launch {
            var remaining = initialMs
            while (isActive && remaining > 0)
            {
                val seconds = (remaining / 1000).toInt()
                if (_binding != null)
                {
                    binding.tvCountdown.text = getString(R.string.next_tx_window_in, seconds)
                }
                delay(1000)
                remaining -= 1000
            }
        }
    }

    private fun cancelCountdown()
    {
        countdownJob?.cancel()
        countdownJob = null
        if (_binding != null)
        {
            binding.countdownRow.visibility = View.GONE
        }
    }

    // ==================== Cleanup ====================

    override fun onDestroyView()
    {
        super.onDestroyView()

        currentAnimator?.cancel()
        countdownJob?.cancel()
        uiScope.cancel()

        _binding = null
    }

    // ==================== Companion ====================

    companion object
    {
        private const val ARG_MESSAGE          = "arg_tx_message"
        private const val ARG_FRIEND_NAME      = "arg_tx_friend_name"
        private const val ARG_FRIEND_PUBLIC_KEY = "arg_tx_friend_public_key"

        const val RESULT_TX_COMPLETE = "tx_complete"

        /**
         * Creates a new instance with the message and friend context required
         * to start a TX session.
         *
         * @param message         Plaintext message to encrypt and transmit
         * @param friendName      Friend's display name (for save and notification)
         * @param friendPublicKey Friend's encoded public key bytes
         */
        fun newInstance(
            message: String,
            friendName: String,
            friendPublicKey: ByteArray
        ): TransmitRadioBottomSheetFragment
        {
            return TransmitRadioBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                    putString(ARG_FRIEND_NAME, friendName)
                    putByteArray(ARG_FRIEND_PUBLIC_KEY, friendPublicKey)
                }
            }
        }
    }
}