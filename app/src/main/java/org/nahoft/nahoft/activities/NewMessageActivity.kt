package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.coroutines.*
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.R
import org.nahoft.showAlert
import org.nahoft.stencil.Stencil
import org.nahoft.util.RequestCodes
import org.nahoft.util.ShareUtil

class NewMessageActivity : AppCompatActivity() {

    private var selectedFriend: Friend? = null
    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        // Select Friend Button
        friend_button.setOnClickListener {
            selectFriend()
        }

        // Send message as text button
        send_as_text_button.setOnClickListener {
            trySendingMessage(false)
        }

        // Send message as image button
        send_as_image_button.setOnClickListener {
            trySendingMessage(true)
        }
    }

    private fun selectFriend() {
        val intent = FriendSelectionActivity.newIntent(this@NewMessageActivity)
        Intent(this, FriendSelectionActivity::class.java)
        startActivityForResult(intent, RequestCodes.selectFriendCode)
    }

    private fun pickImageFromGallery()
    {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickImageIntent, RequestCodes.selectImageCode)
    }

    private fun trySendingMessage(isImage: Boolean)
    {

        // Make sure there is a message to send
        val message = editMessageText.text.toString()
        if (message.isBlank())
        {
            showAlert(getString(R.string.alert_text_write_a_message_to_send))
            return
        }

        if (message.length > compose_message_text_layout.counterMaxLength)
        {
            showAlert(getString(R.string.alert_text_message_too_long))
            return
        }

        // Make sure there is a friend to create the message for.
        if (selectedFriend == null) {
            this.showAlert(getString(R.string.alert_text_select_friend_to_send_message))
            return
        }

        if (isImage) {
            // If the message is sent as an image
            pickImageFromGallery()
        } else {
            // If the message is sent as text
            if (selectedFriend!!.publicKeyEncoded != null) {

                // Share this message as a text
                ShareUtil.shareText(this, message, selectedFriend!!.publicKeyEncoded!!)

                // Clean up the text box and the friend selection.
                editMessageText.text?.clear()
                selectedFriend = null
                friend_button.text = getString(R.string.hintOnChooseFriendButton)
            } else {
                this.showAlert(getString(R.string.alert_text_verified_friends_only))
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RequestCodes.selectImageCode) {
                if (selectedFriend != null) {
                    selectedFriend?.publicKeyEncoded?.let {

                        // Get the message text
                        val message = editMessageText.text.toString()

                        // get data?.data as URI
                        val imageURI = data?.data

                        // Likely need to start the animation here or just before ShareUtil

                        imageURI?.let {
                            imageShareProgressBar.visibility = View.VISIBLE
                            shareAsImage(imageURI, message, selectedFriend!!.publicKeyEncoded!!)
                            editMessageText.text?.clear()
                        }
                    }
                } else {
                    this.showAlert(getString(R.string.alert_text_verified_friends_only))
                    return
                }

                // We can only share an image if a recipient with a public key has been selected

            } else if (requestCode == RequestCodes.selectFriendCode) {

                // Get Friend information from data
                val friend = data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription) as? Friend

                // If friend is not null use it to set the button title and set selectedFriend
                friend?.let {
                    this.selectedFriend = friend
                    friend_button.text = friend.name
                }
            }
        }
    }

    private fun shareAsImage(imageUri: Uri, message: String, encodedFriendPublicKey: ByteArray) {
        try {
            // Encrypt the message
            val encryptedMessage = Encryption(applicationContext).encrypt(encodedFriendPublicKey, message)

            // Encode the image
            val newUri: Deferred<Uri?> =
                coroutineScope.async(Dispatchers.IO) {
                    return@async Stencil().encode(applicationContext, encryptedMessage, imageUri)
                }

            coroutineScope.launch(Dispatchers.Main) {
                val maybeUri = newUri.await()
                // Save bitmap to image roll to get URI for sharing intent
                if (maybeUri != null) {
                    imageShareProgressBar.visibility = View.INVISIBLE
                    ShareUtil.shareImage(applicationContext, maybeUri!!)
                } else {
                    applicationContext.showAlert(applicationContext.getString(R.string.alert_text_unable_to_process_request))
                    print("Unable to send message as photo, we were unable to encode the selected image.")
                }
            }
        } catch (exception: SecurityException) {
            applicationContext.showAlert(applicationContext.getString(R.string.alert_text_unable_to_process_request))
            print("Unable to send message as photo, we were unable to encrypt the mess56age.")
            return
        }
    }
//
//    private suspend fun shareAsImageAsync(imageURI: Uri, message: String, publicKeyEncoded: ByteArray) = withContext(Dispatchers.Default) {
//
//
//    }


}
