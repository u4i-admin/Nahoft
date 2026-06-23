package org.nahoft

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.operatorfoundation.ion.storage.NounType
import org.operatorfoundation.ion.storage.Word
import org.operatorfoundation.transmission.Pipe
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class PipeEncodingTest {

    @Test
    fun testIntegerEncoding() {
        Timber.plant(Timber.DebugTree())

        val pipe = Pipe()
        val writer = pipe.endA
        val reader = pipe.endB

        // Test values
        val testValues = listOf(
            0,
            1,
            255,
            256,
            1014850000,
            1014860000,
            -1,
            -1014850000,
            Int.MAX_VALUE,
            Int.MIN_VALUE
        )

        for (value in testValues) {
            Timber.d("========================================")
            Timber.d("Testing value: $value (0x${value.toString(16).uppercase()})")

            // Create Storage and write
            val storage = Word.make(value, NounType.INTEGER.value)
            Timber.d("Storage: t=${storage.t}, o=${storage.o}")

            Word.to_conn(writer, storage)

            // Read back the bytes
            val bytesAvailable = reader.available()
            Timber.d("Bytes available: $bytesAvailable")

            val bytes = reader.read(bytesAvailable)
            if (bytes != null) {
                val hexString = bytes.joinToString(" ") { "%02X".format(it) }
                Timber.d("Encoded bytes: $hexString")

                // Parse the encoding
                if (bytes.size >= 2) {
                    val t = bytes[0].toInt()
                    val o = bytes[1].toInt()
                    Timber.d("Type byte: $t, Object byte: $o")

                    if (bytes.size > 2) {
                        val length = bytes[2].toUByte().toInt()
                        val isNegative = (length and 0x80) != 0
                        val actualLength = length and 0x7F

                        Timber.d("Length byte: 0x${bytes[2].toString(16).uppercase()} -> " +
                                "negative=$isNegative, length=$actualLength")

                        if (bytes.size >= 3 + actualLength) {
                            val dataBytes = bytes.sliceArray(3 until 3 + actualLength)
                            val dataHex = dataBytes.joinToString(" ") { "%02X".format(it) }
                            Timber.d("Data bytes: $dataHex")
                        }
                    }
                }
            } else {
                Timber.e("Failed to read bytes!")
            }
        }

        Timber.d("========================================")
        Timber.d("All tests completed")
    }

    @Test
    fun testFirstThreeBytesOfTestSequence() {
        Timber.plant(Timber.DebugTree())

        val pipe = Pipe()
        val writer = pipe.endA
        val reader = pipe.endB

        Timber.d("Testing what Arduino sees: first 3 bytes")
        Timber.d("========================================")

        // Send the ON command first
        val on = Word.make(1, NounType.INTEGER.value)
        Timber.d("Sending ON command (value=1)")
        Word.to_conn(writer, on)

        // Read first 3 bytes like Arduino does
        val bytes = reader.read(3)
        if (bytes != null) {
            Timber.d("First 3 bytes Arduino receives: ${bytes.joinToString(", ") { it.toInt().toString() }}")
            Timber.d("First 3 bytes (hex): ${bytes.joinToString(" ") { "%02X".format(it) }}")

            // This should match what Arduino is seeing: 0, 0, 1
            if (bytes.size == 3) {
                Timber.d("Byte 0: ${bytes[0].toInt()} (expected: type byte)")
                Timber.d("Byte 1: ${bytes[1].toInt()} (expected: object byte)")
                Timber.d("Byte 2: ${bytes[2].toInt()} (expected: length byte)")
            }
        }

        // Now send a large frequency
        Timber.d("========================================")
        Timber.d("Sending large frequency (1014850000)")
        val freq = Word.make(1014850000, NounType.INTEGER.value)
        Word.to_conn(writer, freq)

        val freqBytes = reader.read(reader.available())
        if (freqBytes != null) {
            Timber.d("Frequency bytes: ${freqBytes.joinToString(" ") { "%02X".format(it) }}")
        }
    }
}