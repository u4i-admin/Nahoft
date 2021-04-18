package org.nahoft.org.nahoft.swatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.nahoft.codex.Encryption
import org.nahoft.stencil.CapturePhotoUtils
import java.nio.ByteBuffer
import org.nahoft.swatch.Swatch
import org.nahoft.swatch.lengthMessageKey

class Encoder {
    @ExperimentalUnsignedTypes
    fun encode(context: Context, encrypted: ByteArray, coverUri: Uri): Uri? {
        // Get the photo
        val cover = BitmapFactory.decodeStream(context.contentResolver.openInputStream(coverUri))
        val result = encode(encrypted, cover)

        val title = ""
        val description = ""
        val resultUri = CapturePhotoUtils.insertImage(context, result, title, description)

        return resultUri
    }

    @ExperimentalUnsignedTypes
    fun encode(encrypted: ByteArray, cover: Bitmap): Bitmap? {
        val messageLength = encrypted.size.toShort() // Length measured in bytes
        val lengthBytes = ByteBuffer.allocate(java.lang.Short.BYTES).putShort(messageLength).array()
        val encryptedLengthBytes = Swatch.polish(lengthBytes, lengthMessageKey)
        val lengthBits = bitsFromBytes(encryptedLengthBytes)
        val lengthBitsSize = lengthBits.size

        // Convert message size from bytes to bits
        // Pad the message bits to be of max size
        val messageBits = bitsFromBytes(encrypted)
        val messageBitsSize = messageBits.size

        // The number of pixels is the image height (in pixels) times the image width (in pixels)
        val numPixels = cover.height * cover.width

        // Do we have enough pixels for the bits we need to encode?
        val lengthPatchSize = numPixels / (lengthBitsSize * 2)
        val messagePatchSize = numPixels / (messageBitsSize * 2)

        if (lengthPatchSize < Swatch.minimumPatchSize) {
            return null
        }
        if (messagePatchSize < Swatch.minimumPatchSize) {
            return null
        }

        val result = cover.copy(Bitmap.Config.ARGB_8888, true)
        return encode(result, lengthBits, messageBits)
    }

    // Takes both messages (length message, and message message) as bits and the bitmap we want to put them in
    @ExperimentalUnsignedTypes
    fun encode(cover: Bitmap, message1: IntArray, message2: IntArray): Bitmap? {
        // Make a set of rules for encoding each message
        // Use different keys so that the patches are different even if the patch sizes are the same
        val rules1 = makeRules(cover, message1, 1)
        val rules2 = makeRules(cover, message2, 2)

        if (rules1 == null) { return null }
        if (rules2 == null) { return null }

        val solver = Solver(cover, rules1, rules2)
        val success = solver.solve()
        if (!success) {
            return null
        }

        return cover
    }

    /// Generates an array of rules.
    /// Each rule returns 2 patches and a constraint (whether or not they are lighter or darker than each other)
    /// Greater is a 1
    /// Less is a 0
    fun makeRules(cover: Bitmap, message: IntArray, key: Int): Array<Rule>?
    {
        val numPixels = cover.height * cover.width

        // Each bit needs a pair of patches
        val patchSize = numPixels / (message.size*2)

        var mapped = MappedBitmap(cover, key)

        var rules: Array<Rule> = arrayOf()

        // For each bit in the message we get a rule
        // A rule is two patches and a constraint
        for (index in message.indices)
        {
            val bit = message[index]

            // Does patchA need to be lighter than patchB, or darker?
            // Brightness is based on the average brightness for the entire patch.
            val constraint: EncoderConstraint? = when (bit) {
                1 -> EncoderConstraint.GREATER
                0 -> EncoderConstraint.LESS
                else -> null
            }

            if (constraint == null) {
                return null
            }

            val rule = Rule(index, patchSize, constraint, mapped)
            rules += rule
        }

        return rules
    }
}