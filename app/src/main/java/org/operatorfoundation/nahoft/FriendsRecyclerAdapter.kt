package org.operatorfoundation.nahoft

import android.graphics.drawable.Drawable
import android.media.Image
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.friend_recyclerview_item_row.view.*
import kotlinx.android.synthetic.main.friend_selection_recyclerview_item_row.view.*
import kotlinx.android.synthetic.main.friend_selection_recyclerview_item_row.view.friendName
import org.operatorfoundation.inflate

class FriendsRecyclerAdapter(private val friends: ArrayList<Friend>) : RecyclerView.Adapter<FriendsRecyclerAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val inflatedView = parent.inflate(R.layout.friend_recyclerview_item_row, false)
        return FriendViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val itemFriend = friends[position]
        holder.bindFriend(itemFriend)
    }

    override fun getItemCount() = friends.size

    class FriendViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {

        private var friend: Friend? = null
        private var view: View = v

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            println("User Selected Friend View")
        }

        fun bindFriend(newFriend: Friend) {
            this.friend = newFriend
            this.view.friendName.text = newFriend.name
            TODO("Set Friend Icon to Match Status")
            var friendImage = newFriend.status.getIcon()
            this.view.friendIcon.setImageDrawable()
        }

        companion object {
            private val FRIEND_KEY = "Friend"
        }

    }

}
