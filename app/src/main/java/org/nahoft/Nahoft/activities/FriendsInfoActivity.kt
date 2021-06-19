package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_friends_info.*
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.FriendStatus
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.util.RequestCodes
import org.nahoft.util.ShareUtil

class FriendsInfoActivity: AppCompatActivity() {

    private lateinit var thisFriend: Friend

    private val TAG = "FriendsInfoActivity"
    private var editingMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends_info)

        // Get our pending friend
        val maybeFriend = intent.getSerializableExtra(RequestCodes.friendExtraTaskDescription) as? Friend

        if (maybeFriend == null) { // this should never happen, get out of this activity.
            Log.e(TAG, "Attempted to open FriendInfoActivity, but Friend was null.")
            return
        } else {
            thisFriend = maybeFriend
        }

        setClickListeners()
        setupViewByStatus()
    }

    override fun onResume() {
        super.onResume()

        setupViewByStatus()
    }

    private fun setClickListeners() {
        invite_button.setOnClickListener {
            inviteClicked()
        }

        import_invitation_button.setOnClickListener {
            importInvitationClicked()
        }

        decline_button.setOnClickListener {
            declineClicked()
        }

        edit_or_save_button.setOnClickListener {
           saveOrEditClicked()
        }
    }

    private fun saveOrEditClicked() {
        if (editingMode) // Save clicked
        {
            this.hideSoftKeyboard(friend_name_edit_text)

            // Save Changes and exit editing mode
            editingMode = false

            // If a new name has been entered, save it and display it
            if (friend_name_edit_text.text.isNotBlank())
            {
                val newName = friend_name_edit_text.text.toString()

                if (newName != thisFriend.name)
                {
                    Persist.updateFriend(this, thisFriend, newName)
                }

                thisFriend.name = newName
            }
        }
        else // Edit clicked
        {
            // enter editing mode
            editingMode = true
        }

        // Update view to not be in the correct mode
        setupViewByStatus()
    }

    private fun verifyClicked() {
        verifyFriendDialog()
    }

    private fun inviteClicked() {
        // Get user's public key to send to contact
        val userPublicKey = Encryption().ensureKeysExist().publicKey
        val keyBytes = userPublicKey.toBytes()

        // Share the key
        ShareUtil.shareKey(applicationContext, keyBytes)

        if (thisFriend.status == FriendStatus.Requested) {
            // We have already received an invitation from this friend.
            // Set friend status to approved.
            thisFriend.status = FriendStatus.Approved
            setupApprovedView()
        } else {
            // We have not received an invitation from this friend.
            // Set friend status to Invited
            thisFriend.status = FriendStatus.Invited
            setupInvitedView()
        }

        Persist.updateFriend(applicationContext, thisFriend, newStatus = FriendStatus.Invited)
    }

    private fun importInvitationClicked() {
        val importIntent = Intent(applicationContext, ImportTextActivity::class.java)
        importIntent.putExtra(ImportTextActivity.SENDER, thisFriend)
        applicationContext.startActivity(importIntent)
        setupRequestedView()
    }

    private fun declineClicked() {
        // Set Friend Status to Default
        thisFriend.status = FriendStatus.Default
        thisFriend.publicKeyEncoded = null
        Persist.updateFriend(applicationContext, thisFriend, newStatus = FriendStatus.Default)
        setupDefaultView()
    }

    private fun verifyFriendDialog() {
        // Display friend public key as encoded text.
        val codex = Codex()
        val friendCode = codex.encodeKey(PublicKey(thisFriend.publicKeyEncoded).toBytes())

        // Display user public key as encoded text
        val userCode = codex.encodeKey(Encryption().ensureKeysExist().publicKey.toBytes())
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        //builder.setMessage(resources.getString(R.string.enter_nickname))

        val friendCodeLabelTextView = TextView(this)
        friendCodeLabelTextView.text = getString(R.string.label_verify_friend_number, thisFriend.name)
        builder.setView(friendCodeLabelTextView)

        val friendCodeTextView = TextView(this)
        friendCodeTextView.text = friendCode
        builder.setView(friendCodeTextView)

        val userCodeLabelTextView = TextView(this)
        userCodeLabelTextView.text = getString(R.string.label_verify_friend_user_number)
        builder.setView(userCodeLabelTextView)

        val userCodeTextView = TextView(this)
        userCodeTextView.text = userCode
        builder.setView(userCodeTextView)

        // Set the Add and Cancel Buttons
        builder.setPositiveButton(resources.getString(R.string.ok_button)) {
                dialog, _->
            Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Verified,
                encodedPublicKey = thisFriend.publicKeyEncoded
            )
        }
        builder.setNegativeButton(resources.getString(R.string.button_label_reset)) {
                dialog, _->
            thisFriend.publicKeyEncoded = null
            Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Default)
            dialog.cancel()
        }
            .create()
            .show()
    }

    private fun setupViewByStatus() {
        friend_info_name_text_view.text = thisFriend.name
        status_text_view.text = thisFriend.status.name
        when (thisFriend.status) {
            FriendStatus.Default -> setupDefaultView()
            FriendStatus.Requested -> setupRequestedView()
            FriendStatus.Invited -> setupInvitedView()
            FriendStatus.Verified -> setupVerifiedView()
            FriendStatus.Approved -> setupApprovedView()
        }
    }

    private fun setupEditViewBasics()
    {
        friend_name_edit_text.isVisible = true
        friend_info_name_text_view.isVisible = false
        delete_friend_button.isVisible = true
        edit_or_save_button.text = getString(R.string.button_label_save)
        edit_or_save_button.setBackgroundResource(R.drawable.blue_56_btn_bkgd)
    }

    private fun setupNormalViewBasics()
    {
        friend_name_edit_text.isVisible = false
        friend_info_name_text_view.isVisible = true
        delete_friend_button.isVisible = false
        edit_or_save_button.text = getString(R.string.edit_button)
        edit_or_save_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
    }

    private fun setupDefaultView()
    {
        status_icon_image_view.setImageResource(FriendStatus.Default.getIcon())
        import_invitation_button.isVisible = true
        invite_button.isVisible = true
        invite_button.text = getString(R.string.button_label_invite)

        // These buttons should not be visible for default status friends
        verification_code_button.isGone = true
        verify_button.isVisible = false
        decline_button.isVisible = false
        send_message_button.isVisible = false
        import_image_button.isVisible = false
        import_text_button.isVisible = false

        // Handle editing mode on/off
        if (editingMode)
        {
            setupEditViewBasics()

            import_invitation_button.isEnabled = false
            import_invitation_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
            invite_button.isEnabled = false
            invite_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
            status_description_text_view.isVisible = false
        }
        else
        {
            setupNormalViewBasics()

            import_invitation_button.isEnabled = true
            import_invitation_button.setBackgroundResource(R.drawable.blue_56_btn_bkgd)
            invite_button.isEnabled = true
            invite_button.setBackgroundResource(R.drawable.blue_56_btn_bkgd)
            status_description_text_view.isVisible = true
        }
    }

    private fun setupInvitedView()
    {
        status_icon_image_view.setImageResource(FriendStatus.Invited.getIcon())
        import_invitation_button.isVisible = true
        invite_button.isVisible = true
        invite_button.text = getString(R.string.button_label_invite_again)

        // These buttons should not be visible for invited status friends
        verification_code_button.isGone = true
        decline_button.isVisible = false
        verify_button.isVisible = false
        send_message_button.isVisible = false
        import_image_button.isVisible = false
        import_text_button.isVisible = false
        delete_friend_button.isVisible = false

        // Handle editing mode on/off
        if (editingMode)
        {
            setupEditViewBasics()

            import_invitation_button.isEnabled = false
            import_invitation_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
            invite_button.text = getString(R.string.button_label_invite_again)
            invite_button.isEnabled = false
            invite_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
            status_description_text_view.isVisible = false
        }
        else
        {
            setupNormalViewBasics()

            import_invitation_button.isEnabled = true
            import_invitation_button.setBackgroundResource(R.drawable.blue_56_btn_bkgd)
            invite_button.isEnabled = true
            invite_button.text = getString(R.string.button_label_invite_again)
            invite_button.setBackgroundResource(R.drawable.blue_56_btn_bkgd)
            status_description_text_view.isVisible = true
        }
    }

    private fun setupRequestedView()
    {
        status_icon_image_view.setImageResource(FriendStatus.Requested.getIcon())
        decline_button.isVisible = true
        invite_button.isVisible = true
        invite_button.text = getString(R.string.button_label_invite)

        // These buttons should not be visible for requested status friends

        verification_code_button.isGone = true
        verify_button.isVisible = false
        send_message_button.isVisible = false
        import_image_button.isVisible = false
        import_text_button.isVisible = false
        delete_friend_button.isVisible = false
        import_invitation_button.isVisible = false

        if (editingMode)
        {
            setupEditViewBasics()

            invite_button.isEnabled = false
            invite_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
            invite_button.text = getString(R.string.button_label_invite)
            decline_button.isEnabled = false
            decline_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
            friend_info_name_text_view.setBackgroundResource(R.drawable.white_8_bkgd)
            status_description_text_view.isVisible = false
        }
        else
        {
            setupNormalViewBasics()

            decline_button.isEnabled = true
            decline_button.setBackgroundResource(R.drawable.orange_56_btn_bkgd)
            invite_button.isEnabled = true
            invite_button.setBackgroundResource(R.drawable.blue_56_btn_bkgd)
            status_description_text_view.isVisible = true
        }
    }

    private fun setupApprovedView()
    {
        status_icon_image_view.setImageResource(FriendStatus.Approved.getIcon())
        invite_button.isVisible = true
        invite_button.text = getString(R.string.button_label_invite_again)
        verify_button.isVisible = true

        // These buttons should not be visible for approved status friends
        verification_code_button.isGone = true
        import_invitation_button.isVisible = false
        decline_button.isVisible = false
        send_message_button.isVisible = false
        import_image_button.isVisible = false
        import_text_button.isVisible = false
        delete_friend_button.isVisible = false


        if (editingMode)
        {
            setupEditViewBasics()

            invite_button.isEnabled = false
            invite_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
            invite_button.text = getString(R.string.button_label_invite_again)
            verify_button.isEnabled = false
            verify_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
            status_description_text_view.isVisible = false
            edit_or_save_button.setBackgroundResource(R.drawable.blue_56_btn_bkgd)
            friend_info_name_text_view.setBackgroundResource(R.drawable.white_8_bkgd)
        }
        else
        {
            setupNormalViewBasics()

            verify_button.isVisible = true
            invite_button.isVisible = true
            invite_button.text = getString(R.string.button_label_invite_again)
            status_description_text_view.isVisible = true
            edit_or_save_button.setBackgroundResource(R.drawable.transparent_56_btn_bkgd)
        }
    }

    private fun setupVerifiedView()
    {
        status_icon_image_view.setImageResource(FriendStatus.Verified.getIcon())
        verification_code_button.isGone = false

        // These buttons should not be visible for verified status friends
        import_invitation_button.isVisible = false
        decline_button.isVisible = false
        invite_button.isVisible = false
        verify_button.isVisible = false
        status_description_text_view.isVisible = false

        if (editingMode)
        {
            setupEditViewBasics()

            verification_code_button.isEnabled = false
            verification_code_button.setBackgroundResource(R.drawable.grey_outline_8_btn_bkgd)
            friend_info_name_text_view.setBackgroundResource(R.drawable.white_8_bkgd)
            send_message_button.isEnabled = false
            send_message_button.setBackgroundResource(R.drawable.transparent_overlay_radius_8)
            import_image_button.isEnabled = false
            import_image_button.setBackgroundResource(R.drawable.transparent_overlay_radius_8)
            import_text_button.isEnabled = false
            import_text_button.setBackgroundResource(R.drawable.transparent_overlay_radius_8)
            delete_friend_button.isVisible = true
            edit_or_save_button.setBackgroundResource(R.drawable.blue_56_btn_bkgd)
        }
        else
        {
            setupNormalViewBasics()
            
            verification_code_button.setBackgroundResource(R.drawable.green_outline_8_btn_bkgd)
            send_message_button.isVisible = true
            import_image_button.isVisible = true
            import_text_button.isVisible = true
        }
    }

    fun Activity.hideSoftKeyboard(editText: EditText)
    {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(editText.windowToken, 0)
        }
    }
}