package org.nahoft.org.nahoft.swatch

import android.graphics.Bitmap
import kotlin.random.Random

class MappedBitmap(var bitmap: Bitmap, key: Int) {
    var mapping: IntArray

    init {
        val numberOfPixels = bitmap.height * bitmap.width
        val random = Random(key)
        mapping = IntArray(numberOfPixels) { it }
        mapping.shuffle(random)
    }

    val width: Int
        get() = bitmap.width

    fun getPixel(index: Int): Int {
        val mappedIndex = mapping[index]

        val x = mappedIndex % bitmap.width
        val y = mappedIndex / bitmap.width

        return bitmap.getPixel(x, y)
    }

    fun setPixel(index: Int, color: Int) {
        val mappedIndex = mapping[index]

        val x = mappedIndex % bitmap.width
        val y = mappedIndex / bitmap.width

        // FIXME - remove
        if (x == 5 && y == 445) {
            print("Known bad pixel")
        }

        return bitmap.setPixel(x, y, color)
    }

    fun getMappedX(index: Int): Int {
        val mappedIndex = mapping[index]
        return mappedIndex % bitmap.width
    }

    fun getMappedY(index: Int): Int {
        val mappedIndex = mapping[index]
        return mappedIndex / bitmap.width
    }
}