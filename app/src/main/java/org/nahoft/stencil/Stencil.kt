package org.nahoft.stencil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.core.graphics.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.math.roundToInt
import kotlin.math.sqrt

class Stencil {
    private var cachedIndex: Int? = null
    private var cachedRow: Int = 0
    private var cachedCol: Int = 0
    private var cachedLeft: Int = 0
    private var cachedRight: Int = 0
    private var cachedTop: Int = 0
    private var cachedBottom: Int = 0
    private var cachedDirection: Pair<Int, Int> = Pair<Int, Int>(0, 0)

//    val listener: ImageDecoder.OnHeaderDecodedListener =
//        ImageDecoder.OnHeaderDecodedListener { decoder, info, source ->
//            decoder.setOnPartialImageListener {exception->
//                Log.d("ImageDecoder",exception.error.toString())
//                true
//            }
//        }

    @ExperimentalUnsignedTypes
    fun encode(context: Context, encrypted: ByteArray, coverUri: Uri): Uri?
    {
        val cover = BitmapFactory.decodeStream(context.contentResolver.openInputStream(coverUri))
        val numBits = encrypted.size * 8
        val maxStars: Int = (cover.height / 3) * (cover.width / 3)
        if (numBits > maxStars) {
            return null
        }

        val bits = bitsFromBytes(encrypted)
        var result = cover.copy(Bitmap.Config.ARGB_8888, true)

        // Resize result if it is larger than 4mb
        val sizeBytes = result.height * result.width * 4
        val targetSizeBytes = 4000000
        if (sizeBytes > targetSizeBytes)
        {
            val originalSize = ImageSize(result.height, result.width, result.density)
            val scaledSize = resizePreservingAspectRatio(originalSize, targetSizeBytes)
            
            result = Bitmap.createScaledBitmap(result, scaledSize.width, scaledSize.height, true)
        }

        for (index in bits.indices)
        {
            val bit = bits[index]

            if (bit == 1) {
                val position = fitStar(result.height, result.width, index)
                result = addStar(result, position, 255)
            } else if (bit == 0) {
                val position = fitStar(result.height, result.width, index)
                result = addStar(result, position, 0)
            } else {
                println("Bad bit! " + bit)
            }
        }

        val position = fitStar(result.height, result.width, bits.size)
        result = addStar(result, position, 128)
        result = fill(result, bits.size + 1)

        // Quality check - comment out for speed increase
        // val decoded = decode(result) ?: return null

        val title = ""
        val description = ""
        val resultUri = CapturePhotoUtils.insertImage(context, result, title, description)

        return resultUri
    }

    private fun resizePreservingAspectRatio(originalSize: ImageSize, targetSizeBytes: Int): ImageSize
    {
        val aspectRatio = originalSize.height/originalSize.width
        val targetSizePixels = targetSizeBytes/originalSize.colorDepthBytes
        val scaledWidth = sqrt(targetSizePixels.toDouble()/aspectRatio)
        val scaledHeight = aspectRatio * scaledWidth

        return  ImageSize(scaledHeight.roundToInt(), scaledWidth.roundToInt(), originalSize.colorDepthBytes)
    }

    private fun setPixel(bitmap: Bitmap, x: Int, y: Int, value: Int)
    {
        val color = Color.argb(value, value, value, value)
        bitmap.setPixel(x, y, color)
    }

    public fun fitStar(height: Int, width: Int, index: Int): Pair<Int, Int>
    {
        var row = 0
        var column = 0

        var left = 0
        var right = (width / 3) - 1
        var top = 1
        var bottom = (height / 3) - 1

        var direction = Pair(1, 0)

        var startOffset = 0

        if (cachedIndex != null)
        {
            val oldIndex = cachedIndex!!

            if(index == oldIndex + 1)
            {
                row = cachedRow
                column = cachedCol
                left = cachedLeft
                right = cachedRight
                top = cachedTop
                bottom = cachedBottom
                direction = cachedDirection
                startOffset = oldIndex
            }
        }

        for (offset in startOffset until index)
        {
            val xoff = direction.first
            val yoff = direction.second

            val newColumn = column + xoff
            val newRow = row + yoff

            if (xoff == 1) // Right
            {
                if (newColumn > right)
                {
                    right -= 1
                    direction = Pair(0, 1)
                    row = row + 1
                }
                else
                {
                    column = newColumn
                }
            }
            else if(yoff == 1) // Down
            {
                if (newRow > bottom)
                {
                    bottom -= 1
                    direction = Pair(-1, 0)
                    column = column - 1
                }
                else
                {
                    row = newRow
                }
            }
            else if (xoff == -1) // Left
            {
                if (newColumn < left)
                {
                    left += 1
                    direction = Pair(0, -1)
                    row = row - 1
                }
                else
                {
                    column = newColumn
                }
            }
            else if(yoff == -1) // Up
            {
                if (newRow < top)
                {
                    top += 1
                    direction = Pair(1, 0)
                    column = column + 1
                }
                else
                {
                    row = newRow
                }
            }
        }

        if((column < 0) || (row < 0))
        {
            print("break!")
        }

        cachedIndex = index
        cachedRow = row
        cachedCol = column
        cachedLeft = left
        cachedRight = right
        cachedTop = top
        cachedBottom = bottom
        cachedDirection = direction

        return Pair((column * 3) + 1, (row * 3) + 1)
    }

