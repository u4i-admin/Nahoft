package org.org.stencil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.get
import kotlin.math.ceil
import kotlin.math.sqrt
import org.org.codex.makeBitSet

class Stencil {
    fun encode(context: Context, encrypted: ByteArray, coverUri: Uri): Uri?
    {
        val cover = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, coverUri))

        val numBits = encrypted.size * 8
        val numPixels = cover.height * cover.width

        val maxStarsByHeight = cover.height / 3
        var maxStarsByWidth = cover.width / 3
        val maxStars = maxStarsByHeight * maxStarsByWidth

        if (maxStars < numBits) {return null}

        val bits = makeBitSet(encrypted)

        var result = cover.copy(Bitmap.Config.ARGB_8888, true);
        for (index in 0 until bits.length())
        {
            var bit = bits.get(index)
            result = addStar(result, index, bit, maxStarsByWidth)
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

    private fun addStar(bitmap: Bitmap, index: Int, bit: Boolean, maxStarsByWidth: Int): Bitmap
    {
        val row = index / maxStarsByWidth
        val column = index % maxStarsByWidth

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