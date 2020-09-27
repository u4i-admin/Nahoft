package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_home.*
import org.nahoft.nahoft.*
import org.nahoft.codex.Codex
import org.nahoft.codex.KeyOrMessage
import org.nahoft.nahoft.Persist.Companion.friendsFilename
import org.nahoft.nahoft.Persist.Companion.messagesFilename
import org.nahoft.stencil.Stencil
import org.nahoft.util.RequestCodes
import java.io.File
import java.util.*

class HomeActivity : AppCompatActivity() {

    private var decodePayload: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        Persist.app = Nahoft()

        messages_button.setOnClickListener {
            println("message button clicked")
            val messagesIntent = Intent(this, MessagesActivity::class.java)
            startActivity(messagesIntent)
        }

        user_guide_button.setOnClickListener {
            val userGuideIntent = Intent(this, UserGuideActivity::class.java)
            startActivity(userGuideIntent)
        }

        friends_button.setOnClickListener {
            val friendsIntent = Intent(this, FriendsActivity::class.java)
            startActivity(friendsIntent)
        }

        settings_button.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        // Receive shared messages
        when (intent?.action) {
            Intent.ACTION_SEND -> {

                // Received shared data check LoginStatus
                if (Persist.status == LoginStatus.NotRequired) {
                    // We may not have intialized shared preferences yet, let's do it now
                    Persist().loadEncryptedSharedPreferences(this.applicationContext)
                } else if (Persist.status != LoginStatus.LoggedIn) {
                    //FIXME: If the status is not either NotRequired, or Logged in, request login
                    Toast.makeText(this, "Passcode required to proceed", Toast.LENGTH_LONG).show()
                }

                if ("text/plain" == intent.type) {
                    handleSharedText(intent)
                } else if (intent.type?.startsWith("image/") == true) {
                    handleSharedImage(intent)
                }
            }
        }

        loadSavedFriends()
        loadSavedMessages()
    }

    private fun loadSavedFriends() {

        Persist.friendsFile = File(filesDir.absolutePath + File.separator + friendsFilename )

        // Load our existing friends list from our encrypted file
        if (Persist.friendsFile.exists()) {
            val friendsToAdd = FriendViewModel.getFriends(Persist.friendsFile, applicationContext)
            Persist.friendList.addAll(friendsToAdd)
        }
    }

    private fun loadSavedMessages() {
        Persist.messagesFile = File(filesDir.absolutePath + File.separator + messagesFilename )

        // TODO: Load messages from file
//        if (Persist.messagesFile.exists()) {
//            val messagesToAdd = MessageViewModel.getMessages(Persist.messagesFile, applicationContext)
//            Persist.messageList
//        }
    }

    private fun handleSharedText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            // Update UI to reflect text being shared
            Toast.makeText(this, "Received shared text $it.", Toast.LENGTH_SHORT).show()

            val decodeResult = Codex().decode(it)

            if (decodeResult != null) {
                this.decodePayload = decodeResult.payload

                if (decodeResult.type == KeyOrMessage.EncryptedMessage) {
                    // We received a message, have the user select who it is from
                    val selectSenderIntent = Intent(this, SelectMessageSenderActivity::class.java)
                    startActivityForResult(selectSenderIntent, RequestCodes.selectMessageSenderCode)
                } else {
                    // We received a key

                    // We received a key, have the user select who it is from
                    val selectSenderIntent = Intent(this, SelectKeySenderActivity::class.java)
                    startActivityForResult(selectSenderIntent, RequestCodes.selectKeySenderCode)

                    // TODO: Only allow user to select a friend that we have not already received a key for (Default or Invited status)
                }
            } else {
                Toast.makeText(this, "Something went wrong. We were unable to decode the message.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSharedImage(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            // Update UI to reflect image being shared
            Toast.makeText(this, "Received shared image. $it", Toast.LENGTH_SHORT).show()

            // Decode the message and save it locally for use after sender is selected
            this.decodePayload = Stencil().decode(this, it)

            // We received a message, have the user select who it is from
            val selectSenderIntent = Intent(this, SelectMessageSenderActivity::class.java)
            startActivityForResult(selectSenderIntent, RequestCodes.selectMessageSenderCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RequestCodes.selectMessageSenderCode) {
                val sender = data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription)?.let { it as Friend }

                if (sender != null && decodePayload != null) {
                    // Create Message Instance
                    val date = Calendar.getInstance().time
                    val cipherText = decodePayload

                    val newMessage = Message(date, cipherText!!, sender)
                    Persist.messageList.add(newMessage)

                    // Go to message view
                    val messageArguments = MessageActivity.Arguments(message = newMessage)
                    messageArguments.startActivity(this)
                }

            } else if (requestCode == RequestCodes.selectKeySenderCode) {
                val sender = data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription)?.let { it as Friend }

                // Update this friend with a new key and a new status
                if (sender != null && decodePayload != null) {

                    when (sender.status) {
                        FriendStatus.Default -> {
                            updateFriend(friendToUpdate = sender, newStatus = FriendStatus.Requested, encodedPublicKey = decodePayload!!)
                            Toast.makeText(this, "Received an invitation from ${sender.name}. Accept their invite to start communicating.", Toast.LENGTH_LONG).show()
                        }

                        FriendStatus.Invited -> {
                            updateFriend(friendToUpdate = sender, newStatus = FriendStatus.Approved, encodedPublicKey = decodePayload!!)
                            Toast.makeText(this, "${sender.name} accepted your invitation. You can now communicate securely.", Toast.LENGTH_LONG).show()
                        }

                        else ->
                            Toast.makeText(this, "Something went wrong, we were unable to update your friend status.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Something went wrong, we were unable to update your friend status.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateFriend(friendToUpdate: Friend, newStatus: FriendStatus, encodedPublicKey: ByteArray) {

        //val friendExists = Persist.friendList.any{friend: Friend -> friend.id == friendToUpdate.id}
        Persist.friendList.find { it.id == friendToUpdate.id }?.status = newStatus
        Persist.friendList.find { it.id == friendToUpdate.id }?.publicKeyEncoded = encodedPublicKey
    }

}

