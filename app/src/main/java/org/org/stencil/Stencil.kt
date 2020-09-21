package org.org.stencil

import android.graphics.Bitmap
import kotlin.math.ceil
import kotlin.math.sqrt
import org.org.codex.makeBitSet

class Stencil {
    fun encode(plaintext: String, cover: Bitmap): Bitmap?
    {
        val numBits = plaintext.length * 8
        val numPixels = cover.height * cover.width

        val maxStarsByHeight = cover.height / 3
        var maxStarsByWidth = cover.width / 3
        val maxStars = maxStarsByHeight * maxStarsByWidth

        if (maxStars < numBits) {return null}

        val bits = makeBitSet(plaintext.toByteArray())

        var result = cover
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

        return result
    }

    private fun addStar(bitmap: Bitmap, index: Int, bit: Boolean, maxStarsByWidth: Int): Bitmap
    {
        val row = index / maxStarsByWidth
        val column = index % maxStarsByWidth

        val heightOffset = (3 * row) + 2
        val widthOffset = (3 * column) + 2

        if (bit)
        {
            bitmap.setPixel(widthOffset, heightOffset, 255)
        }
        else
        {
            bitmap.setPixel(widthOffset, heightOffset, 0)
        }

        bitmap.setPixel(widthOffset-1, heightOffset, 128)
        bitmap.setPixel(widthOffset, heightOffset-1, 128)
        bitmap.setPixel(widthOffset+1, heightOffset, 128)
        bitmap.setPixel(widthOffset, heightOffset+1, 128)

        return bitmap
    }

    fun decode(ciphertext: Bitmap): String
    {
        return "TBD"
    }
}
