package org.nahoft.nahoft

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.friend_selection_recyclerview_item_row.view.*
import org.nahoft.util.inflate

class SelectKeySenderRecyclerAdapter(
    private val friends: ArrayList<Friend>,
    private val listener: (Friend) -> Unit) : RecyclerView.Adapter<SelectKeySenderRecyclerAdapter.FriendViewHolder>() {

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

    /*fun cleanup(){
        //friends.clear()
    }*/

    // View Holder
    class FriendViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {

        private var friend: Friend? = null
        private var view: View = v

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {

        }

        fun bindFriend(newFriend: Friend) {
            this.friend = newFriend
            this.view.friendSelectionName.text = newFriend.name
        }
    }
}