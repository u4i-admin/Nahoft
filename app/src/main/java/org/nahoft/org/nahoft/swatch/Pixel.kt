package org.nahoft.org.nahoft.swatch

import android.graphics.Color

class Pixel(val index: Int, var bitmap: MappedBitmap)
{
    val x: Int
        get() = index % bitmap.width
    val y: Int
        get() = index / bitmap.width

    fun brightness(): Int
    {
        val colorInt = bitmap.getPixel(index)
        val color = Color.valueOf(colorInt)
        return color.brightness()
    }

    fun brighten(targetChangeInBrightness: Int): Int?
    {
        if (targetChangeInBrightness == 0) {
            // Success!
            return 0
        }

        val colorInt = bitmap.getPixel(index)
        val color = Color.valueOf(colorInt)
        val a: Int = color.alphaInt()
        var r: Int = color.redInt()
        var g: Int = color.greenInt()
        var b: Int = color.blueInt()
        var offsetAmount = targetChangeInBrightness * 3

        // If any of the values are already 255 don't modify this pixel
        if (r == 255 || b == 255 || g == 255) { return null }

        // If the offsetAmount will cause any of the color values to exceed 255,
        // change the value to a number that will cause that color value to be exactly 255
        if ((r + offsetAmount) > 255)
        {
            offsetAmount = 255 - r
        }

        if ((g + offsetAmount) > 255)
        {
            offsetAmount = 255 - g
        }

        if ((b + offsetAmount) > 255)
        {
            offsetAmount = 255 - b
        }

        // Increase each color value by the settled on offsetAmount
        r += offsetAmount
        g += offsetAmount
        b += offsetAmount

        // Return the new correct color for this pixel
        val newColor = Color.argb(a, r, g, b)
        return newColor
    }

    fun darken(targetBrightness: Int): Int?
    {
        val colorInt = bitmap.getPixel(index)
        val color = Color.valueOf(colorInt)
        var offsetAmount = targetBrightness * 3
        val a = color.alphaInt()
        var r = color.redInt()
        var g = color.greenInt()
        var b = color.blueInt()

        // If any of the values are already 0 don't modify this pixel
        if (r == 0 || b == 0 || g == 0) { return null }

        // If the offsetAmount will cause any of the color values to exceed 255,
        // change the value to a number that will cause that color value to be exactly 255
        if ((r - offsetAmount) < 0)
        {
            offsetAmount = r
        }

        if ((g - offsetAmount) < 0)
        {
            offsetAmount = g
        }

        if ((b - offsetAmount) < 0)
        {
            offsetAmount = b
        }

        // Increase each color value by the settled on offsetAmount
        r -= offsetAmount
        g -= offsetAmount
        b -= offsetAmount

        // Return the new correct color for this pixel
        val newColor = Color.argb(a, r, g, b)
        return newColor
    }
}
