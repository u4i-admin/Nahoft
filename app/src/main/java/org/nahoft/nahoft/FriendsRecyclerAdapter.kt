package org.nahoft.nahoft

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.friend_recyclerview_item_row.view.*
import org.nahoft.nahoft.activities.FriendInfoActivity
import org.nahoft.util.RequestCodes
import org.nahoft.util.inflate

class FriendsRecyclerAdapter(private val friends: ArrayList<Friend>) : RecyclerView.Adapter<FriendsRecyclerAdapter.FriendViewHolder>()
{
    var onItemClick: ((Friend) -> Unit)? = null
    var onItemLongClick: ((Friend) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder
    {
        val inflatedView = parent.inflate(R.layout.friend_recyclerview_item_row, false)
        return FriendViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int)
    {
        val itemFriend = friends[position]
        holder.bindFriend(itemFriend)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(itemFriend)
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(itemFriend)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount() = friends.size

    fun cleanup()
    {
        friends.clear()
    }

    inner class FriendViewHolder(v: View) : RecyclerView.ViewHolder(v)
    {
        private var friend: Friend? = null
        private var view: View = v

        init
        {
            v.setOnClickListener {
                friend?.let { friend ->
                    onItemClick?.invoke(friend)
                }
            }
            v.setOnLongClickListener {
                friend?.let { friend ->
                    onItemLongClick?.invoke(friend)
                }
                return@setOnLongClickListener true
            }
        }

        fun bindFriend(newFriend: Friend)
        {
            this.friend = newFriend
            this.view.friend_name_text_view.text = newFriend.name
            this.view.status_text_view.text = newFriend.getStatusString(this.view.context)
            this.view.friend_icon_view.setImageResource(newFriend.status.getIcon())
            this.view.friend_picture.text = newFriend.name.substring(0, 1)
        }
    }
}
