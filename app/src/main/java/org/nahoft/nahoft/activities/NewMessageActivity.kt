package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.coroutines.*
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.R
import org.nahoft.showAlert
import org.nahoft.util.RequestCodes
import org.nahoft.util.ShareUtil

class NewMessageActivity : AppCompatActivity() {

    private var selectedFriend: Friend? = null

    // Coroutines
    private val coroutineExceptionHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        coroutineScope.launch(Dispatchers.Main) {
            composeErrorMessage.visibility = View.VISIBLE
            composeErrorMessage.text = getString(R.string.error_loading_image_message)
        }

        GlobalScope.launch { println("Caught $throwable") }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob + coroutineExceptionHandler)

    companion object {
        private val parentJob = Job()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        // Select Friend Button
        friend_button.setOnClickListener {
            selectFriend()
        }

        // Send message as text button
        send_as_text_button.setOnClickListener {
            val message = editMessageText.text.toString()
            sendAsText(message)

        }

        // Send message as image button
        send_as_image_button.setOnClickListener {
            pickImageFromGallery()
        }
    }

    private fun selectFriend() {
        val intent = FriendSelectionActivity.newIntent(this@NewMessageActivity)
        Intent(this, FriendSelectionActivity::class.java)
        startActivityForResult(intent, RequestCodes.selectFriendCode)
    }

    private fun pickImageFromGallery() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickImageIntent, RequestCodes.selectImageCode)
    }

    private fun sendAsText(message: String) {
        if (selectedFriend != null) {
            if (selectedFriend!!.publicKeyEncoded != null) {
                // Share this message as a text
                ShareUtil.shareText(this, message, selectedFriend!!.publicKeyEncoded!!)
                editMessageText.text.clear()
            } else {
                this.showAlert(getString(R.string.alert_text_verified_friends_only))
            }
        } else {
            // TODO: We may want to simply disable the send buttons until a friend is selected
            this.showAlert(getString(R.string.alert_text_select_friend_to_send_message))
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RequestCodes.selectImageCode) {

                // We can only share an image if a recipient with a public key has been selected
                selectedFriend?.let {
                    it.publicKeyEncoded?.let {

                        // Get the message text
                        val message = editMessageText.text.toString()

                        // get data?.data as URI
                        val imageURI = data?.data

                        imageURI?.let {
                            ShareUtil.shareImage(applicationContext, imageURI, message, selectedFriend!!.publicKeyEncoded!!)
                            editMessageText.text.clear()
                        }
                    }

                }

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
