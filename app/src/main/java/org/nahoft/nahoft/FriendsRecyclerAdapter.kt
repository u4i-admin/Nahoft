package org.nahoft.nahoft

import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.nahoft.nahoft.databinding.FriendRecyclerviewItemRowBinding
import org.nahoft.util.inflate

class FriendsRecyclerAdapter(private val friends: ArrayList<Friend>) : RecyclerView.Adapter<FriendsRecyclerAdapter.FriendViewHolder>()
{
    var onItemClick: ((Friend) -> Unit)? = null
    var onItemLongClick: ((Friend) -> Unit)? = null

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
//
//        holder.itemView.setOnClickListener {
//            onItemClick?.invoke(itemFriend)
//        }
//        holder.itemView.setOnLongClickListener {
//            onItemLongClick?.invoke(itemFriend)
//            return@setOnLongClickListener true
//        }
    }

    override fun getItemCount() = friends.size

    fun cleanup()
    {
        friends.clear()
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
        }
    }
}
