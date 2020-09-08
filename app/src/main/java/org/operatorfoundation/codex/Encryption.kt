package org.operatorfoundation.codex

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.RSAKeyGenParameterSpec.F4
import javax.crypto.Cipher
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

    private val aesTransformation = "AES/GCM/NoPadding"
    private val keystoreProvider = "AndroidKeyStore"
    private val typeRSA = "RSA"
    private val paddingType = "PKCS1Padding"
    private val blockingMode = "NONE"
    private val keySize = 1024
    val keyAlias = "operatorFoundationNahoftKey"

    fun createKeypair() {

        // Check to see if a key with this alias already exists
        if (!isSigningKey(keyAlias)) {

            // Use the RSA Algorithm and store using Android's KeyStore
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                keystoreProvider
            )

            // Set our key properties
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )

            // TODO("Decide what we actually want to use here.")
            keyPairGenerator.initialize(
                keyGenParameterSpec
                    .setAlgorithmParameterSpec(RSAKeyGenParameterSpec(keySize, F4))
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setDigests(
                        KeyProperties.DIGEST_SHA256,
                        KeyProperties.DIGEST_SHA384,
                        KeyProperties.DIGEST_SHA512
                    )
                    // Only permit the private key to be used if the user authenticated
                    // within the last five minutes.
                    .setUserAuthenticationRequired(true)
                    .build()
            )

            val keyPair = keyPairGenerator.generateKeyPair()
            print("Generated a keypair, the public key is: ")
            print(keyPair.public.toString())
        }

    }

    // Return true if a key using our alias already exists
    fun isSigningKey(alias: String?): Boolean {
        val keyStore = KeyStore.getInstance(keystoreProvider)
        keyStore.load(null)

        return keyStore.containsAlias(alias)
    }

    // Returns the private key as a string or null
    fun getSigningKey(alias: String?): String? {
        val privateKey = getPrivateKeyEntry(keyAlias)

        privateKey?.let {
            val cert: Certificate = privateKey.certificate ?: return null
            return Base64.encodeToString(cert.getEncoded(), Base64.NO_WRAP)
        }

        return null
    }

    private fun getCipher(): Cipher? {
        return Cipher.getInstance(
            java.lang.String.format(
                "%s/%s/%s",
                typeRSA,
                blockingMode,
                paddingType
            )
        )
    }

    private fun getPrivateKeyEntry(alias: String): KeyStore.PrivateKeyEntry? {
            val keyStore = KeyStore
                .getInstance(keystoreProvider)
            keyStore.load(null)
            val entry = keyStore.getEntry(alias, null)
            if (entry == null) {
                print("No key found under alias: " + keyAlias)
                return null
            }
            if (entry !is KeyStore.PrivateKeyEntry) {
                print("Not an instance of a PrivateKeyEntry")
                return null
            }

            return entry
    }

    fun encrypt(alias: String?, plaintext: String): String? {
            val publicKey: PublicKey = getPrivateKeyEntry(alias!!)!!.certificate.publicKey
            val cipher = getCipher()

            cipher?.let {
                cipher.init(Cipher.ENCRYPT_MODE, publicKey)
                return Base64.encodeToString(
                    cipher!!.doFinal(plaintext.toByteArray()),
                    Base64.NO_WRAP
                )
            }

        return null
    }

    fun decrypt(alias: String?, ciphertext: String?): String? {
        val privateKey: PrivateKey = getPrivateKeyEntry(alias!!)!!.privateKey
        val cipher = getCipher()

        cipher?.let {
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            return String(cipher!!.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)))
        }

        return null
    }


//    fun encryptMessage(plaintext: String, key: SecretKey): ByteArray {
//        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
//        return encryptData(plaintextBytes, key)
//    }
//
//    fun encryptData(plaintext: ByteArray, key: SecretKey): ByteArray {
//        val cipher = Cipher.getInstance(aesTransformation)
//        cipher.init(Cipher.ENCRYPT_MODE, key)
//        val ciphertext: ByteArray = cipher.doFinal(plaintext)
//
//        // iv (initialization vector) is our nonce
//        val iv: ByteArray = cipher.iv
//
//        return iv+ciphertext
//    }
//
//    fun decryptMessage(ciphertext: ByteArray, key: SecretKey): String {
//        val cipher = Cipher.getInstance(aesTransformation)
//        val ivSize = 12
//        val ivSlice = ciphertext.sliceArray(0..11)
//        val ivSpec = IvParameterSpec(ivSlice)
//        val cipherMessage = ciphertext.sliceArray(12..ciphertext.size - 1)
//        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
//        val plaintext: ByteArray = cipher.doFinal(cipherMessage)
//        val message = String(plaintext, Charsets.UTF_8)
//
//        return message
//    }

}