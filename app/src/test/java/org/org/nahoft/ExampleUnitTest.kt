package org.org.nahoft

import org.junit.Test

import org.junit.Assert.*
import org.org.codex.Codex
import org.org.codex.Encryption

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun codexTest() {
        val codex = Codex()
        val encoded = codex.encode("test")
        val decoded = codex.decode(encoded)

        println()
        println("test")
        println(encoded)
        println(decoded)
    }

    @Test
    fun encryptionTest() {
    }
}