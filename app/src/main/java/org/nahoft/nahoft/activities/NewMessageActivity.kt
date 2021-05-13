package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_message.*
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.coroutines.*
import org.nahoft.codex.Encryption
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.R
import org.nahoft.org.nahoft.swatch.Encoder
import org.nahoft.util.RequestCodes
import org.nahoft.util.ShareUtil
import org.nahoft.util.showAlert

class NewMessageActivity : AppCompatActivity() {

    private var selectedFriend: Friend? = null
    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanUp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        // Select Friend Button
        friend_button.setOnClickListener {
            selectFriend()
        }

        // Send message as text button
        send_as_text_button.setOnClickListener {
            trySendingOrSavingMessage(isImage = false, saveImage = false)
        }

        // Send message as image button
        send_as_image_button.setOnClickListener {
            trySendingOrSavingMessage(isImage = true, saveImage = false)
        }

        // Save message as image button
        save_image_button.setOnClickListener {
            trySendingOrSavingMessage(isImage = true, saveImage = true)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun selectFriend() {
        val intent = FriendSelectionActivity.newIntent(this@NewMessageActivity)
        Intent(this, FriendSelectionActivity::class.java)
        startActivityForResult(intent, RequestCodes.selectFriendCode)
    }

    private fun pickImageFromGallery(saveImage: Boolean) {
        // Calling GetContent contract
        val pickImageIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (saveImage) {
            startActivityForResult(pickImageIntent, RequestCodes.selectImageForSavingCode)
        } else {
            startActivityForResult(pickImageIntent, RequestCodes.selectImageForSharingCode)
        }
    }

    private fun trySendingOrSavingMessage(isImage: Boolean, saveImage: Boolean) {

        // Make sure there is a message to send
        val message = editMessageText.text.toString()
        if (message.isBlank()) {
            showAlert(getString(R.string.alert_text_write_a_message_to_send))
            return
        }

        if (message.length > compose_message_text_layout.counterMaxLength) {
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
            pickImageFromGallery(saveImage)
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

    @ExperimentalUnsignedTypes
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RequestCodes.selectImageForSharingCode) {
                if (selectedFriend != null) {

                    // We can only share an image if a recipient with a public key has been selected
                    selectedFriend?.publicKeyEncoded?.let {

                        // Get the message text
                        val message = editMessageText.text.toString()

                        // get data?.data as URI
                        val imageURI = data?.data

                        imageURI?.let {
                            imageShareProgressBar.visibility = View.VISIBLE
                            shareOrSaveAsImage(
                                imageURI,
                                message,
                                selectedFriend!!.publicKeyEncoded!!,
                                false
                            )
                            editMessageText.text?.clear()
                            selectedFriend = null
                            friend_button.text = getString(R.string.hintOnChooseFriendButton)
                        }
                    }
                } else {
                    this.showAlert(getString(R.string.alert_text_verified_friends_only))
                    return
                }

            } else if (requestCode == RequestCodes.selectFriendCode) {

                // Get Friend information from data
                val friend =
                    data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription) as? Friend

                // If friend is not null use it to set the button title and set selectedFriend
                friend?.let {
                    this.selectedFriend = friend
                    friend_button.text = friend.name
                }
            } else if (requestCode == RequestCodes.selectImageForSavingCode) {
                if (selectedFriend != null) {

                    // We can only share an image if a recipient with a public key has been selected
                    selectedFriend?.publicKeyEncoded?.let {

                        // Get the message text
                        val message = editMessageText.text.toString()

                        // get data?.data as URI
                        val imageURI = data?.data

                        imageURI?.let {
                            imageShareProgressBar.visibility = View.VISIBLE
                            shareOrSaveAsImage(
                                imageURI,
                                message,
                                selectedFriend!!.publicKeyEncoded!!,
                                true
                            )
                            editMessageText.text?.clear()
                            selectedFriend = null
                            friend_button.text = getString(R.string.hintOnChooseFriendButton)
                        }
                    }
                } else {
                    this.showAlert(getString(R.string.alert_text_verified_friends_only))
                    return
                }
            }
        }
    }

    @ExperimentalUnsignedTypes
    private fun shareOrSaveAsImage(
        imageUri: Uri,
        message: String,
        encodedFriendPublicKey: ByteArray,
        saveImage: Boolean
    ) {
        try {
            // Encrypt the message
            val encryptedMessage = Encryption().encrypt(encodedFriendPublicKey, message)

            val newUri: Deferred<Uri?> =
                coroutineScope.async(Dispatchers.IO) {

                    // Encode the image
                    val swatch = Encoder()
                    return@async swatch.encode(applicationContext, encryptedMessage, imageUri, saveImage)
                }

            coroutineScope.launch(Dispatchers.Main) {
                val maybeUri = newUri.await()

                imageShareProgressBar.visibility = View.INVISIBLE

                if (maybeUri != null)
                {
                    if (saveImage) {
                        showAlert(getString(R.string.alert_text_image_saved))
                    }
                    else {
                        ShareUtil.shareImage(applicationContext, maybeUri)
                    }
                }
                else
                {
                    applicationContext.showAlert(applicationContext.getString(R.string.alert_text_unable_to_process_request))
                }
            }

        } catch (exception: SecurityException) {
            applicationContext.showAlert(applicationContext.getString(R.string.alert_text_unable_to_process_request))
            print("Unable to send message as photo, we were unable to encrypt the mess56age.")
            return
        }
    }

    private fun makeWait() {
        imageShareProgressBar.visibility = View.VISIBLE
        send_as_text_button.isEnabled = false
        send_as_image_button.isClickable = false
        send_as_image_button.isEnabled = false
        send_as_image_button.isClickable = false
        friend_button.isEnabled = false
        friend_button.isClickable = false
    }

    private fun noMoreWaiting() {
        imageShareProgressBar.visibility = View.INVISIBLE
        send_as_text_button.isEnabled = true
        send_as_text_button.isClickable = true
        send_as_image_button.isEnabled = true
        send_as_image_button.isClickable = true
        friend_button.isEnabled = true
        friend_button.isClickable = true
    }

    private fun cleanUp() {
        selectedFriend = null
        editMessageText.text?.clear()
    }


}
