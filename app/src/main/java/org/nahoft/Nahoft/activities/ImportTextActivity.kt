package org.nahoft.nahoft.activities

import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_import_text.*
import org.nahoft.codex.Codex
import org.nahoft.codex.KeyOrMessage
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.util.showAlert

class ImportTextActivity: AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private var sender: Friend? = null

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanUp()
        }
    }

    companion object {
        const val SENDER = "Sender"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_text)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        sender = intent.getSerializableExtra(ImportImageActivity.SENDER) as Friend?

        import_text_button.setOnClickListener {
            handleMessageImport()
        }

        setupFriendDropdown()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (position != 0) // The first value is a placeholder
        {
            val maybeFriend: Friend = parent?.selectedItem as Friend
            maybeFriend.let { userTappedFriend: Friend ->
                sender = userTappedFriend
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        //Stub
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun setupFriendDropdown()
    {
        // Only show friends that have been verified
        val allFriends = Friends().allFriendsSpinnerList()
        val friendAdapter: ArrayAdapter<Friend> = ArrayAdapter(this, R.layout.spinner, allFriends)
        val friendsSpinner: Spinner = findViewById(R.id.message_sender_spinner)
        friendsSpinner.adapter = friendAdapter
        friendsSpinner.onItemSelectedListener = this
        friendsSpinner.setSelection(0, false)
    }

    private fun handleMessageImport()
    {
        if (sender == null)
        {
            showAlert(getString(R.string.alert_text_which_friend_sent_this_message))
        }
        else
        {
            val messageText = import_message_text_view.text.toString()
            if (messageText.isNotEmpty())
            {
                if (messageText.length > import_message_text_layout.counterMaxLength)
                {
                    showAlert(getString(R.string.alert_text_message_too_long))
                    return
                }

                val decodeResult = Codex().decode(messageText)

                if (decodeResult != null)
                {
                    when (decodeResult.type)
                    {
                        KeyOrMessage.EncryptedMessage ->
                        {
                            // Create Message Instance
                            val newMessage = Message(decodeResult.payload, sender!!)
                            newMessage.save(this)

                            // Go to message view
                            val messageArguments = MessageActivity.Arguments(message = newMessage)
                            messageArguments.startActivity(this)
                            import_message_text_view.text?.clear()
                        }
                        KeyOrMessage.Key ->
                        {
                            updateKeyAndStatus(sender!!, decodeResult.payload)
                            import_message_text_view.text?.clear()
                        }
                    }
                }
                else
                {
                    this.showAlert(getString(R.string.alert_text_unable_to_decode_message))
                }
            }
        }
    }

    private fun updateKeyAndStatus(keySender: Friend, keyData: ByteArray)
    {
        when (keySender.status)
        {
            FriendStatus.Default ->
            {
                Persist.updateFriend(
                    context = this,
                    friendToUpdate = keySender,
                    newStatus = FriendStatus.Requested,
                    encodedPublicKey = keyData
                )
                this.showAlert(
                    getString(
                        R.string.alert_text_received_invitation,
                        keySender.name
                    )
                )
                finish()
            }

            FriendStatus.Invited ->
            {
                Persist.updateFriend(
                    context = this,
                    friendToUpdate = keySender,
                    newStatus = FriendStatus.Approved,
                    encodedPublicKey = keyData
                )

                this.showAlert(keySender.name, (R.string.alert_text_invitation_accepted))
                finish()
            }

            else ->
                this.showAlert(getString(R.string.alert_text_unable_to_update_friend_status))
        }
    }

    private fun cleanUp () {
        sender = null
        import_message_text_view.text?.clear()
    }

}