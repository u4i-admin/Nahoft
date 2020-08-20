package org.operatorfoundation.codex

import java.math.BigInteger
import java.util.*

class Codex {
    fun encode(plaintext: String): String
    {
        val data = plaintext.toByteArray()
        val bits = makeBitSet(data)

        val script = AlphanumericScript()
        val result = script.encode(bits)

        return result
    }

    fun decode(ciphertext: String): String
    {
        val script = AlphanumericScript()
        val bits = script.decode(ciphertext)
        val data = bits.toByteArray()
        val result = String(data)

        return result
    }

    fun makeBitSet(bytes: ByteArray): BitSet
    {
        var result = BitSet(bytes.size * 8)

        for (byteIndex in 0..bytes.size-1)
        {
            val byte = bytes[byteIndex]

            for (bitIndex in 0..7)
            {
                val byteInt = byte.toInt()
                val bigInt = byteInt.toBigInteger()
                val bigBit = (bigInt shl bitIndex) shr (7 - bitIndex)
                val bit = bigBit.toInt()
                val bool = bit != 0

                val resultIndex = (byteIndex * 8) + bitIndex

                result.set(resultIndex, bool)
            }
        }

        return result
    }
}