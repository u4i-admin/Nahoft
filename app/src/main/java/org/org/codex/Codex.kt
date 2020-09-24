package org.org.codex

import java.util.*

enum class KeyOrMessage(val byte: Byte)
{
    Key(0),
    EncryptedMessage(1)
}

class DecodeResult(val type: KeyOrMessage, val payload: ByteArray)

class Codex {
    fun encodeKey(key: ByteArray): String
    {
        return encode(0, key)
    }

    fun encodeEncryptedMessage(message: ByteArray): String
    {
        return encode(1, message)
    }

    private fun encode(firstByte: Byte, data: ByteArray): String
    {
        val typedData = byteArrayOf(firstByte) + data
        val bits = makeBitSet(typedData)

        val script = AlphanumericScript()
        val result = script.encode(bits)

        return result
    }

    fun decode(ciphertext: String): DecodeResult?
    {
        val script = AlphanumericScript()
        val bits = script.decode(ciphertext)
        val data = bits.toByteArray()

        val type = data[0]
        val payload = data.slice(1..data.lastIndex).toByteArray()

        val result = String(payload)

        when (type)
        {
            0.toByte() -> return DecodeResult(KeyOrMessage.Key, payload)
            1.toByte() -> return DecodeResult(KeyOrMessage.EncryptedMessage, payload)
        }

        return null
    }
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
