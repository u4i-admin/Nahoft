package org.operatorfoundation.codex

import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class Encryption {

    // TODO: Public Key Cryptography


    // Symmetric Key Cryptography
    // transformations:
    // ChaCha20/Poly1305/NoPadding
    // AES/GCM/NoPadding
    // AES_128/GCM/NoPadding
    // AES_256/GCM/NoPadding

    fun encryptMessage(plaintext: String, key: SecretKey): ByteArray {
        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        return encryptData(plaintextBytes, key)
    }

    fun encryptData(plaintext: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext: ByteArray = cipher.doFinal(plaintext)
        val iv: ByteArray = cipher.iv

        return iv+ciphertext
    }

    fun decryptMessage(ciphertext: ByteArray, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
        val ivSize = 12
        val ivSlice = ciphertext.sliceArray(0..11)
        val ivSpec = IvParameterSpec(ivSlice)
        val cipherMessage = ciphertext.sliceArray(12..ciphertext.size - 1)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val plaintext: ByteArray = cipher.doFinal(cipherMessage)
        val message = String(plaintext, Charsets.UTF_8)

        return message
    }

    fun createAES256Key(): SecretKey {
        val keygen = KeyGenerator.getInstance("AES")
        keygen.init(256)
        val key: SecretKey = keygen.generateKey()

        return key
    }
}