package org.org.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.Image
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import org.org.codex.Codex
import org.org.codex.Encryption
import org.org.stencil.Stencil
import java.security.PublicKey


object ShareUtil {

    fun shareImage(context: Context, imageUri: Uri, message: String, encodedFriendPublicKey: ByteArray) {

        // Encrypt the message
        // TODO: Use encrypted message in stencil
        val encryptedMessage = Encryption.encrypt(encodedFriendPublicKey, message)

        // Encode the image
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
        val newBitmap = Stencil().encode(message, bitmap)

        // Sharing requires a custom intent whose action must be Intent.ACTION_SEND
        val intent = Intent(Intent.ACTION_SEND).apply {

            // MIME type is always image/jpeg because the repo saves all the memes locally as JPEGs.
            type = "image/jpeg"

            // TODO: Share the new bitmap instead
            // Resolves the meme's local URL and adds it to the intent as Intent.EXTRA_STREAM
            putExtra(Intent.EXTRA_STREAM, imageUri)

            // You need this flag to let the intent read local data and stream the image content to another app.
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        // Starts the share sheet.
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun shareText(context: Context, message: String, encodedFriendPublicKey: ByteArray) {

        val codex = Codex()
        val encryptedMessage = Encryption.encrypt(encodedFriendPublicKey, message)

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