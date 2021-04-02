package org.nahoft.swatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.set
import org.nahoft.codex.Encryption
import org.nahoft.org.nahoft.swatch.Solver
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
        // Get the photo
        val cover = BitmapFactory.decodeStream(context.contentResolver.openInputStream(coverUri))
        val result = encode(context, encrypted, cover)

        val title = ""
        val description = ""
        val resultUri = CapturePhotoUtils.insertImage(context, result, title, description)

        return resultUri
    }

    @ExperimentalUnsignedTypes
    fun encode(context: Context, encrypted: ByteArray, cover: Bitmap): Bitmap? {
        val messageLength = encrypted.size.toInt() // Length measured in bytes
        val lengthBytes =
            ByteBuffer.allocate(java.lang.Integer.BYTES).putInt(messageLength).array()
        val encryptedLengthBytes = Encryption(context).encryptLengthData(lengthBytes)
        val lengthBits = bitsFromBytes(encryptedLengthBytes)
        val lengthBitsSize = lengthBits.size

        // Convert message size from bytes to bits
        // Pad the message bits to be of max size
        var messageBits = bitsFromBytes(encrypted)
        val messageBitsSize = messageBits.size

        // The number of pixels is the image height (in pixels) times the image width (in pixels)
        val numPixels = cover.height * cover.width

        // Do we have enough pixels for the bits we need to encode?
        val lengthPatchSize = numPixels / (lengthBitsSize*2)
        val messagePatchSize = numPixels / (messageBitsSize*2)

        if (lengthPatchSize < minimumPatchSize) {
            return null
        }
        if (messagePatchSize < minimumPatchSize) {
            return null
        }

        var result = cover.copy(Bitmap.Config.ARGB_8888, true)
        return encode(result, lengthBits, messageBits)
    }

    // Takes both messages (length message, and message message) as bits and the bitmap we want to put them in
    @ExperimentalUnsignedTypes
    fun encode(cover: Bitmap, message1: IntArray, message2: IntArray): Bitmap? {
        // Make a set of rules for encoding each message
        // Use different keys so that the patches are different
        val rules1 = makeRules(1, cover, message1)
        val rules2 = makeRules(2, cover, message2)

        val solver = Solver(cover, rules1, rules2)
        val solution = solver.solve()

        return null
    }

    /// Generates an array of rules.
    /// Each rule returns 2 patches and a constraint (whether or not they are lighter or darker that each other)
    /// Greater is a 1
    /// Less is a 0
    fun makeRules(key: Int, cover: Bitmap, message: IntArray): Array<Rule>
    {
        // Random number generator
        // Seed is  based on the message so that the same random number generator will be used for encoding/decoding
        val random1 = Random(key)

        val numPixels = cover.height * cover.width
        // Each bit needs a pair of patches
        val patchSize = numPixels / (message.size*2)

        var rules: Array<Rule> = arrayOf()

        // For each bit in the message we get a rule
        // A rule is two patches and a constraint
        for (index in message.indices)
        {
            val bit = message[index]

            // Does patchA need to be lighter than patchB, or darker?
            // Brightness is based on the average brightness for the entire patch.
            var constraint: Constraint? = null
            when (bit)
            {
                1 -> constraint = Constraint.GREATER
                0 -> constraint = Constraint.LESS
            }

            // Create pairs
            // index*2, index*2+1 = 0,1 - 2,3 - 4,5 - etc.
            val patchA = Patch(index*2, patchSize)
            val patchB = Patch(index*2+1, patchSize)

            val rule = Rule(patchA, patchB, constraint!!)
            rules += rule
        }

        return rules
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

        return decode(context, bitmap)
    }

    fun decode(context: Context, bitmap: Bitmap): ByteArray?
    {
        val lengthBitsSize = java.lang.Integer.BYTES * 8
        val lengthBitsKey = 1 // FIXME - use proper keys
        val lengthBits = decode(bitmap, lengthBitsKey, lengthBitsSize)

        if (lengthBits == null) { return null }
        lengthBits?.let {
            val encryptedLengthBytes = bytesFromBits(lengthBits)
            encryptedLengthBytes?.let {
                val lengthBytes = Encryption(context).decryptLengthData(encryptedLengthBytes)
                val length = ByteBuffer.wrap(lengthBytes).getInt()
                val messageKey = 2 // FIXME - use proper keys
                val messageBits = decode(bitmap, messageKey, length)
                if (messageBits == null) { return null }
                return bytesFromBits(messageBits)
            }
        }

        return null
    }

    fun decode(bitmap: Bitmap, key: Int, size: Int): List<Int>?
    {
        val numPixels = bitmap.height * bitmap.width

        val patchSize = numPixels / (size*2)

        if (patchSize < minimumPatchSize) {
            return null
        }

        // Random number generator
        // Seed is  based on the message so that the same random number generator will be used for encoding/decoding
        // FIXME: Create seed from message
        val random = Random(key)

        var message: IntArray = IntArray(size)

        var rules: Array<Rule> = arrayOf()
        var pixelArrayIndex = 0
        for (index in message.indices)
        {
            val rule = Rule(Patch(index*2, patchSize), Patch(index*2+1, patchSize), bitmap)
            when (rule.constraint)
            {
                Constraint.GREATER -> message[index] = 1
                Constraint.LESS -> message[index] = 0
            }
        }

        return message.toList()
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

class Pixel(val index: Int)
{
    fun toX(bitmap: Bitmap): Int
    {
        return index % bitmap.width
    }

    fun toY(bitmap: Bitmap): Int
    {
        return index / bitmap.width
    }

    fun brightness(bitmap: Bitmap): Float
    {
        val y = toY(bitmap)
        val x = toX(bitmap)
        var hsv = FloatArray(3)
        val color = bitmap.getPixel(x, y)
        Color.colorToHSV(color, hsv)
        return hsv[2] // V
    }

    fun brighten(bitmap: Bitmap): Int
    {
        val y = toY(bitmap)
        val x = toX(bitmap)
        val colorInt = bitmap.getPixel(x, y)
        val color = Color.valueOf(colorInt)
        val a = color.alpha()
        val r = color.red()
        val g = color.green()
        val b = color.blue()
        val newColor = Color.argb(a, r + 1, g + 1, b + 1)
        return newColor
    }

    fun darken(bitmap: Bitmap): Int
    {
        val y = toY(bitmap)
        val x = toX(bitmap)
        val colorInt = bitmap.getPixel(x, y)
        val color = Color.valueOf(colorInt)
        val a = color.alpha()
        val r = color.red()
        val g = color.green()
        val b = color.blue()
        val newColor = Color.argb(a, r - 1, g - 1, b - 1)
        return newColor
    }
}

class Rule(var patch0: Patch, var patch1: Patch, var constraint: Constraint)
{
    constructor(patch0: Patch, patch1: Patch, bitmap: Bitmap): this(patch0, patch1, Constraint.EQUAL)
    {
        val b0 = patch0.brightness(bitmap)
        val b1 = patch1.brightness(bitmap)

        if (b0 > b1)
        {
            constraint = Constraint.GREATER
        }
        else if (b0 < b1)
        {
            constraint = Constraint.LESS
        }
        else
        {
            constraint = Constraint.EQUAL
        }
    }

    // Does this pair of patches meet the constraint
    fun validate(bitmap: Bitmap): Boolean
    {
        val b0 = patch0.brightness(bitmap)
        val b1 = patch1.brightness(bitmap)

        when (constraint)
        {
            Constraint.GREATER -> return b0 > b1
            Constraint.EQUAL -> return b0 == b1
            Constraint.LESS -> return b0 < b1
        }
    }

    fun constrain(bitmap: Bitmap, directions: Bitmap): Bitmap
    {
        var workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // If the constraint is not satisfied
        // Decide which patch to alter randomly
        // And then alter one pixel in that patch according to the constraint
        while (!validate(workingBitmap))
        {
            if (Random.nextBoolean())
            {
                when (constraint)
                {
                    Constraint.GREATER -> workingBitmap = patch0.brighten(workingBitmap, directions)
                    Constraint.LESS -> workingBitmap = patch0.darken(workingBitmap, directions)
                    Constraint.EQUAL -> workingBitmap = balanceBrighten(workingBitmap, directions)
                }
            }
            else
            {
                when (constraint)
                {
                    Constraint.GREATER -> workingBitmap = patch1.darken(workingBitmap, directions)
                    Constraint.LESS -> workingBitmap = patch1.brighten(workingBitmap, directions)
                    Constraint.EQUAL -> workingBitmap = balanceDarken(workingBitmap, directions)
                }
            }
        }

        return workingBitmap
    }

    fun balanceBrighten(bitmap: Bitmap, directions: Bitmap): Bitmap
    {
        if (patch0.brightness(bitmap) > patch1.brightness(bitmap))
        {
            return patch1.brighten(bitmap, directions)
        }
        else
        {
            return patch0.brighten(bitmap, directions)
        }
    }

    fun balanceDarken(bitmap: Bitmap, directions: Bitmap): Bitmap
    {
        if (patch0.brightness(bitmap) > patch1.brightness(bitmap))
        {
            return patch0.darken(bitmap, directions)
        }
        else
        {
            return patch1.darken(bitmap, directions)
        }
    }
}

class Patch(val patchIndex: Int, val size: Int)
{
    var points = IntArray(size).mapIndexed {index, value -> Pixel(patchIndex*size + index)}

    fun brightness(bitmap: Bitmap): Float
    {
        val values = points.map { pixel -> pixel.brightness(bitmap) }
        return values.sum()
    }

    // Picks a random pixel from the patch changes the color of the pixel to be brighter
    fun brighten(bitmap: Bitmap, directions: Bitmap): Bitmap
    {
        var workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        var pointsCopy = points.toMutableList()
        pointsCopy.shuffle()

        var stillWorking = true
        while (stillWorking && pointsCopy.isNotEmpty())
        {
            val point = pointsCopy.removeFirst()

            val y = point.toY(bitmap)
            val x = point.toX(bitmap)

            val direction = directions.getPixel(x, y)
            if (direction == 2)
            {
                val newValue = point.brighten(workingBitmap)
                workingBitmap.set(x, y, newValue)
                stillWorking = false
            }
        }

        if (stillWorking && pointsCopy.isEmpty())
        {
            print("Failure to modify image")
        }

        return workingBitmap
    }

    // Picks a random pixel from the patch changes the color of the pixel to be darker
    fun darken(bitmap: Bitmap, directions: Bitmap): Bitmap
    {
        var workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        var pointsCopy = points.toMutableList()
        pointsCopy.shuffle()

        var stillWorking = true
        while (stillWorking)
        {
            val point = pointsCopy.removeFirst()

            val y = point.toY(bitmap)
            val x = point.toX(bitmap)
            val direction = directions.getPixel(x, y)
            if (direction == -2)
            {
                val newValue = point.darken(workingBitmap)
                workingBitmap.set(x, y, newValue)
                stillWorking = false
            }
        }

        return workingBitmap
    }
}

enum class Constraint(val constraint: Int)
{
    GREATER(1),
    EQUAL(0),
    LESS(-1)
}

class SetPixel(pixel: Pixel, value: Int)
{
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
