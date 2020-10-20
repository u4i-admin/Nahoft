package org.nahoft.nahoft

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.friend_recyclerview_item_row.view.*
import org.nahoft.codex.Encryption
import org.nahoft.inflate
import org.nahoft.nahoft.activities.VerifyFriendActivity
import org.nahoft.nahoft.ui.ItemTouchHelperListener
import org.nahoft.util.RequestCodes
import org.nahoft.util.ShareUtil

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
        friends.removeAt(position)
        Persist.saveFriendsToFile(viewHolder.itemView.context)
        notifyItemRemoved(position)
    }

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

            v.verify_button.setOnClickListener {
                verifyClicked()
            }
        }

        fun bindFriend(newFriend: Friend) {
            this.friend = newFriend
            this.view.friendName.text = newFriend.name
            setupRowByStatus(newFriend)
        }

        override fun onClick(v: View?) {
            // Stub
        }

        fun inviteClicked() {
            // Get user's public key to send to contact
            val userPublicKey = Encryption(this.view.context).ensureKeysExist().publicKey
            val keyBytes = userPublicKey.toBytes()

            // Share the key
            ShareUtil.shareKey(this.view.context, keyBytes)

            // Set friend status to Invited
            this.friend?.status = FriendStatus.Invited
            Persist.updateFriend(view.context, this.friend!!, FriendStatus.Invited)
            setupInvitedRow()
        }

        fun acceptClicked() {
            // Make sure that we have successfully received the friend's public key
            if (this.friend?.publicKeyEncoded != null) {

                val userPublicKey = Encryption(this.view.context).ensureKeysExist().publicKey
                val keyBytes = userPublicKey.toBytes()

                // Share the key
                ShareUtil.shareKey(this.view.context, keyBytes)

                // Set friend status to Accepted
                this.friend?.status = FriendStatus.Approved
                Persist.updateFriend(view.context, this.friend!!, FriendStatus.Approved)
                setupApprovedRow()
            }
        }

        fun declineClicked() {
            // Set Friend Status to Default
            this.friend?.status = FriendStatus.Default
            Persist.updateFriend(view.context, this.friend!!, FriendStatus.Default)
            setupDefaultRow()
        }

        fun verifyClicked() {
            if (friend != null) {
                val verifyFriendIntent = Intent(this.view.context, VerifyFriendActivity::class.java)
                verifyFriendIntent.putExtra(RequestCodes.friendExtraTaskDescription, friend)
                this.view.context.startActivity(verifyFriendIntent)
            }

        }

        fun setupRowByStatus(thisFriend: Friend) {
            when (thisFriend.status) {
                FriendStatus.Default -> {
                    setupDefaultRow()
                }

                // TODO: User may want to send the key again.
                FriendStatus.Invited -> {
                    setupInvitedRow()
                }

                FriendStatus.Requested -> {
                    setupRequestedRow()
                }

                FriendStatus.Approved -> {
                    setupApprovedRow()
                }

                // TODO: This status is not currently implemented. We should not get here, but if we do, show nothing.
                FriendStatus.Verified -> {
                    setupVerifiedRow()
                }
            }
        }

        fun setupDefaultRow() {
            this.view.friendIcon.setImageResource(FriendStatus.Default.getIcon())
            this.view.invite_button.visibility = View.VISIBLE
            this.view.invite_button.text = view.context.getString(R.string.button_label_invite)
            this.view.accept_button.visibility = View.GONE
            this.view.decline_button.visibility = View.GONE
            this.view.verify_button.visibility = View.GONE
        }

        fun setupInvitedRow() {
            this.view.friendIcon.setImageResource(FriendStatus.Invited.getIcon())
            this.view.invite_button.visibility = View.VISIBLE
            this.view.invite_button.text = view.context.getString(R.string.button_label_invite_again)
            this.view.accept_button.visibility = View.GONE
            this.view.decline_button.visibility = View.GONE
            this.view.verify_button.visibility = View.GONE
        }

        fun setupRequestedRow() {
            this.view.friendIcon.setImageResource(FriendStatus.Requested.getIcon())
            this.view.invite_button.visibility = View.GONE
            this.view.accept_button.visibility = View.VISIBLE
            this.view.decline_button.visibility = View.VISIBLE
            this.view.verify_button.visibility = View.GONE
        }

        fun setupApprovedRow() {
            this.view.friendIcon.setImageResource(FriendStatus.Approved.getIcon())
            this.view.invite_button.visibility = View.GONE
            this.view.accept_button.visibility = View.GONE
            this.view.decline_button.visibility = View.GONE
            this.view.verify_button.visibility = View.VISIBLE
        }

        fun setupVerifiedRow() {
            this.view.friendIcon.setImageResource(FriendStatus.Verified.getIcon())
            this.view.invite_button.visibility = View.GONE
            this.view.accept_button.visibility = View.GONE
            this.view.decline_button.visibility = View.GONE
            this.view.verify_button.visibility = View.GONE
        }
    }
}
