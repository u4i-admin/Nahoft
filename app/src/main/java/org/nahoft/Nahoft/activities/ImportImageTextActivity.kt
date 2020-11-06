package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_import_image_text.*
import kotlinx.android.synthetic.main.activity_new_message.*
import org.nahoft.codex.Codex
import org.nahoft.codex.KeyOrMessage
import org.nahoft.nahoft.*
import org.nahoft.showAlert
import org.nahoft.stencil.Stencil
import org.nahoft.util.RequestCodes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ImportImageTextActivity : AppCompatActivity() {

    private var decodePayload: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_image_text)

        import_text_button.setOnClickListener {
            handleMessageImport()
        }

        import_image_button.setOnClickListener {
            handleImageImport()
        }
    }

    /// If the message is decoded successfully, user will be sent to a select sender activity
    /// Message decryption and saving will be handled in the onActivityResult function
    private fun handleMessageImport() {
        
        val messageText = import_message_text_view.text.toString()
        if (messageText.isNotEmpty()) {

            val decodeResult = Codex().decode(messageText)

            if (decodeResult != null) {
                this.decodePayload = decodeResult.payload

                if (decodeResult.type == KeyOrMessage.EncryptedMessage) {
                    // We received a message, have the user select who it is from
                    val selectSenderIntent = Intent(this, SelectMessageSenderActivity::class.java)
                    startActivityForResult(selectSenderIntent, RequestCodes.selectMessageSenderCode)
                } else {
                    // We received a key, have the user select who it is from
                    val selectSenderIntent = Intent(this, SelectKeySenderActivity::class.java)
                    startActivityForResult(selectSenderIntent, RequestCodes.selectKeySenderCode)
                }
            } else {
                this.showAlert(getString(R.string.alert_text_unable_to_decode_message))
            }
        }
    }

    private fun handleImageImport() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, RequestCodes.selectImageCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RequestCodes.selectMessageSenderCode) {
                val sender = data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription)
                    ?.let { it as Friend }

                if (sender != null && decodePayload != null) {
                    // Create Message Instance
                    val date = LocalDateTime.now()
                    val stringDate = date.format(DateTimeFormatter.ofPattern("M/d/y H:m"))
                    val cipherText = decodePayload

                    // TODO do not perisist messages that cannot be decrypted, let user know that message could not be saved/decrypted

                    // TODO add decrypted snippet to list view

                    val newMessage = Message(stringDate, cipherText!!, sender)
                    Persist.messageList.add(newMessage)
                    Persist.saveMessagesToFile(this)

                    // TODO Clear out text view here

                    // Go to message view
                    val messageArguments = MessageActivity.Arguments(message = newMessage)
                    messageArguments.startActivity(this)
                }

            } else if (requestCode == RequestCodes.selectKeySenderCode) {
                val sender = data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription)
                    ?.let { it as Friend }

                // Update this friend with a new key and a new status
                if (sender != null && decodePayload != null) {

                    when (sender.status) {
                        FriendStatus.Default -> {
                            Persist.updateFriend(
                                context = this,
                                friendToUpdate = sender,
                                newStatus = FriendStatus.Requested,
                                encodedPublicKey = decodePayload!!
                            )
                            this.showAlert(
                                getString(
                                    R.string.alert_text_received_invitation,
                                    sender.name
                                )
                            )
                            finish()
                        }

                        FriendStatus.Invited -> {
                            Persist.updateFriend(
                                context = this,
                                friendToUpdate = sender,
                                newStatus = FriendStatus.Approved,
                                encodedPublicKey = decodePayload!!
                            )

                            this.showAlert(sender.name, (R.string.alert_text_invitation_accepted))
                            finish()
                        }

                        else ->
                            this.showAlert(getString(R.string.alert_text_unable_to_update_friend_status))
                    }
                } else {
                    this.showAlert(getString(R.string.alert_text_unable_to_update_friend_status))
                }
            } else if (requestCode == RequestCodes.selectImageCode) {
                // get data?.data as URI
                val imageURI = data?.data
                imageURI?.let {
                    // Decode the message and save it locally for use after sender is selected
                    this.decodePayload = Stencil().decode(this, it)

                    // We received a message, have the user select who it is from
                    val selectSenderIntent = Intent(this, SelectMessageSenderActivity::class.java)
                    startActivityForResult(selectSenderIntent, RequestCodes.selectMessageSenderCode)
                }
            }
        }
    }
}
