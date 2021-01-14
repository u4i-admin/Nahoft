package org.nahoft.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.*
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.R
import org.nahoft.showAlert
import org.nahoft.stencil.Stencil


object ShareUtil
{
    val parentJob = Job()
    val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    fun shareImage(context: Context, imageUri: Uri, message: String, encodedFriendPublicKey: ByteArray)
    {
        try {
            // Encrypt the message
            val encryptedMessage = Encryption(context).encrypt(encodedFriendPublicKey, message)

            // Encode the image
            val newUri: Deferred<Uri?> =
                coroutineScope.async(Dispatchers.IO) {
                    return@async Stencil().encode(context, encryptedMessage, imageUri)
                }

            coroutineScope.launch(Dispatchers.Main) {
                val maybeUri = newUri.await()
                // Save bitmap to image roll to get URI for sharing intent
                if (maybeUri != null) {

                    val sendIntent = Intent(Intent.ACTION_SEND)
                    //sendIntent.setClipData(ClipData.newRawUri("", newUri))
                    sendIntent.putExtra(Intent.EXTRA_STREAM, maybeUri)
                    sendIntent.type = "image/*"
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    val shareIntent = Intent.createChooser(sendIntent, null)
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    //likely need to end animation here

                    context.startActivity(shareIntent)
                } else {
                    context.showAlert(context.getString(R.string.alert_text_unable_to_process_request))
                    print("Unable to send message as photo, we were unable to encode the selected image.")
                }
            }
        } catch (exception: SecurityException) {
            context.showAlert(context.getString(R.string.alert_text_unable_to_process_request))
            print("Unable to send message as photo, we were unable to encrypt the mess56age.")
            return
        }
    }

    fun shareText(context: Context, message: String, encodedFriendPublicKey: ByteArray)
    {
        val codex = Codex()
        try {
            val encryptedMessage = Encryption(context).encrypt(encodedFriendPublicKey, message)
            val encodedMessage = codex.encodeEncryptedMessage(encryptedMessage)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, encodedMessage)
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        } catch (exception: SecurityException) {
            context.showAlert(context.getString(R.string.alert_text_unable_to_process_request))
            print("We were unable to encrypt the message.")
            return
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