package org.nahoft.nahoft.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.nahoft.nahoft.R
import org.nahoft.nahoft.databinding.FragmentBottomSheetRadioModeBinding
import org.nahoft.nahoft.viewmodels.FriendInfoViewModel
import timber.log.Timber

/**
 * Mode selector bottom sheet for radio transmission and reception.
 *
 * Shown when the user taps "Send via radio" or "Receive via radio" in
 * [FriendInfoActivity]. Presents WSPR and MFSK-16 as selectable cards and
 * delivers the chosen mode via [setFragmentResult].
 *
 * TX and RX use separate result keys ([RESULT_KEY_TX] / [RESULT_KEY_RX]) so
 * the host can register independent listeners for each flow.
 *
 * @see RESULT_KEY_TX
 * @see RESULT_KEY_RX
 * @see EXTRA_MODE
 */
class RadioModeBottomSheetFragment : BottomSheetDialogFragment()
{
    // ==================== Types ====================

    /**
     * Whether this sheet is launching a TX or RX flow.
     * Determines the title string and which conflict check to perform.
     */
    enum class Purpose { TX, RX }

    /**
     * The radio mode the user selected.
     * Delivered in the fragment result bundle under [EXTRA_MODE] as the enum name string.
     */
    enum class RadioMode { WSPR, MFSK }

    // ==================== View Binding ====================

    private var _binding: FragmentBottomSheetRadioModeBinding? = null
    private val binding get() = _binding!!

    // ==================== ViewModel ====================

    private val viewModel: FriendInfoViewModel by activityViewModels()

    // ==================== Arguments ====================

    private val purpose: Purpose
        get() = requireArguments()
            .getString(ARG_PURPOSE)
            ?.let { Purpose.valueOf(it) }
            ?: Purpose.TX

    // ==================== Lifecycle ====================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        _binding = FragmentBottomSheetRadioModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isHideable = true
        }

        setupUI()
        setupClickListeners()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }

    // ==================== Setup ====================

    private fun setupUI()
    {
        binding.tvTitle.text = getString(
            when (purpose)
            {
                Purpose.TX -> R.string.radio_mode_title_tx
                Purpose.RX -> R.string.radio_mode_title_rx
            }
        )

        // Error is hidden until a conflict is detected
        binding.tvError.visibility = View.GONE
    }

    private fun setupClickListeners()
    {
        binding.cardWspr.setOnClickListener { onModeSelected(RadioMode.WSPR) }
        binding.cardMfsk.setOnClickListener { onModeSelected(RadioMode.MFSK) }
        binding.btnCancel.setOnClickListener { dismissAllowingStateLoss() }
    }

    // ==================== Mode Selection ====================

    /**
     * Validates mutual exclusion, then delivers the result and dismisses.
     *
     * If a conflicting session is already active, shows [binding.tvError] and
     * returns without dismissing. TX flows skip the conflict check — TX sessions
     * are one-shot and the primary guard runs in [FriendInfoActivity] before
     * this sheet is shown.
     */
    private fun onModeSelected(mode: RadioMode)
    {
        if (hasConflict(mode))
        {
            val conflictingMode = when (mode)
            {
                RadioMode.WSPR -> "MFSK"
                RadioMode.MFSK -> "WSPR"
            }

            Timber.w("RadioModeBottomSheetFragment: $conflictingMode session active — " +
                    "blocking $mode selection for $purpose")

            binding.tvError.text =
                getString(R.string.radio_mode_conflict_error, conflictingMode)
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val resultKey = when (purpose)
        {
            Purpose.TX -> RESULT_KEY_TX
            Purpose.RX -> RESULT_KEY_RX
        }

        setFragmentResult(resultKey, bundleOf(EXTRA_MODE to mode.name))
        dismissAllowingStateLoss()
    }

    /**
     * Returns true if a session of the mode that would conflict with [mode] is
     * currently active for this [purpose].
     *
     * For RX: WSPR and MFSK both require USB audio and cannot run simultaneously.
     * For TX: no service-level conflict check — handled by the activity.
     */
    private fun hasConflict(mode: RadioMode): Boolean = when (purpose)
    {
        Purpose.RX -> when (mode)
        {
            RadioMode.WSPR -> viewModel.isMfskSessionActive()
            RadioMode.MFSK -> viewModel.isWsprSessionActive()
        }
        Purpose.TX -> false
    }

    // ==================== Companion ====================

    companion object
    {
        private const val ARG_PURPOSE = "arg_radio_mode_purpose"

        /**
         * Fragment result key for TX mode selection.
         * Register in the host with:
         * ```
         * setFragmentResultListener(RadioModeBottomSheetFragment.RESULT_KEY_TX, this) { _, bundle ->
         *     val mode = RadioModeBottomSheetFragment.RadioMode.valueOf(
         *         bundle.getString(RadioModeBottomSheetFragment.EXTRA_MODE)!!
         *     )
         *     // launch appropriate TX sheet
         * }
         * ```
         */
        const val RESULT_KEY_TX = "radio_mode_result_tx"

        /**
         * Fragment result key for RX mode selection.
         * Register in the host identically to [RESULT_KEY_TX].
         */
        const val RESULT_KEY_RX = "radio_mode_result_rx"

        /** Bundle key for the selected [RadioMode], serialized as its enum name string. */
        const val EXTRA_MODE = "radio_mode_selection"

        fun newInstance(purpose: Purpose): RadioModeBottomSheetFragment =
            RadioModeBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PURPOSE, purpose.name)
                }
            }
    }
}