package org.org.nahoft

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_new_message.*
import org.org.codex.Codex
import org.org.stencil.Stencil
import org.org.util.ShareUtil

class NewMessageActivity : AppCompatActivity() {
//EditText message_text_view
//Button send_as_text_button

    val IMAGE_PICK_CODE = 1046
    val FRIEND_PICK_CODE = 1045

    var selectedFriend: Friend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        // Select Friend Button
        friend_button.setOnClickListener {
            selectFriend()
        }

        // Send message as text button
        send_as_text_button.setOnClickListener {
            var message = editMessageText.text.toString()
            sendAsText(message)

        }

        // Send message as image button
        send_as_image_button.setOnClickListener {
            pickImageFromGallery()
        }

        when {
            intent?.action == Intent.ACTION_SEND -> {
                // Received shared data
                Toast.makeText(this, "Received shared data.", Toast.LENGTH_SHORT).show()
                if ("text/plain" == intent.type) {
                    handleSharedText(intent)
                } else if (intent.type?.startsWith("image/") == true) {
                    handleSharedImage(intent)
                }
            }
        }
    }

    fun selectFriend() {
        val intent = FriendSelectionActivity.newIntent(this@NewMessageActivity)
        Intent(this, FriendSelectionActivity::class.java)
        startActivityForResult(intent, FRIEND_PICK_CODE)
    }

    fun pickImageFromGallery() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        if (pickImageIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pickImageIntent, IMAGE_PICK_CODE)
        }
    }

    fun sendAsText(message: String) {
        if (selectedFriend != null) {
            if (selectedFriend!!.publicKeyEncoded != null) {
                // Share this message as a text
                ShareUtil.shareText(this, message, selectedFriend!!.publicKeyEncoded!!)
            } else {
                Toast.makeText(this, getString(R.string.toastTextCanOnlyMessageAVerifiedFriend), Toast.LENGTH_SHORT).show()
                print("Unable to send message as text, we do not have the recipient's public key.")
            }
        } else {
            Toast.makeText(this, getString(R.string.toastTextMustSelectAFriendToSendAMessage), Toast.LENGTH_SHORT).show()
            // TODO: We may want to simply disable the send buttons until a friend is selected
            print("Unable to send a message as text, the recipient has not been selected.")
        }
    }

    fun handleSharedText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            // Update UI to reflect text being shared
            Toast.makeText(this, "Received shared text $it.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSharedImage(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            // Update UI to reflect image being shared
            Toast.makeText(this, "Received shared image. $it", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_CODE) {

                // We can only share an image if a recipient with a public key has been selected
                selectedFriend?.let {
                    it.publicKeyEncoded?.let {

                        // Get the message text
                        var message = editMessageText.text.toString()

                        // get data?.data as URI
                        val imageURI = data?.data as? Uri

                        imageURI?.let {
                            ShareUtil.shareImage(this, imageURI, message, selectedFriend!!.publicKeyEncoded!!)
                        }
                    }

                }

            } else if (requestCode == FRIEND_PICK_CODE) {

                // Get Friend information from data
                val friend = data?.getSerializableExtra(FriendSelectionActivity.FRIEND_EXTRA_TASK_DESCRIPTION) as? Friend

                // If friend is not null use it to set the button title and set selectedFriend
                friend?.let {
                    this.selectedFriend = friend
                    friend_button.text = friend.name
                }
            }
        }
    }


}
