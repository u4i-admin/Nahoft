package org.nahoft.swatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.get
import org.nahoft.stencil.CapturePhotoUtils
import java.nio.ByteBuffer

class Swatch {
    val minimumPatchSize = 2

    @ExperimentalUnsignedTypes
    fun encode(context: Context, encrypted: ByteArray, coverUri: Uri): Uri? {
        val cover = BitmapFactory.decodeStream(context.contentResolver.openInputStream(coverUri))
        val result = encode(encrypted, cover)

        val title = ""
        val description = ""
        val resultUri = CapturePhotoUtils.insertImage(context, result, title, description)

        return resultUri
    }

    @ExperimentalUnsignedTypes
    fun encode(encrypted: ByteArray, cover: Bitmap): Bitmap? {
        val messageLength = encrypted.size.toInt() // Length measured in bytes
        val lengthBytes =
            ByteBuffer.allocate(java.lang.Integer.BYTES).putInt(messageLength).array()
        val lengthBits = bitsFromBytes(encrypted)
        val lengthBitsSize = lengthBits.size

        // Convert message size from bytes to bits
        val messageBits = bitsFromBytes(encrypted)
        val messageBitsSize = messageBits.size

        // The number of pixels is the image height (in pixels) times the image width (in pixels)
        val numPixels = cover.height * cover.width

        val lengthPatchSize = numPixels / lengthBitsSize
        val messagePatchSize = numPixels / messageBitsSize

        if (lengthPatchSize < minimumPatchSize) {
            return null
        }
        if (messagePatchSize < minimumPatchSize) {
            return null
        }

        var result = cover.copy(Bitmap.Config.ARGB_8888, true)
        return encode(result, lengthBits, messageBits)
    }

    @ExperimentalUnsignedTypes
    fun encode(cover: Bitmap, message1: IntArray, message2: IntArray): Bitmap? {
        return null
    }

        private fun setPixel(bitmap: Bitmap, x: Int, y: Int, value: Int)
    {
        val color = Color.argb(value, value, value, value)
        bitmap.setPixel(x, y, color)
    }

    fun decode(context: Context, uri: Uri): ByteArray?
    {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))

//        val bitmap = ImageDecoder.decodeBitmap(
//            ImageDecoder.createSource(
//                context.contentResolver,
//                uri
//            )
//        )

        return decode(bitmap)
    }

    fun decode(bitmap: Bitmap): ByteArray?
    {
        return null
    }
}

@ExperimentalUnsignedTypes
fun bitsFromBytes(bytes: ByteArray): IntArray
{
    var result: IntArray = IntArray(bytes.size * 8)

    for (byteIndex in 0 until bytes.size)
    {
        val byte = bytes[byteIndex].toUByte()

        for (bitIndex in 0 until 8)
        {
            val arrayIndex = byteIndex * 8 + bitIndex
            val bit = byte and masks[bitIndex]

            if (bit == 0.toUByte())
            {
                result[arrayIndex] = 0
            }
            else
            {
                result[arrayIndex] = 1
            }
        }
    }

    return result
}

fun bytesFromBits(bits: List<Int>): ByteArray?
{
    if (bits.size % 8 != 0)
    {
        return null
    }

    var result = ByteArray(bits.size / 8)

    for (byteIndex in 0 until result.size)
    {
        var byte: UByte = 0.toUByte()

        for (bitIndex in 0 until 8)
        {
            val arrayIndex = (byteIndex * 8) + bitIndex
            val value = bits[arrayIndex]
            if (value == 0)
            {
                continue
            }
            else if (value == 1)
            {
                val maskValue: UByte = masks[bitIndex]
                byte = (byte + maskValue).toUByte()
            }
            else
            {
                return null
            }
        }

        result[byteIndex] = byte.toByte()
    }

    return result
}

@ExperimentalUnsignedTypes
val masks: List<UByte> = listOf(
    128.toUByte(),
    64.toUByte(),
    32.toUByte(),
    16.toUByte(),
    8.toUByte(),
    4.toUByte(),
    2.toUByte(),
    1.toUByte()
)
