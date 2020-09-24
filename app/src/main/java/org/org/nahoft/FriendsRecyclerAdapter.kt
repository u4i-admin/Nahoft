package org.org.nahoft

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import kotlinx.android.synthetic.main.friend_recyclerview_item_row.view.*
import org.org.codex.Encryption
import org.org.inflate
import org.org.nahoft.Persist.Companion.encryptedSharedPreferences

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

            v.invite_button.setOnClickListener {
                inviteClicked()
            }

            v.accept_button.setOnClickListener {
                acceptClicked()
            }

            v.decline_button.setOnClickListener {
                declineClicked()
            }
        }

        override fun onClick(v: View) {
            // TODO: This is just for testing status icon
            friend?.let {
                println("Friend is not Null")
                it.status = FriendStatus.Approved
                it.publicKeyEncoded = Encryption(this.view.context).ensureKeysExist().public.encoded
                this.view.friendIcon.setImageResource(it.status.getIcon())
            }
        }

        fun bindFriend(newFriend: Friend) {
            this.friend = newFriend
            this.view.friendName.text = newFriend.name
            setupRowByStatus(newFriend)
        }

        fun inviteClicked() {
            // Get user's public key to send to contact
            encryptedSharedPreferences.getString()

            // This is a key exchange, so we will encode it, but not encrypt it

            // Share the key

            // Set friend status to Invited
            this.friend?.status = FriendStatus.Invited
        }

        fun acceptClicked() {
            // Set friend status to Accepted
            this.friend?.status = FriendStatus.Approved
        }

        fun declineClicked() {
            // Set Friend Status to Default
            this.friend?.status = FriendStatus.Default
        }

        fun setupRowByStatus(thisFriend: Friend) {
            this.view.friendIcon.setImageResource(thisFriend.status.getIcon())

            when (thisFriend.status) {
                FriendStatus.Default -> {
                    this.view.invite_button.visibility = View.VISIBLE
                    this.view.status_label_text_view.visibility = View.GONE
                    this.view.accept_button.visibility = View.GONE
                    this.view.decline_button.visibility = View.GONE
                }

                FriendStatus.Invited -> {
                    this.view.invite_button.visibility = View.GONE
                    this.view.status_label_text_view.visibility = View.VISIBLE
                    this.view.accept_button.visibility = View.GONE
                    this.view.decline_button.visibility = View.GONE
                }

                FriendStatus.Requested -> {
                    this.view.invite_button.visibility = View.GONE
                    this.view.status_label_text_view.visibility = View.GONE
                    this.view.accept_button.visibility = View.VISIBLE
                    this.view.decline_button.visibility = View.VISIBLE
                }

                // TODO: This status is not currently implemented. We should not get here, but if we do, show nothing.
                FriendStatus.Verified -> {
                    this.view.invite_button.visibility = View.GONE
                    this.view.status_label_text_view.visibility = View.GONE
                    this.view.accept_button.visibility = View.GONE
                    this.view.decline_button.visibility = View.GONE
                }

                // TODO: Might be nice to allow the user to send a message to Approved friends from this view.
                FriendStatus.Approved -> {
                    this.view.invite_button.visibility = View.GONE
                    this.view.status_label_text_view.visibility = View.GONE
                    this.view.accept_button.visibility = View.GONE
                    this.view.decline_button.visibility = View.GONE
                }
            }

        }
    }

}
