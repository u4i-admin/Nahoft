package org.nahoft.org.nahoft.swatch

import android.graphics.Color
import kotlin.math.absoluteValue

class Pixel(val index: Int, var bitmap: MappedBitmap)
{
    val x: Int
        get() = index % bitmap.width
    val y: Int
        get() = index / bitmap.width

    val color: Int
        get() = bitmap.getPixel(index)

    fun brightness(): Int
    {
        val colorInt = bitmap.getPixel(index)
        val color = Color.valueOf(colorInt)
        return color.brightness()
    }

    // Tries to increase the brightness by targetChanbgeInBrightness, returns actual change in brightness achieved.
    fun brighten(targetChangeInBrightness: Int): Int
    {
        if (targetChangeInBrightness == 0) {
            // Success!
            return 0
        }

        var offsetAmount = targetChangeInBrightness * 3

        val colorInt = bitmap.getPixel(index)
        val color = Color.valueOf(colorInt)
        val a: Int = color.alphaInt()
        var r: Int = color.redInt()
        var g: Int = color.greenInt()
        var b: Int = color.blueInt()

        // If any of the values are already 255 don't modify this pixel
        if (r == 255 || b == 255 || g == 255) { return 0 }

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

        var oldBrightness = brightness()

        val newColor = Color.argb(a, r, g, b)
        bitmap.setPixel(index, newColor)

        // FIXME - remove
        if (index == 82343) {
            print("Known bad pixel")
        }

        var newBrightness = brightness()

        return (newBrightness - oldBrightness).absoluteValue
    }

    // Tries to decrease the brightness by targetChanbgeInBrightness, returns actual change in brightness achieved.
    fun darken(targetChangeInBrightness: Int): Int
    {
        if (targetChangeInBrightness == 0) {
            // Success!
            return 0
        }

        var offsetAmount = targetChangeInBrightness * 3

        val colorInt = bitmap.getPixel(index)
        val color = Color.valueOf(colorInt)
        val a = color.alphaInt()
        var r = color.redInt()
        var g = color.greenInt()
        var b = color.blueInt()

        // If any of the values are already 0 don't modify this pixel
        if (r == 0 || b == 0 || g == 0) { return 0 }

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

        var oldBrightness = brightness()

        val newColor = Color.argb(a, r, g, b)
        bitmap.setPixel(index, newColor)

        // FIXME - remove
        if (index == 82343) {
            print("Known bad pixel")
        }

        var newBrightness = brightness()

        return (newBrightness - oldBrightness).absoluteValue
    }
}

class MappedPixel(val x: Int, val y: Int, val bitmap: MappedBitmap) {
    val index: Int
        get() = y * bitmap.width + x
}