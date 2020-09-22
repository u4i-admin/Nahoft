package org.org.codex

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.*
import java.security.spec.ECPublicKeySpec
import java.security.spec.EncodedKeySpec
import java.security.spec.KeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec

object Encryption {

    private val keystoreProvider = "AndroidKeyStore"
    private val encryptionAlgorithm = "ChaCha20"
    private val paddingType = "PKCS1Padding"
    private val blockingMode = "NONE"
    private val keySize = 256
    val keyAlias = "nahoftKey"
    val testKeyAlias = "keyForTesting"

    fun createKeypair() {

        // Check to see if a key with this alias already exists
        if (!isSigningKey(keyAlias)) {
            // Generate ephemeral keypair and store using Android's KeyStore
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                keystoreProvider
            )

            // Set our key properties
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(keySize)
                .build()

            keyPairGenerator.initialize(keyGenParameterSpec)
            val keyPair = keyPairGenerator.generateKeyPair()

            print("Generated a keypair, the public key is: ")
            print(keyPair.public.toString())
        }
    }

    fun createTestKeypair(): ByteArray? {

        // Check to see if a key with this alias already exists
        if (!isSigningKey(testKeyAlias)) {

            // Generate ephemeral keypair and store using Android's KeyStore
            val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, keystoreProvider)

            // Set our key properties
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(keySize)
                .build()

            keyPairGenerator.initialize(keyGenParameterSpec)

            val keyPair = keyPairGenerator.generateKeyPair()

            print("Generated a keypair, the public key is: ")
            print(keyPair.public.toString())
            print(">>>Encoded format for public key is ${keyPair.public.format}")

            return  keyPair.public.encoded
        } else {
            return getPrivateKeyEntry(testKeyAlias)?.certificate?.publicKey?.encoded
        }
    }

    fun publicKeyFromByteArray(encodedPublicKey: ByteArray): PublicKey? {

        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC, keystoreProvider)

        val keySpec = X509EncodedKeySpec(encodedPublicKey)

        return keyFactory.generatePublic(keySpec)
    }


    // Return true if a key using our alias already exists
    fun isSigningKey(alias: String?): Boolean {
        val keyStore = KeyStore.getInstance(keystoreProvider)
        keyStore.load(null)

        return keyStore.containsAlias(alias)
    }

    private fun getCipher(): Cipher? {
        return Cipher.getInstance(
            java.lang.String.format(
                "%s/%s/%s",
                encryptionAlgorithm,
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

    fun encrypt(encodedPublicKey: ByteArray, plaintext: String): ByteArray? {

        val publicKey = publicKeyFromByteArray(encodedPublicKey)

        publicKey?.let {
            val derivedKey = getDerivedKey(publicKey)
            derivedKey?.let {
                val cipher = getCipher()

                cipher?.let {
                    cipher.init(Cipher.ENCRYPT_MODE, derivedKey)
                    return cipher.doFinal(plaintext.toByteArray())
                }
            }
        }

        return null
    }

    fun decrypt(friendPublicKey: PublicKey, ciphertext: ByteArray): String? {
        val derivedKey = getDerivedKey(friendPublicKey)
        derivedKey?.let {
            val cipher = getCipher()

            cipher?.let {
                cipher.init(Cipher.DECRYPT_MODE, derivedKey)
                return String(cipher.doFinal(ciphertext))
            }

            return null
        }

        return null
    }

    fun getDerivedKey(friendPublicKey: PublicKey): Key? {
        // Perform key agreement
        val privateKeyEntry = getPrivateKeyEntry(keyAlias)
        privateKeyEntry?.let {
            val ourPublicKey = privateKeyEntry.certificate.publicKey
            val keyAgreement: KeyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(privateKeyEntry.privateKey)
            keyAgreement.doPhase(friendPublicKey, true)

            // Read shared secret
            val sharedSecret: ByteArray = keyAgreement.generateSecret()

            // Derive a key from the shared secret and both public keys
            val hash = MessageDigest.getInstance("SHA-256")
            hash.update(sharedSecret)

            // Simple deterministic ordering
            val keys: List<ByteBuffer> = Arrays.asList(ByteBuffer.wrap(ourPublicKey.encoded), ByteBuffer.wrap(friendPublicKey.encoded))
            Collections.sort(keys)
            hash.update(keys[0])
            hash.update(keys[1])
            val derivedKeyBytes = hash.digest()
            val derivedKey: Key = SecretKeySpec(derivedKeyBytes, encryptionAlgorithm)

            return derivedKey
        }

        return null
    }


}