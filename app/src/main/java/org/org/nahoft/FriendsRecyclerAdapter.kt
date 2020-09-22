package org.org.nahoft

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.friend_recyclerview_item_row.view.*
import org.org.codex.Encryption
import org.org.inflate

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
            println("FriendClicked")

            // TODO: This is just for testing status icon
            friend?.let {
                println("Friend is not Null")
                it.status = FriendStatus.Verified
                it.publicKeyEncoded = Encryption.createTestKeypair().public.encoded
                this.view.friendIcon.setImageResource(it.status.getIcon())
            }
        }

        fun bindFriend(newFriend: Friend) {

            this.friend = newFriend
            this.view.friendName.text = newFriend.name
            this.view.friendIcon.setImageResource(newFriend.status.getIcon())
        }
    }

}
