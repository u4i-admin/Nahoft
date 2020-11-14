package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_new_message.*
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.R
import org.nahoft.showAlert
import org.nahoft.util.RequestCodes
import org.nahoft.util.ShareUtil

class NewMessageActivity : AppCompatActivity() {

    private var selectedFriend: Friend? = null

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

        if (message.length > 1000)
        {
            showAlert(getString(R.string.alert_text_message_too_long))
        }

        // Make sure there is a friend to create the message for.
        if (selectedFriend == null) {
            this.showAlert(getString(R.string.alert_text_select_friend_to_send_message))
            return
        }

        if (isImage == true) {
            // If the message is sent as an image
            pickImageFromGallery()
        } else {
            // If the message is sent as text
            if (selectedFriend!!.publicKeyEncoded != null) {
                // Share this message as a text
                ShareUtil.shareText(this, message, selectedFriend!!.publicKeyEncoded!!)
                editMessageText.text?.clear()
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

                        imageURI?.let {
                            ShareUtil.shareImage(applicationContext, imageURI, message, selectedFriend!!.publicKeyEncoded!!)
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

//    private fun shareAsImage(imageURI: Uri, message: String, publicKeyEncoded: ByteArray) {
//        coroutineScope.launch(Dispatchers.Main) {
//            shareAsImageAsync(imageURI, message, publicKeyEncoded)
//        }
//    }
//
//    private suspend fun shareAsImageAsync(imageURI: Uri, message: String, publicKeyEncoded: ByteArray) = withContext(Dispatchers.Default) {
//
//
//    }


}
