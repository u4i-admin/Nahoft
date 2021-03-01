package org.nahoft.swatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import org.nahoft.stencil.CapturePhotoUtils
import java.nio.ByteBuffer
import kotlin.random.Random

class Swatch {
    val minimumPatchSize = 2

    // Maximum Message Size:
    // 1,000 characters * 4 bytes per character (as a guess) * number of bits in a byte
    val maxMessageSizeBits = 1000 * 4 * 8

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
        // Pad the message bits to be of max size
        var messageBits = bitsFromBytes(encrypted)
        val paddingArray = IntArray(maxMessageSizeBits - messageBits.size)
        messageBits = messageBits + paddingArray
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
        val numPixels = cover.height * cover.width

        val patch1Size = numPixels / message1.size
        val patch2Size = numPixels / message2.size

        // FIXME - Proper seeds
        val rules1 = makeRules(1, cover, message1)
        val rules2 = makeRules(2, cover, message2)

        return null
    }

    /// Generates a set of rules.
    /// Each rule returns 2 patches and whether or not they are lighter or darker that each other
    /// Greater is a 1
    /// Less is a 0
    fun makeRules(key: Int, cover: Bitmap, message: IntArray): Array<Rule>
    {
        // Random number generator
        // Seed is  based on the message so that the same random number generator will be used for encoding/decoding
        // FIXME: Create seed from message
        val random1 = Random(1)

        // Get an array of all of the pixel locations (x,y) and randomize the order using our number generator
        val pixels1 = getPixelArray(cover)
        pixels1.shuffle(random1)

        // Create patches by chunking the array into (message.size * 2) chunks
        // The number of patches should be equal to the number of bits in the message * 2
        val chunks1 = pixels1.asList().chunked(message.size * 2)
        val patches1 = chunks1.map { Patch(it) }

        // Group our patches into pairs
        val pairs1 = patches1.chunked(2)

        // Take each pair and a bit from the message and turn it into a rule (2 patches and a constraint)
        var rules: Array<Rule> = arrayOf()
        for (index in 0 until pairs1.size)
        {
            val bit = message[index]
            val (patch1, patch2) = pairs1[index]
            when (bit)
            {
                1 -> rules += Rule(patch1, patch2, Constraint.GREATER)
                0 -> rules += Rule(patch1, patch2, Constraint.LESS)
            }
        }

        return rules
    }

    fun getPixelArray(bitmap: Bitmap): Array<Pair<Int,Int>>
    {
        var results: Array<Pair<Int,Int>> = arrayOf()

        for (x in 0 until bitmap.width)
        {
            for (y in 0 until bitmap.height)
            {
                results += Pair<Int,Int>(x, y)
            }
        }

        return results
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

class Rule(patch0: Patch, patch1: Patch, constraint: Constraint)
{
}

class Patch(points: List<Pair<Int,Int>>)
{
}

enum class Constraint(val constraint: Int)
{
    GREATER(1),
    EQUAL(-1),
    LESS(0)
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
