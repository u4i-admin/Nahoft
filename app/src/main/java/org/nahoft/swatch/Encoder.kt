package org.nahoft.org.nahoft.swatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import org.nahoft.stencil.CapturePhotoUtils
import org.nahoft.stencil.ImageSize
import org.nahoft.swatch.Swatch
import org.nahoft.swatch.payloadMessageKey
import org.nahoft.util.SaveUtil
import kotlin.math.roundToInt
import kotlin.math.sqrt


class Encoder
{
    @ExperimentalUnsignedTypes
    fun encode(context: Context, encrypted: ByteArray, coverUri: Uri, saveToGallery: Boolean): Uri?
    {
        var inputStream = context.contentResolver.openInputStream(coverUri)
        if (inputStream == null)
        {
            return null
        }

        val exifInterface = ExifInterface(inputStream)
        val orientation: String = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION) ?: ExifInterface.ORIENTATION_UNDEFINED.toString()

        // Refresh inputStream
        inputStream = context.contentResolver.openInputStream(coverUri)

        // Get the photo
        val cover = BitmapFactory.decodeStream(inputStream)
        val result = encode(encrypted, cover)
        val title = ""
        val description = ""

        if (result == null) { return null }

        if (saveToGallery)
        {
            val saved = SaveUtil.saveImageToGallery(context, result)
            if (saved)
            {
                return coverUri
            }
            else
            {
                return null
            }
        }
        else
        {
            return CapturePhotoUtils.insertImage(context, result, title, description)
        }
    }

    @ExperimentalUnsignedTypes
    fun encode(encrypted: ByteArray, cover: Bitmap): Bitmap?
    {
        var workingCover = cover

        // Convert message size from bytes to bits
        // Pad the message bits to be of max size
        val messageBits = bitsFromBytes(encrypted)
        val messageBitsSize = messageBits.size

        // Scale the image if necessary
        workingCover = scale(cover, messageBitsSize)

        // The number of pixels is the image height (in pixels) times the image width (in pixels)
        val numPixels = workingCover.height * workingCover.width

        // Do we have enough pixels for the bits we need to encode?
        val messagePatchSize = numPixels / (messageBitsSize * 2)

        if (messagePatchSize < Swatch.minimumPatchSize) {
            return null
        }

        val result = workingCover.copy(Bitmap.Config.ARGB_8888, true)
        return encode(result, messageBits)
    }

    // Takes both messages (length message, and message message) as bits and the bitmap we want to put them in
    @ExperimentalUnsignedTypes
    fun encode(cover: Bitmap, message: IntArray): Bitmap? {
        val rules = makeRules(cover, message, payloadMessageKey)
        if (rules == null) { return null }

        val solver = Solver(cover, rules)
        val success = solver.solve()
        if (!success) {
            return null
        }

        return cover
    }

    fun scale(bitmap: Bitmap, bits: Int): Bitmap {
        var p = bits * 2 * Swatch.minimumPatchSize
        val size = bitmap.height * bitmap.width
        if (size == p) {
            return bitmap
        } else {
            val originalSize = ImageSize(
                bitmap.height.toDouble(),
                bitmap.width.toDouble(),
                32.0 //ARGB_8888 8 bits for each in ARGB added together
            )

            var scaledSize = resizePreservingAspectRatio(originalSize, p)
            var newHeight = scaledSize.height.roundToInt()
            var newWidth = scaledSize.width.roundToInt()
            var newNumPixels = newHeight * newWidth
            var newBits = newNumPixels / (Swatch.minimumPatchSize * 2)
            while (newBits < bits) {
                print("Error in scaling algorithm.")
                p += 1
                scaledSize = resizePreservingAspectRatio(originalSize, p)
                newHeight = scaledSize.height.roundToInt()
                newWidth = scaledSize.width.roundToInt()
                newNumPixels = newHeight * newWidth
                newBits = newNumPixels / (Swatch.minimumPatchSize * 2)
            }
            val newBitmap = Bitmap.createScaledBitmap(
                bitmap,
                newHeight,
                newWidth,
                true
            )

            return newBitmap
        }
    }

    private fun resizePreservingAspectRatio(originalSize: ImageSize, targetSizePixels: Int): ImageSize {
        val aspectRatio = originalSize.height / originalSize.width
        val scaledHeight = sqrt(targetSizePixels / aspectRatio)
        val scaledWidth = aspectRatio * scaledHeight

        return  ImageSize(scaledHeight, scaledWidth, originalSize.colorDepthBytes)
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