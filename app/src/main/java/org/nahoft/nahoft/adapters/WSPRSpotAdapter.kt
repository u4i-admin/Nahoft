package org.nahoft.nahoft.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nahoft.nahoft.R
import org.nahoft.nahoft.databinding.ItemWsprSpotBinding
import org.nahoft.nahoft.models.NahoftSpotStatus
import org.nahoft.nahoft.models.WSPRSpotItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying WSPR spots in a RecyclerView.
 */
class WSPRSpotAdapter : ListAdapter<WSPRSpotItem, WSPRSpotAdapter.SpotViewHolder>(SpotDiffCallback())
{
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotViewHolder
    {
        val binding = ItemWsprSpotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SpotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SpotViewHolder, position: Int)
    {
        holder.bind(getItem(position))
    }

    inner class SpotViewHolder(
        private val binding: ItemWsprSpotBinding
    ) : RecyclerView.ViewHolder(binding.root)
    {
        fun bind(spot: WSPRSpotItem)
        {
            binding.tvCallsign.text = spot.callsign
            binding.tvTime.text     = timeFormat.format(Date(spot.timestamp))
            binding.tvGrid.text     = spot.gridSquare
            binding.tvPower.text    = "${spot.powerDbm} dBm"
            binding.tvSnr.text      = "%+.0f dB".format(spot.snrDb)

            if (spot.nahoftStatus is NahoftSpotStatus.Decrypted)
            {
                binding.spotCard.setBackgroundResource(R.drawable.spot_card_background_nahoft)
                showNahoftStatus(spot)
            }
            else
            {
                binding.spotCard.setBackgroundResource(R.drawable.spot_card_background)
                binding.rowNahoftStatus.visibility = View.GONE
            }
        }

        private fun showNahoftStatus(spot: WSPRSpotItem)
        {
            val context = binding.root.context
            val green   = ContextCompat.getColor(context, R.color.caribbeanGreen)

            binding.rowNahoftStatus.visibility  = View.VISIBLE
            binding.tvPartNumber.text           = formatPartNumber(spot)
            binding.tvPartNumber.setTextColor(green)
            binding.tvStatus.text               = spot.statusDisplay ?: ""
            binding.tvStatus.setTextColor(green)
        }

        private fun formatPartNumber(spot: WSPRSpotItem): String =
            when (val status = spot.nahoftStatus)
            {
                is NahoftSpotStatus.Decrypted -> "Part ${status.partNumber} of ${status.totalParts}"
                else                          -> ""
            }
    }

    class SpotDiffCallback : DiffUtil.ItemCallback<WSPRSpotItem>()
    {
        override fun areItemsTheSame(oldItem: WSPRSpotItem, newItem: WSPRSpotItem): Boolean
        {
            return oldItem.callsign == newItem.callsign &&
                    oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: WSPRSpotItem, newItem: WSPRSpotItem): Boolean
        {
            return oldItem == newItem
        }
    }
}