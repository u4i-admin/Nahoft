package org.nahoft.nahoft

import android.graphics.BitmapFactory
import org.junit.Assert
import org.junit.Test
import org.nahoft.codex.Encryption
import org.nahoft.swatch.Swatch
import java.net.URL

class SwatchInstrumentedTest {

    @ExperimentalUnsignedTypes
    @Test
    fun swatchEncodeDecodeTest() {
        val swatch = Swatch()
        val someData = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
            28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41)
        val encrypted = Encryption().encryptLengthData(someData)
        val url = URL("https://64.media.tumblr.com/ae7aa5c431127e95f4473efda39f06e5/tumblr_nkymoccyIH1tlnaoto1_500.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchPuppyM1Test() {
        val swatch = Swatch()
        val encrypted = "A".toByteArray()
        val url = URL("https://dogtime.com/assets/uploads/gallery/corgi-puppies/corgi-puppy-1.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchPuppyM2Test() {
        val swatch = Swatch()
        val encrypted = "ABCD1234".toByteArray()
        val url = URL("https://dogtime.com/assets/uploads/gallery/corgi-puppies/corgi-puppy-1.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchPuppyM3Test() {
        val swatch = Swatch()
        val encrypted = "Operator Foundation is the best of all time.".toByteArray()
        val url = URL("https://dogtime.com/assets/uploads/gallery/corgi-puppies/corgi-puppy-1.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchCatM1Test() {
        val swatch = Swatch()
        val encrypted = "A".toByteArray()
        val url = URL("https://media.istockphoto.com/photos/gray-fluffy-kitten-under-transparent-purple-decorative-lamp-picture-id1156261699?s=170x170")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchCatM2Test() {
        val swatch = Swatch()
        val encrypted = "ABCD1234".toByteArray()
        val url = URL("https://media.istockphoto.com/photos/gray-fluffy-kitten-under-transparent-purple-decorative-lamp-picture-id1156261699?s=170x170")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchCatM3Test() {
        val swatch = Swatch()
        val encrypted = "Operator Foundation is the best of all time.".toByteArray()
        val url = URL("https://media.istockphoto.com/photos/gray-fluffy-kitten-under-transparent-purple-decorative-lamp-picture-id1156261699?s=170x170")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchBananaM1Test() {
        val swatch = Swatch()
        val encrypted = "A".toByteArray()
        val url = URL("https://image.freepik.com/free-vector/sketch-bunch-bananas-hand-drawn-bananas-ink-engraved-illustration_152222-194.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchBananaM2Test() {
        val swatch = Swatch()
        val encrypted = "ABCD1234".toByteArray()
        val url = URL("https://image.freepik.com/free-vector/sketch-bunch-bananas-hand-drawn-bananas-ink-engraved-illustration_152222-194.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchBananaM3Test() {
        val swatch = Swatch()
        val encrypted = "Operator Foundation is the best of all time.".toByteArray()
        val url = URL("https://image.freepik.com/free-vector/sketch-bunch-bananas-hand-drawn-bananas-ink-engraved-illustration_152222-194.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        if (encoded != null) decoded = swatch.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

}
