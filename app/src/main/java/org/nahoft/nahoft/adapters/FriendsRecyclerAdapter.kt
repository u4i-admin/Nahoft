package org.nahoft.nahoft.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.nahoft.nahoft.R
import org.nahoft.nahoft.databinding.FriendRecyclerviewItemRowBinding
import org.nahoft.nahoft.models.Friend

class FriendsRecyclerAdapter(private val friends: ArrayList<Friend>) : RecyclerView.Adapter<FriendsRecyclerAdapter.FriendViewHolder>()
{
    var onItemClick: ((Friend) -> Unit)? = null
    var onItemLongClick: ((Friend) -> Unit)? = null
    var receivingFriendName: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder
    {
        val binding = FriendRecyclerviewItemRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int)
    {
        val itemFriend = friends[position]
        holder.bindFriend(itemFriend)
    }

    override fun getItemCount() = friends.size

    fun cleanup()
    {
        friends.clear()
    }

    fun setReceivingFriend(name: String?)
    {
        val oldName = receivingFriendName
        receivingFriendName = name

        // Refresh affected items
        oldName?.let { notifyFriendChanged(it) }
        name?.let { notifyFriendChanged(it) }
    }

    private fun notifyFriendChanged(name: String)
    {
        val index = friends.indexOfFirst { it.name == name }
        if (index >= 0) notifyItemChanged(index)
    }

    inner class FriendViewHolder(private val binding: FriendRecyclerviewItemRowBinding) : RecyclerView.ViewHolder(binding.root)
    {
        private var friend: Friend? = null

        init
        {
            // Set click listeners once in init, using binding.root
            binding.root.setOnClickListener {
                friend?.let { friend ->
                    onItemClick?.invoke(friend)
                }
            }

            binding.root.setOnLongClickListener {
                friend?.let { friend ->
                    onItemLongClick?.invoke(friend)
                }
                return@setOnLongClickListener true
            }
        }

        fun bindFriend(newFriend: Friend)
        {
            this.friend = newFriend
            binding.friendNameTextView.text = newFriend.name
            binding.statusTextView.text = newFriend.getStatusString(binding.root.context)
            binding.friendIconView.setImageResource(newFriend.status.getIcon())
            binding.friendPicture.text = newFriend.name.substring(0, 1)

            // Receiving indicator
            val isReceiving = newFriend.name == receivingFriendName
            binding.receivingIndicator.visibility = if (isReceiving) View.VISIBLE else View.GONE

            if (isReceiving)
            {
                binding.receivingIndicator.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        binding.root.context,
                        R.anim.pulse
                    )
                )
            }
            else
            {
                binding.receivingIndicator.clearAnimation()
            }
        }
    }
}