    //private class Dimensions(val x: Int, val y: Int)

    private fun addStar(bitmap: Bitmap, position: Pair<Int, Int>, color: Int): Bitmap?
    {
        val widthOffset = position.first
        val heightOffset = position.second

        val newBitmap = bitmap

        setPixel(newBitmap, widthOffset, heightOffset, Color.argb(255, color, color, color))

        setPixel(newBitmap, widthOffset - 1, heightOffset, Color.argb(255, 255, 255, 255))
        setPixel(newBitmap, widthOffset, heightOffset - 1, Color.argb(255, 255, 255, 255))
        setPixel(newBitmap, widthOffset + 1, heightOffset, Color.argb(255, 255, 255, 255))
        setPixel(newBitmap, widthOffset, heightOffset + 1, Color.argb(255, 255, 255, 255))

        setPixel(newBitmap, widthOffset - 1, heightOffset + 1, Color.argb(255, 255, 255, 255))
        setPixel(newBitmap, widthOffset - 1, heightOffset - 1, Color.argb(255, 255, 255, 255))
        setPixel(newBitmap, widthOffset + 1, heightOffset + 1, Color.argb(255, 255, 255, 255))
        setPixel(newBitmap, widthOffset + 1, heightOffset - 1, Color.argb(255, 255, 255, 255))

        return newBitmap
    }

    fun fill(bitmap: Bitmap, startIndex: Int): Bitmap
    {
        var index = startIndex
        var position = fitStar(bitmap.height, bitmap.width, index)
        var widthOffset = position.first
        var heightOffset = position.second
        var result = bitmap

        // Until we reach a top-left corner
        while ((widthOffset != heightOffset) || (widthOffset > bitmap.width / 2))
        {
            // Easy, (not very random) psuedo-random color generator.
            val color = ((index % 3) % 2) * 255
            val newBitmap = addStar(result, position, color)
            newBitmap?.let {
                result = newBitmap
            }

            index += 1
            position = fitStar(bitmap.height, bitmap.width, index)
            widthOffset = position.first
            heightOffset = position.second
        }

        return result
    }

    fun destroy(bitmap: Bitmap): Bitmap
    {
        val colors = arrayOf(
            Color.argb(0, 0, 0, 0), Color.argb(255, 255, 255, 255), Color.argb(
                128,
                128,
                128,
                128
            )
        )

        var index = 0
        var newBitmap = bitmap
        for (x in 0 until bitmap.width-1)
        {
            for(y in 0 until bitmap.height-1)
            {
                newBitmap.setPixel(x, y, colors[index % 3])
                index += 1
            }
        }

        return newBitmap
    }

    fun decode(context: Context, uri: Uri): ByteArray?
    {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
        return decode(bitmap)
    }

    fun decode(bitmap: Bitmap): ByteArray?
    {
        var working = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        val bits = findStars(working)
        if (bits != null)
        {
            return decodeStars(bits)
        }

        return null
    }

    fun findStars(bitmap: Bitmap): List<Int>?
    {
        var result: List<Int> = emptyList()

        var index = 0
        var done = false
        while (!done)
        {
            try
            {
                val position = fitStar(bitmap.height, bitmap.width, index)
                index += 1

                val colorValue = bitmap.get(position.first, position.second)
                val color = decodeColor(colorValue)
                when (color)
                {
                    DecodeBitResult.Zero -> result += 0
                    DecodeBitResult.One -> result += 1
                    DecodeBitResult.Stop -> done = true
                    DecodeBitResult.Error -> return null
                }
            }
            catch (e: Exception)
            {
                return null
            }
        }

        return result
    }

    fun decodeColor(color: Int): DecodeBitResult
    {
        val alpha = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val values = listOf(r, g, b)

        if (checkValues(values, 0, 80))
        {
            return DecodeBitResult.Zero
        }
        else if(checkValues(values, 200, 255))
        {
            return DecodeBitResult.One
        }
        else
        {
            return DecodeBitResult.Stop
        }
    }

    fun checkValue(value: Int, lower: Int, upper: Int): Boolean
    {
        return (value >= lower) and (value <= upper)
    }

    fun checkValues(values: List<Int>, lower: Int, upper: Int): Boolean
    {
        for (value in values)
        {
            if (checkValue(value, lower, upper))
            {
                continue
            }
            else
            {
                return false
            }
        }

        return true
    }

    fun decodeStars(stars: List<Int>): ByteArray?
    {
        return bytesFromBits(stars)
    }
}

class Star(val x: Int, val y: Int)

enum class DecodeBitResult(val value: Int)
{
    Zero(0),
    One(255),
    Stop(128),
    Error(-1024)
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

private data class ImageSize(val height: Int, val width: Int, val colorDepthBytes: Int)