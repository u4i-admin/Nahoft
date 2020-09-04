package org.operatorfoundation.nahoft

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.friend_selection_recyclerview_item_row.view.*
import org.operatorfoundation.inflate

class FriendSelectionRecyclerAdapter(private val friends: ArrayList<Friend>,
                                     private val listener: (Friend) -> Unit
) : RecyclerView.Adapter<FriendSelectionRecyclerAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val inflatedView = parent.inflate(R.layout.friend_selection_recyclerview_item_row, false)
        return FriendViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friendItem = friends[position]
        holder.bindFriend(friendItem)
        holder.itemView.setOnClickListener { listener(friendItem) }
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
//            val context: Context = itemView.context
//
//            //Return to NewMessageActivity
//            val returnToMessageIntent = Intent(context, NewMessageActivity::class.java)
//
//
//
//            context.startActivity(returnToMessageIntent)
//
//            //Update Choose Friend Button with selected friend
        }

        fun bindFriend(newFriend: Friend) {
            this.friend = newFriend
            this.view.friendName.text = newFriend.name
        }

        companion object {
            val FRIEND_KEY = "Friend"
        }

    }

}
