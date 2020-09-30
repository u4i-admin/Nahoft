package org.nahoft.stencil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.get
import kotlin.math.ceil
import kotlin.math.sqrt
import org.nahoft.codex.makeBitSet
import java.util.*

class Stencil {
    fun encode(context: Context, encrypted: ByteArray, coverUri: Uri): Uri?
    {
        val cover = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, coverUri))

        val numBits = encrypted.size * 8
        val numPixels = cover.height * cover.width

        val maxStars: Int = (cover.height/3) * (cover.width/3)
        if (numBits > maxStars)
        {
            return null
        }

        if (maxStars < numBits) {return null}

        val bits = makeBitSet(encrypted)
        val bitsLen = bits.length()

        val numColumns = fitStars(cover, bitsLen)!!

        var result = cover.copy(Bitmap.Config.ARGB_8888, true);
        for (index in 0 until bits.length())
        {
            var bit = bits.get(index)

            if (bit)
            {
                result = addStar(result, index, 255, numColumns)
            }
            else
            {
                result = addStar(result, index, 0, numColumns)
            }
        }

        result = addStar(result, bits.length(), 128, numColumns)

        // Quality check
        val decoded = decode(result)
        if (decoded == null)
        {
            return null
        }

//        result = destroy(result)

        val pixelsPerBit = numPixels.toDouble() / numBits.toDouble()
        val heightPerBit = ceil(cover.height.toDouble() / sqrt(pixelsPerBit)).toInt()
        val widthPerBit = ceil(cover.width.toDouble() / sqrt(pixelsPerBit)).toInt()

        val heightCenter = heightPerBit / 2
        val widthCenter = widthPerBit / 2

        val title = ""
        val description = ""
        val resultUri = CapturePhotoUtils.insertImage(context.contentResolver, result, title , description);

        return resultUri
    }

    private fun fitStars(bitmap: Bitmap, numBits: Int): Int?
    {
        val maxRows = bitmap.height / 3
        val maxColumns = bitmap.width / 3

        var numColumnsPicked = 0
        var valid = false
        for (numRows in 1..maxRows)
        {
            for (numColumns in 1..maxColumns)
            {
                val numStars = numRows * numColumns
                if (numStars >= numBits)
                {
                    valid = true
                    numColumnsPicked = numColumns
                    break
                }
            }
        }

        if (!valid)
        {
            return null
        }

        return numColumnsPicked
    }

    private class Dimensions(val x: Int, val y: Int)

    private fun addStar(bitmap: Bitmap, index: Int, color: Int, numColumns: Int): Bitmap?
    {
        val row = index / numColumns
        val column = index % numColumns

        val heightOffset = (3 * row) + 2
        val widthOffset = (3 * column) + 2

        var newBitmap = bitmap

        newBitmap.setPixel(widthOffset, heightOffset, color)

        newBitmap.setPixel(widthOffset-1, heightOffset, 128)
        newBitmap.setPixel(widthOffset, heightOffset-1, 128)
        newBitmap.setPixel(widthOffset+1, heightOffset, 128)
        newBitmap.setPixel(widthOffset, heightOffset+1, 128)

        return newBitmap
    }

    fun destroy(bitmap: Bitmap): Bitmap
    {
        var newBitmap = bitmap
        for (x in 0 until bitmap.width-1)
        {
            for(y in 0 until bitmap.height-1)
            {
                newBitmap.setPixel(x, y, 0)
            }
        }

        return newBitmap
    }

    fun decode(context: Context, uri: Uri): ByteArray?
    {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        return decode(bitmap)
    }

    fun decode(bitmap: Bitmap): ByteArray?
    {
        var working = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        val maxColumns = bitmap.width / 3

        var bits: List<Int>? = null
        for (numColumns in 1 until maxColumns)
        {
            bits = findStars(working, numColumns)
            if (bits != null)
            {
                return decodeStars(bits)
            }
        }

        return null
    }

    fun findStars(bitmap: Bitmap, numColumns: Int): List<Int>?
    {
        var result: List<Int> = emptyList()

        var index = 0
        var done = false
        while (!done)
        {
            val row = index / numColumns
            val column = index % numColumns

            val heightOffset = (3 * row) + 2
            val widthOffset = (3 * column) + 2

            try
            {
                val color = bitmap.getPixel(widthOffset, heightOffset)
                when (color)
                {
                    0 -> result += 0
                    255 -> result += 1
                    128 -> done = true
                    else -> return null
                }
            }
            catch(e: Exception)
            {
                return null
            }

            index += 1
        }

        return result
    }

    fun decodeStars(stars: List<Int>): ByteArray
    {
        val bits = BitSet(stars.size)
        for (index in 0 until stars.size)
        {
            val star = stars[index]
            when (star)
            {
                0 -> bits.clear(index)
                1 -> bits.set(index)
            }
        }

        return bits.toByteArray()
    }
}

class Star(val x: Int, val y: Int)