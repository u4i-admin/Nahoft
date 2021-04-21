package org.nahoft.nahoft

import android.graphics.BitmapFactory
import org.junit.Assert
import org.junit.Test
import org.nahoft.org.nahoft.swatch.Encoder
import org.nahoft.org.nahoft.swatch.Decoder
import org.nahoft.swatch.Swatch
import org.nahoft.swatch.lengthMessageKey
import org.nahoft.swatch.payloadMessageKey
import java.net.URL

class SwatchInstrumentedTest {
    @ExperimentalUnsignedTypes
    @Test
    fun swatchEncodeTest_1byte() {
        val swatch = Encoder()
        val someData = byteArrayOf(0)
        val encrypted = Swatch.polish(someData, payloadMessageKey)
        val url = URL("https://64.media.tumblr.com/ae7aa5c431127e95f4473efda39f06e5/tumblr_nkymoccyIH1tlnaoto1_500.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        Assert.assertNotNull(encoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchEncodeDecodeTest_1byte() {
        val encoder = Encoder()
        val someData = byteArrayOf(0)
        val encrypted = Swatch.polish(someData, payloadMessageKey)
        val url = URL("https://64.media.tumblr.com/ae7aa5c431127e95f4473efda39f06e5/tumblr_nkymoccyIH1tlnaoto1_500.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = encoder.encode(encrypted, bitmap)

        Assert.assertNotNull(encoded)
        if (encoded == null) {
            return
        }

        val decoder = Decoder()
        val decoded = decoder.decode(encoded)
        Assert.assertArrayEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchEncodeDecodeTest() {
        val swatch = Encoder()
        val someData = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
            28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41)
        val encrypted = Swatch.polish(someData, payloadMessageKey)
        val url = URL("https://64.media.tumblr.com/ae7aa5c431127e95f4473efda39f06e5/tumblr_nkymoccyIH1tlnaoto1_500.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        if (encoded != null) decoded = decoder.decode(encoded)!!
        var decoded = ByteArray(42)
        val decoder = Decoder()
        Assert.assertEquals(encrypted, decoded)
    }

    @Test
    fun testLengthEncryption() {
        val length = "48"
        val lengthData = length.encodeToByteArray()
        val encryptedLength = Swatch.polish(lengthData, lengthMessageKey)
        val decryptedLength = Swatch.unpolish(encryptedLength, lengthMessageKey)
        Assert.assertEquals(encryptedLength, decryptedLength)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchPuppyM1Test() {
        val swatch = Encoder()
        val encrypted = "A".toByteArray()
        val url = URL("https://dogtime.com/assets/uploads/gallery/corgi-puppies/corgi-puppy-1.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        val decoder = Decoder()
        if (encoded != null) decoded = decoder.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchPuppyM2Test() {
        val swatch = Encoder()
        val encrypted = "ABCD1234".toByteArray()
        val url = URL("https://dogtime.com/assets/uploads/gallery/corgi-puppies/corgi-puppy-1.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        val decoder = Decoder()
        if (encoded != null) decoded = decoder.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchPuppyM3Test() {
        val swatch = Encoder()
        val encrypted = "Operator Foundation is the best of all time.".toByteArray()
        val url = URL("https://dogtime.com/assets/uploads/gallery/corgi-puppies/corgi-puppy-1.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        val decoder = Decoder()
        if (encoded != null) decoded = decoder.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchCatM1Test() {
        val swatch = Encoder()
        val encrypted = "A".toByteArray()
        val url = URL("https://media.istockphoto.com/photos/gray-fluffy-kitten-under-transparent-purple-decorative-lamp-picture-id1156261699?s=170x170")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        val decoder = Decoder()
        if (encoded != null) decoded = decoder.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchCatM2Test() {
        val swatch = Encoder()
        val encrypted = "ABCD1234".toByteArray()
        val url = URL("https://media.istockphoto.com/photos/gray-fluffy-kitten-under-transparent-purple-decorative-lamp-picture-id1156261699?s=170x170")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        val decoder = Decoder()
        if (encoded != null) decoded = decoder.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchCatM3Test() {
        val swatch = Encoder()
        val encrypted = "Operator Foundation is the best of all time.".toByteArray()
        val url = URL("https://media.istockphoto.com/photos/gray-fluffy-kitten-under-transparent-purple-decorative-lamp-picture-id1156261699?s=170x170")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        val decoder = Decoder()
        if (encoded != null) decoded = decoder.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchBananaM1Test() {
        val swatch = Encoder()
        val encrypted = "A".toByteArray()
        val url = URL("https://image.freepik.com/free-vector/sketch-bunch-bananas-hand-drawn-bananas-ink-engraved-illustration_152222-194.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        val decoder = Decoder()
        if (encoded != null) decoded = decoder.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchBananaM2Test() {
        val swatch = Encoder()
        val encrypted = "ABCD1234".toByteArray()
        val url = URL("https://image.freepik.com/free-vector/sketch-bunch-bananas-hand-drawn-bananas-ink-engraved-illustration_152222-194.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        val decoder = Decoder()
        if (encoded != null) decoded = decoder.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun swatchBananaM3Test() {
        val swatch = Encoder()
        val encrypted = "Operator Foundation is the best of all time.".toByteArray()
        val url = URL("https://image.freepik.com/free-vector/sketch-bunch-bananas-hand-drawn-bananas-ink-engraved-illustration_152222-194.jpg")
        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val encoded = swatch.encode(encrypted, bitmap)
        var decoded = ByteArray(42)
        val decoder = Decoder()
        if (encoded != null) decoded = decoder.decode(encoded)!!
        Assert.assertEquals(encrypted, decoded)
    }

}
