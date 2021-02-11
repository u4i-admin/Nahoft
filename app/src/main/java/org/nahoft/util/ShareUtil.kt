package org.nahoft.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.R
import org.nahoft.showAlert

object ShareUtil
{
    fun shareImage(context: Context, imageUri: Uri)
    {
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        sendIntent.type = "image/*"
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(shareIntent)
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