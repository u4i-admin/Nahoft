package org.nahoft.nahoft

import org.junit.Test

import org.junit.Assert.*
import org.libsodium.jni.SodiumConstants
import org.libsodium.jni.crypto.Box
import org.libsodium.jni.crypto.Random
import org.libsodium.jni.keys.KeyPair

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class EncryptionUnitTest {
    @Test
    fun encrypt_decrypt() {
        val plaintext = "a"

        val seed1 = Random().randomBytes(SodiumConstants.SECRETKEY_BYTES)
        val keyPair1 = KeyPair(seed1)

        val seed2 = Random().randomBytes(SodiumConstants.SECRETKEY_BYTES)
        val keyPair2 = KeyPair(seed2)

        val box = Box(keyPair2.publicKey, keyPair1.privateKey)
        val nonce = Random().randomBytes(SodiumConstants.NONCE_BYTES)
        val ciphertext = box.encrypt(nonce, plaintext.toByteArray())
        val encrypted = nonce + ciphertext

        val box2 = Box(keyPair1.publicKey, keyPair2.privateKey)
        val nonce2 = encrypted.slice(0..SodiumConstants.NONCE_BYTES - 1).toByteArray()
        val ciphertext2 =
            encrypted.slice(SodiumConstants.NONCE_BYTES..ciphertext.lastIndex).toByteArray()

        val plaintext2 = box2.decrypt(nonce2, ciphertext2).toString()

        assertEquals(plaintext, plaintext2)
    }
}