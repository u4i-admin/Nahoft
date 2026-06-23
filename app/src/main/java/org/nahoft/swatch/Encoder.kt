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
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL)

        // Close the EXIF stream and open a fresh one for bitmap decoding.
        // ExifInterface consumes part of the stream, so it can't be reused.
        inputStream = context.contentResolver.openInputStream(coverUri)
        val rawCover = BitmapFactory.decodeStream(inputStream) ?: return null

        // Apply EXIF orientation. Without this, photos taken in portrait mode
        // (stored as landscape pixels + a "rotate 90" EXIF flag)
        // will appear sideways in the saved/shared output.
        val cover = applyExifOrientation(rawCover, orientation)

        val result = encode(encrypted, cover) ?: return null

        val title = ""
        val description = ""

        return if (saveToGallery)
        {
            if (SaveUtil.saveImageToGallery(context, result)) coverUri else null
        }
        else
        {
            CapturePhotoUtils.insertImage(context, result, title, description)
        }
    }

    /**
     * Applies the rotation/flip described by an EXIF orientation tag to a bitmap.
     * Returns the original bitmap unchanged for ORIENTATION_NORMAL or UNDEFINED.
     *
     * The eight EXIF orientation values describe combinations of rotation and
     * mirroring. We handle all of them, though in practice only the four pure
     * rotations (1, 3, 6, 8) appear in camera output.
     */
    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap
    {
        val matrix = android.graphics.Matrix()

        when (orientation)
        {
            ExifInterface.ORIENTATION_NORMAL,
            ExifInterface.ORIENTATION_UNDEFINED -> return bitmap

            ExifInterface.ORIENTATION_ROTATE_90       -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180      -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270      -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL   -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE       -> { matrix.postRotate(90f);  matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE      -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }

            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // createBitmap may return the same instance if the matrix is identity.
        // Only recycle if we got a genuinely new bitmap, otherwise we'd be
        // recycling the bitmap we're about to return.
        if (rotated != bitmap) bitmap.recycle()

        return rotated
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

    /**
     * Returns a bitmap sized appropriately for encoding `bits` bits of payload.
     */
    fun scale(bitmap: Bitmap, bits: Int): Bitmap
    {
        var targetPixels = bits * 2 * Swatch.minimumPatchSize
        val currentPixels = bitmap.height * bitmap.width

        if (currentPixels == targetPixels) { return bitmap }

        val originalSize = ImageSize(
            bitmap.height.toDouble(),
            bitmap.width.toDouble(),
            32.0  // ARGB_8888
        )

        var scaledSize = resizePreservingAspectRatio(originalSize, targetPixels)
        var newHeight = scaledSize.height.roundToInt()
        var newWidth = scaledSize.width.roundToInt()
        var newCapacityBits = (newHeight * newWidth) / (Swatch.minimumPatchSize * 2)

        // Rounding can land us 1-2 pixels short. Nudge up if so.
        while (newCapacityBits < bits)
        {
            targetPixels += 1
            scaledSize = resizePreservingAspectRatio(originalSize, targetPixels)
            newHeight = scaledSize.height.roundToInt()
            newWidth = scaledSize.width.roundToInt()
            newCapacityBits = (newHeight * newWidth) / (Swatch.minimumPatchSize * 2)
        }

        // NOTE: createScaledBitmap signature is (source, dstWidth, dstHeight, filter).
        // The arguments below look swapped because resizePreservingAspectRatio
        // computes aspectRatio = height / width (inverted from convention), so
        // its `height` field actually carries the new width and vice versa.
        // The two inversions cancel and the output has correct aspect ratio.
        return Bitmap.createScaledBitmap(bitmap, newHeight, newWidth, true)
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