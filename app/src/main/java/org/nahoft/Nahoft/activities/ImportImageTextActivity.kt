package org.nahoft.nahoft.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_import_image_text.*
import kotlinx.android.synthetic.main.activity_import_image_text.import_help_button
import kotlinx.coroutines.*
import org.nahoft.codex.Codex
import org.nahoft.codex.KeyOrMessage
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.org.nahoft.swatch.Decoder
import org.nahoft.util.showAlert
import org.nahoft.util.RequestCodes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ImportImageTextActivity: AppCompatActivity() {

    private var decodePayload: ByteArray? = null
    private var sender: Friend? = null
    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

   /* private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
        }
    }*/

    companion object {
        const val SENDER = "Sender"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_image_text)

        sender = intent.getSerializableExtra(SENDER) as Friend?

        // Help Button
        import_help_button.setOnClickListener {showDialogButtonImportHelp()}

        import_text_button.setOnClickListener {
            handleMessageImport()
        }

        import_image_button.setOnClickListener {
            handleImageImport()
        }
    }

/*    override fun onStop() {

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })
        cleanUp()
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        unregisterReceiver(receiver)
    }*/

    private fun showDialogButtonImportHelp() {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.dialog_button_import_help_title))
            .setMessage(resources.getString(R.string.dialog_button_import_help))
            .setPositiveButton(resources.getString(R.string.ok_button)) {
                    dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    /// If the message is decoded successfully, user will be sent to a select sender activity
    /// Message decryption and saving will be handled in the onActivityResult function
    private fun handleMessageImport() {
        
        val messageText = import_message_text_view.text.toString()
        if (messageText.isNotEmpty()) {

            if (messageText.length > import_message_text_layout.counterMaxLength)
            {
                showAlert(getString(R.string.alert_text_message_too_long))
                return
            }

            val decodeResult = Codex().decode(messageText)

            if (decodeResult != null) {
                this.decodePayload = decodeResult.payload

                if (decodeResult.type == KeyOrMessage.EncryptedMessage) {
                    // We received a message, have the user select who it is from
                    val selectSenderIntent = Intent(this, SelectMessageSenderActivity::class.java)
                    startActivityForResult(selectSenderIntent, RequestCodes.selectMessageSenderCode)
                }
                else if (sender != null)
                {
                    updateKeyAndStatus(sender!!, decodePayload!!)
                }
                else
                {
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
        startActivityForResult(intent, RequestCodes.selectImageForSharingCode)
    }

    @ExperimentalUnsignedTypes
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RequestCodes.selectMessageSenderCode) {
                val selectedSender = data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription)
                    ?.let { it as Friend }

                if (selectedSender != null)

                {
                    if (decodePayload != null)
                    {
                        // Create Message Instance
                        val newMessage = createAndSaveMessage(selectedSender, decodePayload!!)

                        // Go to message view
                        val messageArguments = MessageActivity.Arguments(message = newMessage)
                        messageArguments.startActivity(this)
                    } else {
                        showAlert(getString(R.string.alert_text_unable_to_decode_message))
                    }
                } else {
                    showAlert(getString(R.string.alert_text_unable_to_process_request))
                }


            } else if (requestCode == RequestCodes.selectKeySenderCode) {
                val selectedSender = data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription)
                    ?.let { it as Friend }

                // Update this friend with a new key and a new status
                if (selectedSender != null && decodePayload != null)
                {
                    updateKeyAndStatus(selectedSender, decodePayload!!)
                }
                else
                {
                    this.showAlert(getString(R.string.alert_text_unable_to_update_friend_status))
                }
            }
            else if (requestCode == RequestCodes.selectImageForSharingCode)
            {
                // get data?.data as URI
                val imageURI = data?.data
                imageURI?.let {

                    makeWait()

                    val decodeResult: Deferred<ByteArray?> =
                        coroutineScope.async(Dispatchers.IO) {
                            val swatch = Decoder()
                            return@async swatch.decode(applicationContext, imageURI)
                        //return@async Stencil().decode(applicationContext, it)
                    }

                    coroutineScope.launch(Dispatchers.Main) {
                        val maybeBytes = decodeResult.await()
                        noMoreWaiting()
                        handleImageDecodeResult(maybeBytes)
                    }
                }
            }
        }
    }

    private fun handleImageDecodeResult(maybeBytes: ByteArray?)
    {
        if (maybeBytes != null) {
            // Decode the message and save it locally for use after sender is selected
            this.decodePayload = maybeBytes

            // We received a message, have the user select who it is from
            val selectSenderIntent = Intent(this, SelectMessageSenderActivity::class.java)
            startActivityForResult(selectSenderIntent, RequestCodes.selectMessageSenderCode)
        } else {
            showAlert(getString(R.string.alert_text_unable_to_decode_message))
        }
    }

    private fun createAndSaveMessage(messageSender: Friend, messageData: ByteArray): Message
    {
        val date = LocalDateTime.now()
        val stringDate = date.format(DateTimeFormatter.ofPattern("M/d/y H:m"))
        val newMessage = Message(stringDate, messageData, messageSender)

        Persist.messageList.add(newMessage)
        Persist.saveMessagesToFile(this)

        import_message_text_view.text?.clear()

        return newMessage
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

    private fun makeWait()
    {
        imageImportProgressBar.visibility = View.VISIBLE
        import_text_button.isEnabled = false
        import_text_button.isClickable = false
        import_image_button.isEnabled = false
        import_image_button.isClickable = false
        import_message_text_view.isEnabled = false
        import_message_text_view.isClickable = false
    }

    private fun noMoreWaiting()
    {
        imageImportProgressBar.visibility = View.INVISIBLE
        import_text_button.isEnabled = true
        import_text_button.isClickable = true
        import_image_button.isEnabled = true
        import_image_button.isClickable = true
        import_message_text_view.isEnabled = true
        import_message_text_view.isClickable = true
    }

/*    fun cleanUp () {
        decodePayload = null
        sender = null
        import_message_text_view.text = null
        //showAlert("Import Image Text Activity Logout Timer Broadcast Received", length = Toast.LENGTH_LONG)
    }*/
}
