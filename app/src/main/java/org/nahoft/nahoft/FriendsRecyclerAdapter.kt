package org.nahoft.nahoft

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.friend_recyclerview_item_row.view.*
import org.nahoft.nahoft.activities.FriendsInfoActivity
import org.nahoft.util.inflate
import org.nahoft.nahoft.ui.ItemTouchHelperListener
import org.nahoft.util.RequestCodes

class FriendsRecyclerAdapter(private val friends: ArrayList<Friend>) : RecyclerView.Adapter<FriendsRecyclerAdapter.FriendViewHolder>(), ItemTouchHelperListener {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {

        val inflatedView = parent.inflate(R.layout.friend_recyclerview_item_row, false)
        return FriendViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {

        val itemFriend = friends[position]
        holder.bindFriend(itemFriend)
    }

    override fun getItemCount() = friends.size

    override fun onItemDismiss(viewHolder: RecyclerView.ViewHolder, position: Int) {
        Persist.removeFriendAt(viewHolder.itemView.context, friends[position])
        notifyItemRemoved(position)
    }

    override fun onCancel(position: Int) {
        notifyItemChanged(position)
    }

    fun cleanup() {
        friends.clear()
    }

    class FriendViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {

        private var friend: Friend? = null
        private var view: View = v

        init {
            v.setOnClickListener(this)
        }

        fun bindFriend(newFriend: Friend) {
            this.friend = newFriend
            this.view.friend_name_text_view.text = newFriend.name
            this.view.status_text_view.text = newFriend.status.name
            this.view.friend_icon_view.setImageResource(newFriend.status.getIcon())
        }

        override fun onClick(v: View?) {
            showInfoActivity()
        }


        private fun showInfoActivity()
        {
            if (friend != null)
            {
                val friendInfoIntent = Intent(this.view.context, FriendsInfoActivity::class.java)
                friendInfoIntent.putExtra(RequestCodes.friendExtraTaskDescription, friend)
                this.view.context.startActivity(friendInfoIntent)
            }
        }
    }
}
