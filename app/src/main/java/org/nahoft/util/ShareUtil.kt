package org.nahoft.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.stencil.Stencil


object ShareUtil {

    fun shareImage(context: Context, imageUri: Uri, message: String, encodedFriendPublicKey: ByteArray) {

        // Encrypt the message
        val encryptedMessage = Encryption(context).encrypt(encodedFriendPublicKey, message)

        if (encryptedMessage != null) {
            // Encode the image
            val newUri = Stencil().encode(context, encryptedMessage, imageUri)

            // Save bitmap to image roll to get URI for sharing intent
            if (newUri != null) {

                val sendIntent = Intent(Intent.ACTION_SEND)
                //sendIntent.setClipData(ClipData.newRawUri("", newUri))
                sendIntent.putExtra(Intent.EXTRA_STREAM, newUri)
                sendIntent.type = "image/*"
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val shareIntent = Intent.createChooser(sendIntent, null)
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(shareIntent)
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

            val encodedMessage = codex.encodeEncryptedMessage(encryptedMessage)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, encodedMessage)
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }
    }

    fun shareKey(context: Context, keyBytes: ByteArray) {
        val codex = Codex()
        val encodedKey = codex.encodeKey(keyBytes)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, encodedKey)
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

}