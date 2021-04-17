package org.nahoft.org.nahoft.swatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.nahoft.codex.Encryption
import org.nahoft.swatch.*
import java.nio.ByteBuffer
import kotlin.random.Random

class Decoder {
    fun decode(context: Context, uri: Uri): ByteArray?
    {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))

        return decode(bitmap)
    }

    fun decode(bitmap: Bitmap): ByteArray?
    {
        //val lengthBitsSize = java.lang.Integer.BYTES * 8
        val lengthBitsSize = 16 * 8
        val lengthBits = decode(bitmap, lengthBitsSize, lengthMessageSeed)

        // FIXME: Ciphertext is wrong length
        if (lengthBits == null) { return null }
        lengthBits?.let {
            val encryptedLengthBytes = bytesFromBits(lengthBits)
            encryptedLengthBytes?.let {
                val lengthBytes = Encryption().decryptLengthData(encryptedLengthBytes)
                val length = ByteBuffer.wrap(lengthBytes).getInt()
                val messageBits = decode(bitmap, length, payloadMessageSeed)
                if (messageBits == null)
                { return null }
                return bytesFromBits(messageBits)
            }
        }

        return null
    }

    fun decode(bitmap: Bitmap, size: Int, messageSeed: Int): List<Int>?
    {
        val numberOfPixels = bitmap.height * bitmap.width
        val patchSize = numberOfPixels / (size*2)

        if (patchSize < Swatch.minimumPatchSize) { return null }

        // Randomly map the pixels
        // Make an array of integers 0 - numberOfPixels and shuffle it
        val mapped = MappedBitmap(bitmap, messageSeed)

        var message = IntArray(size)

        for (index in message.indices)
        {
            val detector = Detector(index, patchSize, mapped)

            when (detector.constraint)
            {
                DecoderConstraint.GREATER -> message[index] = 1
                DecoderConstraint.LESS -> message[index] = 0
                DecoderConstraint.EQUAL -> return null
            }
        }

        return message.toList()
    }
}

class Detector(index: Int, patchSize: Int, mapped: MappedBitmap) {
    var patch0: Patch
    var patch1: Patch
    var constraint: DecoderConstraint

    init {
        patch0 = Patch(index * 2, patchSize, mapped)
        patch1 = Patch((index * 2) + 1, patchSize, mapped)

        val b0 = patch0.brightness
        val b1 = patch1.brightness

        if (b0 > b1) {
            constraint = DecoderConstraint.GREATER
        } else if (b0 < b1) {
            constraint = DecoderConstraint.LESS
        } else {
            constraint = DecoderConstraint.EQUAL
        }
    }
}

enum class DecoderConstraint(val constraint: Int)
{
    GREATER(1),
    EQUAL(0),
    LESS(-1)
}
