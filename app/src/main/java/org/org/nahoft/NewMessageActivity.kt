package org.org.nahoft

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
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
        println("**New Messages Activity")
        setContentView(R.layout.activity_new_message)

        friend_button.setOnClickListener {
            println("friend button clicked")
            
            val intent = FriendSelectionActivity.newIntent(this@NewMessageActivity)
                Intent(this, FriendSelectionActivity::class.java)
            startActivityForResult(intent, FRIEND_PICK_CODE)
        }

        send_as_text_button.setOnClickListener {

            var message = editMessageText.text.toString()

            if (selectedFriend != null) {
                if (selectedFriend!!.publicKeyEncoded != null) {
                    ShareUtil.shareText(this, message, selectedFriend!!.publicKeyEncoded!!)
                } else {
                    // TODO: create a toast to tell user they can only send a message to a verified friend.
                    print("Unable to send message as text, we do not have the recipient's public key.")
                }
            } else {
                // TODO: create a toast to tell user they must select a friend to send a message
                // TODO: We may want to simply disable the send buttons until a friend is selected
                print("Unable to send a message as text, the recipient has not been selected.")
            }
        }

        send_as_image_button.setOnClickListener {
            pickImageFromGallery()
        }
    }

    fun pickImageFromGallery() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        if (pickImageIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pickImageIntent, IMAGE_PICK_CODE)
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

                        // TODO: get data?.data as URI
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
