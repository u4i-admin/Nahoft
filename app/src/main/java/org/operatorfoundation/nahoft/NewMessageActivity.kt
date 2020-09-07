package org.operatorfoundation.nahoft

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_new_message.*
import org.operatorfoundation.codex.Codex
import org.operatorfoundation.stencil.Stencil

class NewMessageActivity : AppCompatActivity() {
//EditText message_text_view
//Button send_as_text_button

    val IMAGE_PICK_CODE = 1046
    val FRIEND_PICK_CODE = 1045

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
            val codex = Codex()
            message = codex.encode(message)
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        send_as_image_button.setOnClickListener {
            pickImageFromGallery()
        }
    }

    fun pickImageFromGallery() {
        val pickImageIntent = Intent().apply {
            action = Intent.ACTION_PICK
            type = "image/*"
        }
        startActivityForResult(pickImageIntent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
                if (requestCode == IMAGE_PICK_CODE) {
                    // Get the message text
                    var message = editMessageText.text.toString()
                    // TODO: get data?.data as URI
                    // TODO: Get bitmap from URI and send through Stencil

                    val stencil = Stencil()
                    //stencil.encode(message, plainBitmap)
                    // TODO: Save bitmap received from Stencil to a URI
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        // TODO: This should share the URI created to save the bitmap received from Stencil instead
                        putExtra(Intent.EXTRA_STREAM, data!!.data)
                        type = "image/jpeg"
                    }
                    startActivity(Intent.createChooser(shareIntent, null))
                } else if (requestCode == FRIEND_PICK_CODE) {
                    // Get Friend information from data
                    // Update button title with Friend name
                    println("NewMessageActivity received OK result from friend selection.")

                    val friend = data?.getStringExtra(FriendSelectionActivity.FRIEND_EXTRA_TASK_DESCRIPTION)

                    // If friend is not null use it to set the button title
                    friend?.let {
                        println(friend)
                        friend_button.text = friend
                    }

                }
        }
    }

}
