package org.nahoft.nahoft

import android.graphics.BitmapFactory
import org.junit.Assert
import org.junit.Test
import java.net.URL
import org.nahoft.org.nahoft.swatch.Encoder

class StencilInstrumentedTest {

//    @ExperimentalUnsignedTypes
//    @Test
//    fun encodeDecodeTest() {
//        Stencil()
//        val encrypted = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
//            28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41)
//        val coverUri: Uri = Uri.parse("http://www.google.com")
//        val encoded = Stencil().encode(null, encrypted, coverUri)
//        val decoded = Stencil().decode(null, encoded!!)
//        Assert.assertEquals(encrypted, decoded)
//    }

//    @ExperimentalUnsignedTypes
//    @Test
//    fun swatchEncodeDecodeTest() {
//        val swatch = Encoder()
//        val encrypted = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
//            28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41)
////        val instrumentation = getInstrumentation()
////        val context = instrumentation.context
////        val resource = context.resources
//        val url = URL("https://64.media.tumblr.com/ae7aa5c431127e95f4473efda39f06e5/tumblr_nkymoccyIH1tlnaoto1_500.jpg")
//        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
////        val puppyPic = R.drawable.the_puppy
////        val bitmap = BitmapFactory.decodeResource(Resources.getSystem(), puppyPic)
//        val encoded = swatch.encode(encrypted, bitmap)
//        var decoded = ByteArray(42)
//        if (encoded != null) decoded = swatch.decode(encoded)!!
//        Assert.assertEquals(encrypted, decoded)
//    }
}