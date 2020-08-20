package org.operatorfoundation

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recyclerview_item_row.view.*
import org.operatorfoundation.nahoft.R

class RecyclerAdapter(private val friends: ArrayList<String>) : RecyclerView.Adapter<RecyclerAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.FriendViewHolder {
        val inflatedView = parent.inflate(R.layout.recyclerview_item_row, false)
        return FriendViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: RecyclerAdapter.FriendViewHolder, position: Int) {
        val itemFriend = friends[position]
        holder.saveFriend(itemFriend)
    }

    override fun getItemCount() = friends.size

    class FriendViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {

        private var friend: String? = null
        private var view: View = v

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            println("User Selected Friend View")
        }

        fun saveFriend(newFriend: String) {
            this.friend = newFriend
        }
    }

}
