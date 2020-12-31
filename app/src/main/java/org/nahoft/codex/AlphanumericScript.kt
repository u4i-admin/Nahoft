package org.nahoft.codex

import java.math.BigInteger

class AlphanumericScript: BaseScript() {
    companion object
    {
        val alphabet: Array<String> = arrayOf(
            "۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹",
            "ی",
            "ء", "أ", "ئ", "ؤ", "ا", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ", "د",
            "ذ", "ر", "ز", "ژ", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق",
            "ک", "گ", "ل", "م", "ن", "و", "ه"
        )
    }

    fun encode(bytes: ByteArray): String
    {
        val byteDigits = bytesToDigits(bytes)
//        println("bytes: " + byteDigits)

        val integer = digitsToBigInteger(byteDigits, 256)
//        println("integer: " + integer)

        val digits = bigIntegerToDigits(integer, alphabet.size)
//        println("digits: " + digits)

        return digitsToSymbols(digits)
    }

    fun decode(ciphertext: String): ByteArray
    {
        var result = byteArrayOf()

        val base = alphabet.size

        val digits = symbolsToDigits(ciphertext)
//        println("digits: " + digits)

        val integer = digitsToBigInteger(digits, base)
//        println("integer: " + integer)

        val byteDigits = bigIntegerToDigits(integer, 256)
//        println("bytes: " + byteDigits)

        val bytes = digitsToBytes(byteDigits)

        return bytes
    }
}
