package org.operatorfoundation.codex

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KotlinCryptoExample {

    // transformations:
    // ChaCha20/Poly1305/NoPadding
    // AES/GCM/NoPadding
    // AES_128/GCM/NoPadding
    // AES_256/GCM/NoPadding

    fun encryptMessage(plaintext: ByteArray): ByteArray {
        val keygen = KeyGenerator.getInstance("AES")
        keygen.init(256)
        val key: SecretKey = keygen.generateKey()
        val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext: ByteArray = cipher.doFinal(plaintext)
        val iv: ByteArray = cipher.iv

        return ciphertext
    }

    fun decryptMessage(ciphertext: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val plaintext: ByteArray = cipher.doFinal(ciphertext)

        return plaintext
    }
}