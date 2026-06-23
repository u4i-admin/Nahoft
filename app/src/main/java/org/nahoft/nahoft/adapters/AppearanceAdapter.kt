package org.nahoft.nahoft.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.recyclerview.widget.RecyclerView
import org.nahoft.nahoft.R
import org.nahoft.nahoft.databinding.ItemAppearanceIdentityBinding
import org.nahoft.util.AppIdentity

class AppearanceAdapter(
    private val identities: Array<AppIdentity>,
    private var selectedIdentity: AppIdentity,
    private val onSelectionChanged: (AppIdentity) -> Unit
) : RecyclerView.Adapter<AppearanceAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAppearanceIdentityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(identity: AppIdentity) {
            val context = binding.root.context
            val sizePx = context.resources.getDimensionPixelSize(R.dimen.app_icon_preview_size)

            val bitmap = BitmapFactory.decodeResource(context.resources, identity.dialogIconRes)
            if (bitmap != null) {
                val scaled = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
                val circular = RoundedBitmapDrawableFactory.create(context.resources, scaled).apply {
                    isCircular = true
                }
                binding.ivIdentityIcon.setImageDrawable(circular)
            }

            binding.tvIdentityName.text = context.getString(identity.labelRes)
            binding.root.isSelected = identity == selectedIdentity

            binding.root.setOnClickListener {
                val previous = selectedIdentity
                selectedIdentity = identity
                notifyItemChanged(identities.indexOf(previous))
                notifyItemChanged(identities.indexOf(identity))
                onSelectionChanged(identity)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppearanceIdentityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(identities[position])
    }

    override fun getItemCount() = identities.size
}