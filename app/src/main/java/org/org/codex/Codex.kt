package org.org.codex

import java.security.Key
import java.util.*

enum class KeyOrMessage(val byte: Byte)
{
    Key(1),
    EncryptedMessage(2)
}

class DecodeResult(val type: KeyOrMessage, val payload: ByteArray)

class Codex {
    fun encodeKey(key: ByteArray): String
    {
        return encode(KeyOrMessage.Key.byte, key)
    }

    fun encodeEncryptedMessage(message: ByteArray): String
    {
        return encode(KeyOrMessage.EncryptedMessage.byte, message)
    }

    fun encode(firstByte: Byte, data: ByteArray): String
    {
        val typedData = byteArrayOf(firstByte) + data

        val script = AlphanumericScript()
        val result = script.encode(typedData)

        return result
    }

    fun decode(ciphertext: String): DecodeResult?
    {
        val script = AlphanumericScript()
        val data = script.decode(ciphertext)

        val type = data[0]
        val payload = data.slice(1..data.lastIndex).toByteArray()

        when (type)
        {
            KeyOrMessage.Key.byte -> return DecodeResult(KeyOrMessage.Key, payload)
            KeyOrMessage.EncryptedMessage.byte -> return DecodeResult(KeyOrMessage.EncryptedMessage, payload)
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
            val bit = (byte.toInt() shl bitIndex) shr (7 - bitIndex)
            val bool = bit != 0

            val resultIndex = (byteIndex * 8) + bitIndex

            result.set(resultIndex, bool)
        }
    }

    return result
}
