package org.nahoft.codex

import java.math.BigInteger

open class BaseScript {
    fun checkAlphabet(): Boolean {
        for (letter in WordScript.wordList) {
            var index = -1
            var offset = 0

            for (letter2 in WordScript.wordList) {
                if (letter == letter2) {
                    if (index == -1) {
                        index = offset
                    } else {
                        println("failure: " + index + " " + offset)
                        return false
                    }
                }

                offset += 1
            }
        }

        return true
    }

    // takes a string of words with spaces in them, breaks them up into individual word
    // removing the spaces, then looks up each word in word list and gives the index and
    // returns a list of the index
    fun symbolsToDigits(ciphertext: String): List<Int> {
        var digits: List<Int> = listOf()
        val noSpace = ciphertext.split(" ")
        for (offset in 0..noSpace.lastIndex) {
            val word = noSpace[offset]

            var foundIndex = -1
            for (index in 0..WordScript.wordList.lastIndex) {
                if (WordScript.wordList[index] == word) {
                    foundIndex = index
                    break
                }
            }

            if (foundIndex == -1) {
//                println("Symbol not found")
                return emptyList()
            }

            val digit = foundIndex
            digits += digit
        }

        return digits
    }

    fun bigIntegerToDigits(integer: BigInteger, base: Int): List<Int> {
        if (integer == BigInteger.ZERO) {
            return listOf(0)
        }

        var working = integer
        val bigBase = base.toBigInteger()

        var result: List<Int> = listOf()

        val numDigits = computeNumDigits(integer, base)

        val placeValues = generatePlaceValues(numDigits, base)
        // println("placeValues: " + placeValues)

        working = integer
        for (placeValue in placeValues) {
            val digit = working / placeValue
            working %= placeValue

//            print(" " + placeValue + " * " + digit + " + ")

            result += digit.toInt()
        }

//        println()

        while (result[0] == 0) {
            result = result.slice(1..result.size - 1)
        }

        return result
    }

    fun digitsToBigInteger(digits: List<Int>, base: Int): BigInteger {
        val bigBase = base.toBigInteger()

        val numDigits = digits.size

        val placeValues = generatePlaceValues(numDigits, base)
        // println("placeValues: " + placeValues)

        return computeInteger(placeValues, digits)

    }

    fun bytesToDigits(bytes: ByteArray): List<Int> {
        var results: List<Int> = listOf()

        for (byte in bytes) {
            var result: Int
            result = byte.toUByte().toInt()

            assert(result >= 0)
            assert(result <= 255)
//            println("Input: " + byte)
//            println("Output: " + result)

            results += result
        }

        return results
    }

    fun digitsToBytes(digits: List<Int>): ByteArray {
        var result = byteArrayOf()

        for (digit in digits) {
            assert(digit >= 0)
            assert(digit <= 255)

            result += digit.toUByte().toByte()
        }

        return result
    }

    // takes a series of digits, looks it up in the word list and gives back a string of words
    fun digitsToSymbols(digits: List<Int>): String {
        var result: String = String()

        for (digit in digits) {
            val word = WordScript.wordList[digit.toInt()]
            result = result + word + " "
        }
//        println("symbol: " + result)

        return result.trim()
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
}