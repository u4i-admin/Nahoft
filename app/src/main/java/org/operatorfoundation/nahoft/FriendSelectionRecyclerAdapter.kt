package org.operatorfoundation.nahoft

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recyclerview_item_row.view.*
import org.operatorfoundation.inflate

class FriendSelectionRecyclerAdapter(private val friends: ArrayList<Friend>) : RecyclerView.Adapter<FriendSelectionRecyclerAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val inflatedView = parent.inflate(R.layout.recyclerview_item_row, false)
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
            val context: Context = itemView.context

            //Return to NewMessageActivity
            val returnToMessageIntent = Intent(context, NewMessageActivity::class.java)

            if (friend != null) {
                returnToMessageIntent.putExtra(FRIEND_KEY, friend!!.name)
            }

            context.startActivity(returnToMessageIntent)

            //Update Choose Friend Button with selected friend
        }

        fun bindFriend(newFriend: Friend) {
            this.friend = newFriend
            this.view.friendName.text = newFriend.name
        }

        companion object {
            private val FRIEND_KEY = "Friend"
        }

    }

}
