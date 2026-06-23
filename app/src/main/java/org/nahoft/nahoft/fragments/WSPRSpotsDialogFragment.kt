package org.nahoft.nahoft.fragments

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.nahoft.nahoft.R
import org.nahoft.nahoft.adapters.WSPRSpotAdapter
import org.nahoft.nahoft.databinding.DialogWsprSpotsBinding
import org.nahoft.nahoft.models.GroupPosition
import org.nahoft.nahoft.models.NahoftSpotStatus
import org.nahoft.nahoft.models.WSPRSpotItem

/**
 * Dialog fragment displaying all WSPR spots received during a session.
 *
 * Usage:
 * ```
 * val dialog = WSPRSpotsDialogFragment.newInstance()
 * dialog.show(supportFragmentManager, "WSPRSpotsDialog")
 *
 * // Update spots as they come in:
 * dialog.updateSpots(spotsList)
 * ```
 */
class WSPRSpotsDialogFragment : DialogFragment()
{
    private var _binding: DialogWsprSpotsBinding? = null
    private val binding get() = _binding!!

    private val adapter = WSPRSpotAdapter()

    // Current list of spots (stored to allow updates while dialog is open)
    private var currentSpots: List<WSPRSpotItem> = emptyList()

    companion object
    {
        /**
         * Creates a new instance of the dialog.
         */
        fun newInstance(): WSPRSpotsDialogFragment {
            return WSPRSpotsDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        _binding = DialogWsprSpotsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        updateUI()
    }

    override fun onStart()
    {
        super.onStart()

        // Set dialog width to 90% of screen width
        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    private fun setupRecyclerView()
    {
        binding.recyclerSpots.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WSPRSpotsDialogFragment.adapter
        }
    }

    private fun setupClickListeners()
    {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Updates the displayed spots list.
     *
     * Call this method to refresh the dialog with new spots.
     * Handles computing group positions for visual connectors.
     *
     * @param spots List of all WSPR spots to display (newest first recommended)
     */
    fun updateSpots(spots: List<WSPRSpotItem>)
    {
        currentSpots = spots

        if (isAdded && _binding != null) {
            updateUI()
        }
    }

    /**
     * Updates all UI elements based on current spots list.
     */
    private fun updateUI()
    {
        // Update spot count
        binding.tvSpotCount.text = resources.getQuantityString(
            R.plurals.packet_count_format,
            currentSpots.size,
            currentSpots.size
        )

        // Show/hide empty state
        val hasSpots = currentSpots.isNotEmpty()
        binding.emptyStateContainer.visibility = if (hasSpots) View.GONE else View.VISIBLE
        binding.recyclerSpots.visibility = if (hasSpots) View.VISIBLE else View.GONE

        // Compute group positions and submit to adapter
        val spotsWithPositions = computeGroupPositions(currentSpots)
        adapter.submitList(spotsWithPositions)
    }

    /**
     * Computes the visual group position for each Nahoft spot.
     *
     * Groups spots by their groupId and assigns FIRST/MIDDLE/LAST/SINGLE
     * positions for drawing connector lines.
     *
     * @param spots Raw list of spots
     * @return List with groupPosition computed for each spot
     */
    private fun computeGroupPositions(spots: List<WSPRSpotItem>): List<WSPRSpotItem>
    {
        if (spots.isEmpty()) return spots

        // Group Nahoft spots by groupId
        val groupedSpots = mutableMapOf<Int, MutableList<Int>>() // groupId -> list of indices

        spots.forEachIndexed { index, spot ->
            val groupId = when (val status = spot.nahoftStatus)
            {
                is NahoftSpotStatus.Decrypted -> status.groupId
                else                          -> null
            }

            groupId?.let {
                groupedSpots.getOrPut(it) { mutableListOf() }.add(index)
            }
        }

        // Create new list with computed positions
        return spots.mapIndexed { index, spot ->
            val groupId = when (val status = spot.nahoftStatus)
            {
                is NahoftSpotStatus.Decrypted -> status.groupId
                else                          -> null
            }

            if (groupId == null)
            {
                // Not a Nahoft spot, no group position
                spot.copy(groupPosition = GroupPosition.NONE)
            }
            else
            {
                val groupIndices = groupedSpots[groupId] ?: listOf(index)
                val positionInGroup = groupIndices.indexOf(index)
                val groupSize = groupIndices.size

                val position = when {
                    groupSize == 1 -> GroupPosition.SINGLE
                    positionInGroup == 0 -> GroupPosition.FIRST
                    positionInGroup == groupSize - 1 -> GroupPosition.LAST
                    else -> GroupPosition.MIDDLE
                }

                spot.copy(groupPosition = position)
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }
}