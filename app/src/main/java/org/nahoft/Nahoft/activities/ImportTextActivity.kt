package org.nahoft.nahoft.activities

import android.content.Intent
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
import org.nahoft.util.RequestCodes
import org.nahoft.util.showAlert

class ImportTextActivity: AppCompatActivity(), AdapterView.OnItemSelectedListener
{
    private var sender: Friend? = null

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanUp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_text)

        makeSureAccessIsAllowed()

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        // Check to see if a friend was selected in a previous activity
        val maybeFriend = intent.getSerializableExtra(RequestCodes.friendExtraTaskDescription) as? Friend
        if (maybeFriend != null)
        {
            sender = maybeFriend
        }

        import_text_button.setOnClickListener {
            handleMessageImport()
        }

        setupFriendDropdown()
        receiveSharedMessages()
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

    override fun onDestroy()
    {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun setupFriendDropdown()
    {
        // Only show friends that have been verified
        val allFriends = Friends().allFriendsSpinnerList()
        val friendAdapter: ArrayAdapter<Friend> = ArrayAdapter(this, R.layout.spinner, allFriends)
        val friendSpinner: Spinner = findViewById(R.id.message_sender_spinner)
        friendSpinner.adapter = friendAdapter
        friendSpinner.onItemSelectedListener = this

        // If sender != null select the correct friend in the spinner
        if (sender != null)
        {
            friendSpinner.setSelection(friendAdapter.getPosition(sender))
        }
        else
        {
            friendSpinner.setSelection(0, false)
        }
    }

    private fun receiveSharedMessages()
    {
        // Receive shared messages
        if (intent?.action == Intent.ACTION_SEND)
        {
            if (intent.type == "text/plain")
            {
                // Check to see if we received a text send intent
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    //Populate the edittext view
                    import_message_text_view.setText(it)

                    //Attempt to decode the message
                    decodeStringMessage(it)
                }
            }
            else
            {
                showAlert(getString(R.string.alert_text_unable_to_process_request))
            }
        }
        else // See if we got intent extras from the Login Activity
        {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let{
                //Populate the edittext view
                import_message_text_view.setText(it)

                //Attempt to decode the message
                decodeStringMessage(it)
            }
        }
    }

    private fun decodeStringMessage(messageString: String)
    {
        // Update UI to reflect text being shared
        val decodeResult = Codex().decode(messageString)

        if (decodeResult != null)
        {
            when (decodeResult.type)
            {
                KeyOrMessage.EncryptedMessage ->
                {
                    if (sender == null)
                    {
                        showAlert(getString(R.string.alert_text_which_friend_sent_this_message))
                    }
                    else
                    {
                        // Create Message Instance
                        val newMessage = Message(decodeResult.payload, sender!!)
                        newMessage.save(this)

                        // Go to message view
                        val messageArguments = MessageActivity.Arguments(message = newMessage)
                        messageArguments.startActivity(this)
                    }
                }
                KeyOrMessage.Key ->
                {
                    if (sender == null)
                    {
                        showAlert(getString(R.string.alert_text_which_friend_sent_this_invitation))
                    }
                    else
                    {
                        updateKeyAndStatus(sender!!, decodeResult.payload)
                    }
                }
            }
        }
        else
        {
            this.showAlert(getString(R.string.alert_text_unable_to_decode_message))
        }
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

                decodeStringMessage(messageText)
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
                goToFriendList()
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
                goToFriendList()
            }

            else ->
                this.showAlert(getString(R.string.alert_text_unable_to_update_friend_status))
        }
    }

    private fun goToFriendList()
    {
        val friendListIntent = Intent(this, FriendListActivity::class.java)
        startActivity(friendListIntent)
    }

    private fun makeSureAccessIsAllowed()
    {
        Persist.getStatus()

        if (Persist.status == LoginStatus.NotRequired || Persist.status == LoginStatus.LoggedIn)
        {
            return
        }
        else
        {
            sendToLogin()
        }
    }

    private fun sendToLogin()
    {
        // If the status is not either NotRequired, or Logged in, request login
        this.showAlert(getString(R.string.alert_text_passcode_required_to_proceed))

        // Send user to the Login Activity
        val loginIntent = Intent(applicationContext, LogInActivity::class.java)

        // We received a shared message but the user is not logged in
        // Save the intent
        if (intent?.action == Intent.ACTION_SEND)
        {
            if (intent.type == "text/plain")
            {
                val messageString = intent.getStringExtra(Intent.EXTRA_TEXT)
                loginIntent.putExtra(Intent.EXTRA_TEXT, messageString)
            }
            else
            {
                showAlert(getString(R.string.alert_text_unable_to_process_request))
            }
        }

        startActivity(loginIntent)
    }

    private fun cleanUp () {
        sender = null
        import_message_text_view.text?.clear()
    }

}