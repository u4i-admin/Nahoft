package org.operatorfoundation.nahoft

import org.junit.Test

import org.junit.Assert.*
import org.nahoft.codex.*
import kotlin.random.Random

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CodexUnitTest {
    @Test
    fun alphanumeric_checkAlphabet() {
        val codex = AlphanumericScript()
        val success = codex.checkAlphabet()
        assertTrue(success)
    }

    @Test
    fun alphanumeric_encode() {
        val input = 1.toByte()
        val codex = Codex()
        val output = codex.encode(input, byteArrayOf())
        println("output: " + output)
    }

    @Test
    fun alphanumeric_encode_decode() {
        val input = 1.toByte()
        val codex = Codex()
        val output = codex.encode(input, byteArrayOf())
        println("output: " + output)
        val decoded = codex.decode(output)
        println("decoded: " + decoded)
        assertEquals(KeyOrMessage.Key, decoded!!.type)
    }

    @Test
    fun alphanumeric_encodeKey_decode() {
        val key = byteArrayOf(5)
        val codex = Codex()
        val output = codex.encodeKey(key)
        val decoded = codex.decode(output)
        assertEquals(KeyOrMessage.Key, decoded!!.type)
        assertArrayEquals(key, decoded!!.payload)
    }

    @Test
    fun alphanumeric_encodeKey_decode_2() {
        val key = byteArrayOf(5, 6)
        val codex = Codex()
        val output = codex.encodeKey(key)
        val decoded = codex.decode(output)
        assertArrayEquals(key, decoded!!.payload)
    }

    @Test
    fun alphanumeric_digits() {
        val bytes = byteArrayOf(1, 1, 2, 3)
        val byteDigits = bytesToDigits(bytes)
        println("byteDigits: " + byteDigits)

        val integer = digitsToBigInteger(byteDigits, 256)
        println("integer: " + integer)

        val digits = bigIntegerToDigits(integer, 52)
        println("digits: " + digits)

        val integer2 = digitsToBigInteger(digits, 52)
        println("integer2: " + integer2)
        assertEquals(integer, integer2)

        val digits2 = bigIntegerToDigits(integer2, 256)
        println("digits2: " + digits2)
    }

    @Test
    fun alphanumeric_encodeKey_decode_3() {
        val key = byteArrayOf(1, 2, 3)
        val codex = Codex()
        val output = codex.encodeKey(key)
        val decoded = codex.decode(output)
        assertArrayEquals(key, decoded!!.payload)
    }

    @Test
    fun alphanumeric_encodeKey_decode_10() {
        val key = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val codex = Codex()
        val output = codex.encodeKey(key)
        val decoded = codex.decode(output)
        assertArrayEquals(key, decoded!!.payload)
    }

    @Test
    fun alphanumeric_encodeKey_decode_11() {
        val key = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        val codex = Codex()
        val output = codex.encodeKey(key)
        val decoded = codex.decode(output)
        assertArrayEquals(key, decoded!!.payload)
    }

    @Test
    fun alphanumeric_encodeKey_decode_32() {
        val key = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32)
        val codex = Codex()
        val output = codex.encodeKey(key)
        val decoded = codex.decode(output)
        assertArrayEquals(key, decoded!!.payload)
    }

    @Test
    fun alphanumeric_encodeKey_decode_32_random() {
        val key = ByteArray(32)
        Random.nextBytes(key)
        val codex = Codex()
        val output = codex.encodeKey(key)
        val decoded = codex.decode(output)
        assertArrayEquals(key, decoded!!.payload)
    }

    @Test
    fun alphanumeric_encodeKey_decode_32_random100() {
        val codex = Codex()

        repeat(100)
        {
            val key = ByteArray(32)
            Random.nextBytes(key)
            val output = codex.encodeKey(key)
            val decoded = codex.decode(output)
            assertArrayEquals(key, decoded!!.payload)
        }
    }

    @Test
    fun bigInteger_Base2_0() {
        val input = 0.toBigInteger()
        val digits = bigIntegerToDigits(input, 2)
        print(digits)

        val result = digitsToBigInteger(digits, 2)
        assertEquals(input, result)
    }

    @Test
    fun bigInteger_Base2_1() {
        val input = 1.toBigInteger()
        val digits = bigIntegerToDigits(input, 2)
        print(digits)

        val result = digitsToBigInteger(digits, 2)
        assertEquals(input, result)
    }

    @Test
    fun bigInteger_Base2_2() {
        val input = 2.toBigInteger()
        val digits = bigIntegerToDigits(input, 2)
        print(digits)

        val result = digitsToBigInteger(digits, 2)
        assertEquals(input, result)
    }

    @Test
    fun bigInteger_Base2_3() {
        val input = 3.toBigInteger()
        val digits = bigIntegerToDigits(input, 2)
        print(digits)

        val result = digitsToBigInteger(digits, 2)
        assertEquals(input, result)
    }

    @Test
    fun bigInteger_Base10_100() {
        val input = 100.toBigInteger()
        val digits = bigIntegerToDigits(input, 10)
        print(digits)

        val result = digitsToBigInteger(digits, 10)
        assertEquals(input, result)
    }

    @Test
    fun bigInteger_Base52_100() {
        val input = 100.toBigInteger()
        val digits = bigIntegerToDigits(input, 52)
        print(digits)

        val result = digitsToBigInteger(digits, 52)
        assertEquals(input, result)
    }
}