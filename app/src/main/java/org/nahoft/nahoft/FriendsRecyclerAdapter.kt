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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder
    {
        val inflatedView = parent.inflate(R.layout.friend_recyclerview_item_row, false)
        return FriendViewHolder(inflatedView)
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

    class FriendViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener
    {

        private var friend: Friend? = null
        private var view: View = v

        init
        {
            v.setOnClickListener(this)
        }

        fun bindFriend(newFriend: Friend)
        {
            this.friend = newFriend
            this.view.friend_name_text_view.text = newFriend.name
            this.view.status_text_view.text = newFriend.getStatusString(this.view.context)
            this.view.friend_icon_view.setImageResource(newFriend.status.getIcon())
        }

        override fun onClick(v: View?)
        {
            showInfoActivity()
        }


        private fun showInfoActivity()
        {
            if (friend != null)
            {
                val friendInfoIntent = Intent(this.view.context, FriendInfoActivity::class.java)
                friendInfoIntent.putExtra(RequestCodes.friendExtraTaskDescription, friend)
                this.view.context.startActivity(friendInfoIntent)
            }
        }
    }

}
