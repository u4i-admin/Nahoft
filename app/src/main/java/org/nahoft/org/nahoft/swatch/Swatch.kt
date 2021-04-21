package org.nahoft.swatch

val lengthMessageKey = 1
val payloadMessageKey = 2

class Swatch {
    companion object {
        val minimumPatchSize = 400

        // Maximum Message Size:
        // 1,000 characters * 4 bytes per character (as a guess) * number of bits in a byte
        val maxMessageSizeBits = 1000 * 4 * 8

        fun polish(data: ByteArray, key: Int): ByteArray
        {
            val random = java.util.Random(key.toLong())
            var entropy = ByteArray(data.size)
            random.nextBytes(entropy)

            var result = ByteArray(data.size)
            for ((index, value) in data.withIndex()) {
                val dataInt =  value.toInt()
                val entropyInt = entropy[index].toInt()
                val resultInt = dataInt xor entropyInt
                result[index] = resultInt.toByte()
            }

            return result
        }

        fun unpolish(data: ByteArray, key: Int): ByteArray {
            // This transformation is symmetric, so polish is the same as unpolish.
            return Swatch.polish(data, key)
        }
    }
}

