package org.nahoft.org.nahoft.swatch

@ExperimentalUnsignedTypes
fun bitsFromBytes(bytes: ByteArray): IntArray
{
    var result: IntArray = IntArray(bytes.size * 8)

    for (byteIndex in 0 until bytes.size)
    {
        val byte = bytes[byteIndex].toUByte()

        for (bitIndex in 0 until 8)
        {
            val arrayIndex = byteIndex * 8 + bitIndex
            val bit = byte and masks[bitIndex]

            if (bit == 0.toUByte())
            {
                result[arrayIndex] = 0
            }
            else
            {
                result[arrayIndex] = 1
            }
        }
    }

    return result
}

@ExperimentalUnsignedTypes
fun bytesFromBits(bits: List<Int>): ByteArray?
{
    if (bits.size % 8 != 0)
    {
        return null
    }

    var result = ByteArray(bits.size / 8)

    for (byteIndex in 0 until result.size)
    {
        var byte: UByte = 0.toUByte()

        for (bitIndex in 0 until 8)
        {
            val arrayIndex = (byteIndex * 8) + bitIndex
            val value = bits[arrayIndex]
            if (value == 0)
            {
                continue
            }
            else if (value == 1)
            {
                val maskValue: UByte = masks[bitIndex]
                byte = (byte + maskValue).toUByte()
            }
            else
            {
                return null
            }
        }

        result[byteIndex] = byte.toByte()
    }

    return result
}

@ExperimentalUnsignedTypes
val masks: List<UByte> = listOf(
    128.toUByte(),
    64.toUByte(),
    32.toUByte(),
    16.toUByte(),
    8.toUByte(),
    4.toUByte(),
    2.toUByte(),
    1.toUByte()
)
