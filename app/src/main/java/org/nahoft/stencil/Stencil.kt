package org.nahoft.stencil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.get
import kotlin.math.ceil
import kotlin.math.sqrt
import org.nahoft.codex.makeBitSet

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

        var result = cover.copy(Bitmap.Config.ARGB_8888, true);
        for (index in 0 until bits.length())
        {
            var bit = bits.get(index)
            result = addStar(result, index, bit, bits.length())
        }

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

    private fun addStar(bitmap: Bitmap, index: Int, bit: Boolean, numBits: Int): Bitmap?
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

        val row = index / numColumnsPicked
        val column = index % numColumnsPicked

        val heightOffset = (3 * row) + 2
        val widthOffset = (3 * column) + 2

        var newBitmap = bitmap

        if (bit)
        {
            newBitmap.setPixel(widthOffset, heightOffset, 255)
        }
        else
        {
            newBitmap.setPixel(widthOffset, heightOffset, 0)
        }

        newBitmap.setPixel(widthOffset-1, heightOffset, 128)
        newBitmap.setPixel(widthOffset, heightOffset-1, 128)
        newBitmap.setPixel(widthOffset+1, heightOffset, 128)
        newBitmap.setPixel(widthOffset, heightOffset+1, 128)

        return newBitmap
    }

    fun decode(context: Context, uri: Uri): ByteArray
    {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        return decode(bitmap)
    }

    fun decode(bitmap: Bitmap): ByteArray
    {
        var stars = findStars(bitmap)
        stars = verifyStars(bitmap, stars)

        return decodeStars(stars)
    }

    fun findStars(bitmap: Bitmap): List<Star>
    {
        var result: List<Star> = listOf()

        for (x in 0..bitmap.width)
        {
            for (y in 0..bitmap.height)
            {
                val value = bitmap.get(x, y)
                if ((value == 0) or (value == 255))
                {
                    val star = Star(x, y)
                    result += star
                }
            }
        }

        return result
    }

    fun verifyStars(bitmap: Bitmap, stars: List<Star>): List<Star>
    {
        var result: List<Star> = listOf()

        for (star in stars)
        {
            val x = star.x
            val y = star.y

            if (bitmap.get(x+1, y) != 128) {continue}
            if (bitmap.get(x+1, y+1) != 128) {continue}
            if (bitmap.get(x, y+1) != 128) {continue}
            if (bitmap.get(x+1, y+1) != 128) {continue}

            result += star
        }

        return result
    }

    fun decodeStars(stars: List<Star>): ByteArray
    {
        return byteArrayOf()
    }
}

class Star(val x: Int, val y: Int)