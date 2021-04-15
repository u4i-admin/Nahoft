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

val lengthMessageSeed = 1
val payloadMessageSeed = 2

class Swatch {
    val minimumPatchSize = 2

    // Maximum Message Size:
    // 1,000 characters * 4 bytes per character (as a guess) * number of bits in a byte
    val maxMessageSizeBits = 1000 * 4 * 8

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
        val messageLength = encrypted.size.toInt() // Length measured in bytes
        val lengthBytes =
            ByteBuffer.allocate(java.lang.Integer.BYTES).putInt(messageLength).array()
        val encryptedLengthBytes = Encryption().encryptLengthData(lengthBytes)
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

        if (lengthPatchSize < minimumPatchSize) {
            return null
        }
        if (messagePatchSize < minimumPatchSize) {
            return null
        }

        val result = cover.copy(Bitmap.Config.ARGB_8888, true)
        return encode(result, lengthBits, messageBits)
    }

    // Takes both messages (length message, and message message) as bits and the bitmap we want to put them in
    @ExperimentalUnsignedTypes
    fun encode(cover: Bitmap, message1: IntArray, message2: IntArray): Bitmap? {
        // Make a set of rules for encoding each message
        // Use different keys so that the patches are different
        val rules1 = makeRules(cover, message1)
        val rules2 = makeRules(cover, message2)

        val solver = Solver(cover, rules1, rules2)
        val solution = solver.solve()

        return solution
    }

    /// Generates an array of rules.
    /// Each rule returns 2 patches and a constraint (whether or not they are lighter or darker than each other)
    /// Greater is a 1
    /// Less is a 0
    fun makeRules(cover: Bitmap, message: IntArray): Array<Rule>
    {
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
            // index*2, index*2+1 = 0,1 -> 2,3 -> 4,5 -> etc.
            val patchA = Patch(index * 2, patchSize)
            val patchB = Patch(index * 2 + 1, patchSize)

            val rule = Rule(patchA, patchB, constraint!!)
            rules += rule
        }

        return rules
    }

    fun decode(context: Context, uri: Uri): ByteArray?
    {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))

        return decode(bitmap)
    }

    fun decode(bitmap: Bitmap): ByteArray?
    {
        val lengthBitsSize = java.lang.Integer.BYTES * 8
        val lengthBits = decode(bitmap, lengthBitsSize, lengthMessageSeed)

        if (lengthBits == null) { return null }
        lengthBits?.let {
            val encryptedLengthBytes = bytesFromBits(lengthBits)
            encryptedLengthBytes?.let {
                val lengthBytes = Encryption().decryptLengthData(encryptedLengthBytes)
                val length = ByteBuffer.wrap(lengthBytes).getInt()
                val messageBits = decode(bitmap, length, payloadMessageSeed)
                if (messageBits == null)
                { return null }
                return bytesFromBits(messageBits)
            }
        }

        return null
    }

    fun decode(bitmap: Bitmap, size: Int, messageSeed: Int): List<Int>?
    {
        val numberOfPixels = bitmap.height * bitmap.width
        val patchSize = numberOfPixels / (size*2)

        if (patchSize < minimumPatchSize)
        { return null }

        // Randomly map the pixels
        // Make an array of integers 0 - numberOfPixels and shuffle it
        val random = Random(messageSeed)
        var pixelList = IntArray(numberOfPixels) { it }
        pixelList.shuffle(random)

        var message = IntArray(size)

        for (index in message.indices)
        {
            val rule = Rule(
                Patch(index * 2, patchSize),
                Patch(index * 2 + 1, patchSize),
                bitmap,
                pixelList
            )
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

@ExperimentalUnsignedTypes
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
    fun toX(bitmap: Bitmap, pixelList: IntArray): Int
    {
        val mappedIndex = pixelList[index]
        val mappedPixel = Pixel(mappedIndex)
        return mappedPixel.toX(bitmap)
    }

    fun toX(bitmap: Bitmap): Int
    {
        return index % bitmap.width
    }

    fun toY(bitmap: Bitmap, pixelList: IntArray): Int
    {
        val mappedIndex = pixelList[index]
        val mappedPixel = Pixel(mappedIndex)
        return mappedPixel.toY(bitmap)
    }

    fun toY(bitmap: Bitmap): Int
    {
        return index / bitmap.width
    }

    fun brightness(bitmap: Bitmap, pixelList: IntArray): Float
    {
        val y = toY(bitmap, pixelList)
        val x = toX(bitmap, pixelList)
        var hsv = FloatArray(3)
        val color = bitmap.getPixel(x, y)
        Color.colorToHSV(color, hsv)
        return hsv[2] // V
    }

    fun brighten(bitmap: Bitmap, pixelList: IntArray): Int
    {
        val y = toY(bitmap, pixelList)
        val x = toX(bitmap, pixelList)
        val colorInt = bitmap.getPixel(x, y)
        val color = Color.valueOf(colorInt)
        val a: Int = colorInt.shr(24) and 0xff // or color >>> 24

        var r: Int = colorInt.shr(16) and 0xff
        if (r <= 250) {
            r += 5
        } else {
            r = 255
        }

        var g: Int = colorInt.shr( 8 ) and 0xff
        if (g <= 250) {
            g += 5
        } else {
            g = 255
        }

        var b: Int = colorInt and 0xff
        if (b <= 250) {
            b += 5
        } else {
            b = 255
        }

        val newColor = Color.argb(a, r, g, b)
        return newColor
    }

    fun darken(bitmap: Bitmap, pixelList: IntArray): Int
    {
        val y = toY(bitmap, pixelList)
        val x = toX(bitmap, pixelList)
        val colorInt = bitmap.getPixel(x, y)
//        val color = Color.valueOf(colorInt)
//        val a = color.alpha()
//        val r = color.red()
//        val g = color.green()
//        val b = color.blue()

        val a: Int = colorInt.shr(24) and 0xff // or color >>> 24
        var r: Int = colorInt.shr(16) and 0xff
        if (r > 5) {
            r -= 5
        } else {
            r = 0
        }
        var g: Int = colorInt.shr( 8 ) and 0xff
        if (g > 5) {
            g -= 5
        } else {
            g = 0
        }
        var b: Int = colorInt and 0xff
        if (b > 5) {
            b -= 5
        } else {
            b = 0
        }

        val newColor = Color.argb(a, r, g, b)
        return newColor
    }
}

class Rule(var patch0: Patch, var patch1: Patch, var constraint: Constraint)
{
    constructor(patch0: Patch, patch1: Patch, bitmap: Bitmap, pixelList: IntArray): this(
        patch0,
        patch1,
        Constraint.EQUAL
    )
    {
        val b0 = patch0.brightness(bitmap, pixelList)
        val b1 = patch1.brightness(bitmap, pixelList)

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
    fun validate(bitmap: Bitmap, pixelList: IntArray): Boolean
    {
        val b0 = patch0.brightness(bitmap, pixelList)
        val b1 = patch1.brightness(bitmap, pixelList)

        when (constraint)
        {
            Constraint.GREATER -> return b0 > b1
            Constraint.EQUAL -> return b0 == b1
            Constraint.LESS -> return b0 < b1
        }
    }

    fun removeConflictedPixels(directions: Bitmap, pixelList: IntArray): Boolean {

        val patch0NotConflicts = removeConflictedPixelsForPatch(patch0, directions, pixelList)
        if (patch0NotConflicts.isEmpty())
        {
            print("Error removing conflicts from a patch, all points were in conflict.")
            return false
        }
        patch0.points = patch0NotConflicts

        val patch1NotConflicts = removeConflictedPixelsForPatch(patch1, directions, pixelList)
        if (patch1NotConflicts.isEmpty())
        {
            print("Error removing conflicts from a patch, all points were in conflict.")
            return false
        }
        patch1.points = patch1NotConflicts

        return true
    }

    fun removeConflictedPixelsForPatch(patch: Patch, directions: Bitmap, pixelList: IntArray): List<Pixel>
    {
        var notConflictsList = patch.points.toMutableList()

        for (point in patch.points) {
            val x = point.toX(directions, pixelList)
            val y = point.toY(directions, pixelList)
            val pixelColor = directions.getPixel(x, y)

            // If there is a conflict, remove it from our list
            if (pixelColor == Color.RED) {
                notConflictsList.remove(point)
            }
        }

        return notConflictsList
    }

    fun constrain(bitmap: Bitmap, directions: Bitmap, pixelList: IntArray): Bitmap
    {
        var workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // If the working bitmap is already valid return it
        if (validate(workingBitmap, pixelList))
        { return workingBitmap }

        // If there is an error removing conflicted pixels, just return the working bitmap
        // FIXME: This should return null (function should return optional bitmap)
        if (!removeConflictedPixels(directions, pixelList))
        { return workingBitmap }

        // If the constraint is not satisfied
        // Decide which patch to alter randomly
        // And then alter one pixel in that patch according to the constraint
        // TODO: Refactor modifyBrightness so that we don't need this if/else (It should brighten and darken, not do one or the other)
        workingBitmap = modifyBrightness(workingBitmap, directions, pixelList)


        return workingBitmap
    }


    fun modifyBrightness(bitmap: Bitmap, directions: Bitmap, pixelList: IntArray): Bitmap
    {
        val workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        // TODO: Refactor to remove code duplication in this if/else
        if (Random.nextBoolean())
        {
            while (!validate(workingBitmap, pixelList)) {
                for (point in patch0.points) {
                    val y = point.toY(workingBitmap, pixelList)
                    val x = point.toX(workingBitmap, pixelList)
                    val direction = directions.getPixel(x, y)

                    // Picks a random pixel from the patch changes the color of the pixel to be darker
                    val newValue = when (direction) {
                        Color.BLUE -> point.darken(workingBitmap, pixelList)
                        Color.GREEN -> point.brighten(workingBitmap, pixelList)
                        else -> null
                    }

                    if (newValue == null) {
                        return workingBitmap
                    }
                    workingBitmap.set(x, y, newValue)

                    if (validate(workingBitmap, pixelList)) {
                        return workingBitmap
                    }
                }

                print("modify has iterated through all of the points in a patch0.")
            }
        }
        else
        {
            while (!validate(workingBitmap, pixelList)) {
                for (point in patch1.points) {
                    val y = point.toY(workingBitmap, pixelList)
                    val x = point.toX(workingBitmap, pixelList)
                    val direction = directions.getPixel(x, y)

                    // Picks a random pixel from the patch changes the color of the pixel to be darker
                    val newValue = when (direction) {
                        // FIXME: Experiment, lighten/darken directions should be opposite of patch0?
                        Color.BLUE -> point.brighten(workingBitmap, pixelList)
                        Color.GREEN -> point.darken(workingBitmap, pixelList)
                        else -> null
                    }

                    if (newValue == null) {
                        return workingBitmap
                    }
                    workingBitmap.set(x, y, newValue)

                    if (validate(workingBitmap, pixelList)) {
                        return workingBitmap
                    }
                }

                print("modify has iterated through all of the points in a patch1.")
            }
        }

        return workingBitmap
    }
}

class Patch(val patchIndex: Int, val size: Int)
{
    var points = IntArray(size).mapIndexed { index, value -> Pixel(patchIndex * size + index)}

    fun brightness(bitmap: Bitmap, pixelList: IntArray): Float
    {
        val values = points.map { pixel -> pixel.brightness(bitmap, pixelList) }
        return values.sum()
    }
}

enum class Constraint(val constraint: Int)
{
    GREATER(1),
    EQUAL(0),
    LESS(-1)
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
