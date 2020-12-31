package org.operatorfoundation.nahoft

import android.graphics.Bitmap
import org.junit.Test

import org.junit.Assert.*
import org.nahoft.stencil.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class StencilUnitTest {
    @Test
    fun star_encode_decode() {
        val encrypted = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
        28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41)
        val width = 960
        val height = 1280

        val stencil = Stencil()

        val numBits = encrypted.size * 8
        val maxStars: Int = (height / 3) * (width / 3)
        if (numBits > maxStars) {
            return
        }

        val bits = bitsFromBytes(encrypted)
        var lastPosition = Pair<Int,Int>(-1, -1)

        for (index in bits.indices)
        {
            val bit = bits[index]

            if (bit == 1) {
                val position = stencil.fitStar(height, width, index)
                assertNotEquals(position, lastPosition)
                lastPosition = position
            } else if (bit == 0) {
                val position = stencil.fitStar(height, width, index)
                assertNotEquals(position, lastPosition)
                lastPosition = position
            } else {
                println("Bad bit! " + bit)
            }
        }
    }
}