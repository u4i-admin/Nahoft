package org.nahoft.swatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import androidx.core.graphics.set
import org.nahoft.codex.Encryption
import org.nahoft.org.nahoft.swatch.*
import org.nahoft.stencil.CapturePhotoUtils
import java.nio.ByteBuffer
import kotlin.math.absoluteValue
import kotlin.random.Random

val lengthMessageSeed = 1
val payloadMessageSeed = 2

class Swatch {
    companion object {
        val minimumPatchSize = 2

        // Maximum Message Size:
        // 1,000 characters * 4 bytes per character (as a guess) * number of bits in a byte
        val maxMessageSizeBits = 1000 * 4 * 8
    }
}

