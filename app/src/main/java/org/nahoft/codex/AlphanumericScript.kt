package org.nahoft.codex

import java.math.BigInteger

val alphabet: Array<String> = arrayOf(
    "۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹",
    "ی",
    "ء", "أ", "ئ", "ؤ", "ا", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ", "د",
    "ذ", "ر", "ز", "ژ", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق",
    "ک", "گ", "ل", "م", "ن", "و", "ه"
)

class AlphanumericScript {
    fun checkAlphabet(): Boolean
    {
        for (letter in alphabet)
        {
            var index = -1
            var offset = 0

            for (letter2 in alphabet)
            {
                if (letter == letter2)
                {
                    if (index == -1)
                    {
                        index = offset
                    }
                    else
                    {
                        println("failure: " + index + " " + offset)
                        return false
                    }
                }

                offset += 1
            }
        }

        return true
    }

    fun encode(bytes: ByteArray): String
    {
        val byteDigits = bytesToDigits(bytes)
        println("bytes: " + byteDigits)

        val integer = digitsToBigInteger(byteDigits, 256)
        println("integer: " + integer)

        val digits = bigIntegerToDigits(integer, alphabet.size)
        println("digits: " + digits)

        return digitsToSymbols(digits)
    }

    fun decode(ciphertext: String): ByteArray
    {
        var result = byteArrayOf()

        val base = alphabet.size

        val digits = symbolsToDigits(ciphertext)
        println("digits: " + digits)

        val integer = digitsToBigInteger(digits, base)
        println("integer: " + integer)

        val byteDigits = bigIntegerToDigits(integer, 256)
        println("bytes: " + byteDigits)

        val bytes = digitsToBytes(byteDigits)

        return bytes
    }
}

fun digitsToSymbols(digits: List<Int>): String
{
    var result: String = String()

    for (digit in digits)
    {
        val symbol = alphabet[digit.toInt()]
        result = result + symbol
    }
    println("symbol: " + result)

    return result
}

fun symbolsToDigits(ciphertext: String): List<Int>
{
    var digits: List<Int> = listOf()
    for (offset in 0..ciphertext.lastIndex)
    {
        val symbol = Character.toString(ciphertext[offset])

        var foundIndex = -1
        for (index in 0..alphabet.lastIndex)
        {
            if (alphabet[index] == symbol)
            {
                foundIndex = index
                break
            }
        }

        if (foundIndex == -1)
        {
            println("Symbol not found")
            return emptyList()
        }

        val digit = foundIndex
        digits += digit
    }

    return digits
}

fun bigIntegerToDigits(integer: BigInteger, base: Int): List<Int>
{
    if (integer == BigInteger.ZERO)
    {
        return listOf(0)
    }

    var working = integer
    val bigBase = base.toBigInteger()

    var result: List<Int> = listOf()

    val numDigits = computeNumDigits(integer, base)

    val placeValues = generatePlaceValues(numDigits, base)
    // println("placeValues: " + placeValues)

    working = integer
    for (placeValue in placeValues)
    {
        val digit = working / placeValue
        working %= placeValue

        print(" " + placeValue + " * " + digit + " + ")

        result += digit.toInt()
    }

    println()

    while (result[0] == 0)
    {
        result = result.slice(1..result.size-1)
    }

    return result
}

fun digitsToBigInteger(digits: List<Int>, base: Int): BigInteger
{
    val bigBase = base.toBigInteger()

    val numDigits = digits.size

    val placeValues = generatePlaceValues(numDigits, base)
    // println("placeValues: " + placeValues)

    return computeInteger(placeValues, digits)
}

fun generatePlaceValues(numDigits: Int, base: Int): List<BigInteger>
{
    var placeValues: List<BigInteger> = listOf()
    for (index in 0..numDigits-1) {
        val place = (numDigits-1) - index
        val placeValue = generatePlaceValue(place, base)
        placeValues += placeValue
    }

    return placeValues
}

fun generatePlaceValue(place: Int, base: Int): BigInteger
{
    val bigBase = base.toBigInteger()
    val placeValue = bigBase.pow(place)
    return placeValue
}

fun computeInteger(placeValues: List<BigInteger>, digits: List<Int>): BigInteger
{
    val numDigits = digits.size
    var working = BigInteger.ZERO

    for (index in 0..numDigits-1) {
        val digit = digits[index].toBigInteger()
        val placeValue = placeValues[index]
        val value = digit * placeValue
        working += value

        print(" " + placeValue + " * " + digit + " + ")
    }

    return working
}

fun computeNumDigits(integer: BigInteger, base: Int): Int
{
    val bigBase = base.toBigInteger()
    if (integer < bigBase)
    {
        return 1
    }

    var place = 1
    var placeValue = bigBase
    while (integer > placeValue)
    {
        place += 1
        placeValue = generatePlaceValue(place, base)
    }

    if (integer == placeValue)
    {
        return place + 1
    }
    else
    {
        return place
    }
}

fun bytesToDigits(bytes: ByteArray): List<Int>
{
    var result: List<Int> = listOf()

    for (byte in bytes)
    {
        if (byte >= 0)
        {
            result += byte.toInt()
        }
        else
        {
            result += (-byte.toInt()) + 128
        }
    }

    return result
}

fun digitsToBytes(digits: List<Int>): ByteArray
{
    var result = byteArrayOf()

    for (digit in digits)
    {
        if (digit > 128)
        {
            result += (-(digit-128)).toByte()
        }
        else
        {
            result += digit.toByte()
        }
    }

    return result
}