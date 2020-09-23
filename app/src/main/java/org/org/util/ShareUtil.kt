package org.org.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.org.codex.Codex
import org.org.codex.Encryption
import org.org.stencil.CapturePhotoUtils
import org.org.stencil.Stencil


object ShareUtil {

    fun shareImage(context: Context, imageUri: Uri, message: String, encodedFriendPublicKey: ByteArray) {

        // Encrypt the message
        val encryptedMessage = Encryption(context).encrypt(encodedFriendPublicKey, message)

        if (encryptedMessage != null) {
            // Encode the image
            val newUri = Stencil().encode(context, encryptedMessage, imageUri)

            // Save bitmap to image roll to get URI for sharing intent
            if (newUri != null) {

                // Sharing requires a custom intent whose action must be Intent.ACTION_SEND
                val intent = Intent(Intent.ACTION_SEND).apply {

                    type = "image/jpeg"

                    // Resolves the images local URL and adds it to the intent as Intent.EXTRA_STREAM
                    putExtra(Intent.EXTRA_STREAM, newUri)

                    // You need this flag to let the intent read local data and stream the image content to another app.
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                context.startActivity(Intent.createChooser(intent, null))
            } else {
                print("Unable to send message as photo, we were unable to encode the selected image.")
                return
            }
        } else {
            // TODO: Toast
            print("Unable to send message as photo, we were unable to encrypt the message.")
            return
        }

    }

    fun shareText(context: Context, message: String, encodedFriendPublicKey: ByteArray) {

        val codex = Codex()
        val encryptedMessage = Encryption(context).encrypt(encodedFriendPublicKey, message)

        encryptedMessage?.let {

            val encodedMessage = codex.encode(encryptedMessage)
            val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, encodedMessage)
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }

    }

}