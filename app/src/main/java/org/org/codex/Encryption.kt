package org.org.codex

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.security.*
import java.security.cert.CertificateFactory
import java.security.spec.ECGenParameterSpec
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
    val keyPassword = "nahoft"
    val privateKeyAlias = "nahoftPrivateKey"
    val publicKeyAlias = "nahoftPublicKey"
    val testKeyAlias = "keyForTesting"

    fun generateKeypair(): KeyPair {

        // Generate ephemeral keypair and store using Android's KeyStore
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        // Set our key properties
        val keyGenParameterSpec = ECGenParameterSpec("secp256r1")
        keyPairGenerator.initialize(keyGenParameterSpec)

        val keyPair = keyPairGenerator.generateKeyPair()

        println("Generated a keypair, the public key is: ")
        println(keyPair.public.toString())

        return  keyPair
    }

    fun createTestKeypair(): KeyPair {
        // Generate ephemeral keypair and store using Android's KeyStore
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        // Set our key properties
        val keyGenParameterSpec = ECGenParameterSpec("secp256r1")
        keyPairGenerator.initialize(keyGenParameterSpec)

        val keyPair = keyPairGenerator.generateKeyPair()

        println("Generated a keypair, the public key is: ")
        println(keyPair.public.toString())

        return  keyPair
    }

    fun publicKeyFromByteArray(encodedPublicKey: ByteArray): PublicKey? {

        val keySpec = X509EncodedKeySpec(encodedPublicKey)
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        return keyFactory.generatePublic(keySpec)
    }


    // Return true if a key using our alias already exists
//    fun keysExist(): Boolean {
//        val keyStore = KeyStore.getInstance(keystoreProvider)
//        keyStore.load(null)
//
//        return false
//    }
//
//    fun ensureKeysExist(): KeyPair {
//        if (!keysExist()) {
//            return generateKeypair()
//        }
//
//        // TODO: Get it from encrypted shared preferences
//        return null
//    }

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

    private fun getPrivateKey(alias: String): PrivateKey? {
        val keyStore = KeyStore
            .getInstance(keystoreProvider)

        keyStore.load(null)

        val key = keyStore.getKey(alias, keyPassword.toCharArray())

        if (key == null) {
            print("No key found under alias: " + privateKeyAlias)
            return null
        }

        if (key is PrivateKey) {
            return key
        } else {
            return null
        }
    }

    private fun getPublicKey(alias: String): PublicKey? {
        val keyStore = KeyStore
            .getInstance(keystoreProvider)

        keyStore.load(null)

        val key = keyStore.getKey(alias, keyPassword.toCharArray())

        if (key == null) {
            print("No key found under alias: " + publicKeyAlias)
            return null
        }

        if (key is PublicKey) {
            return key
        } else {
            return null
        }
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
        val privateKey = getPrivateKey(privateKeyAlias)
        val publicKey = getPublicKey(publicKeyAlias)
        privateKey?.let {
            publicKey?.let {
                val keyAgreement: KeyAgreement = KeyAgreement.getInstance("ECDH")
                keyAgreement.init(privateKey)
                keyAgreement.doPhase(friendPublicKey, true)

                // Read shared secret
                val sharedSecret: ByteArray = keyAgreement.generateSecret()

                // Derive a key from the shared secret and both public keys
                val hash = MessageDigest.getInstance("SHA-256")
                hash.update(sharedSecret)

                // Simple deterministic ordering
                val keys: List<ByteBuffer> = Arrays.asList(ByteBuffer.wrap(publicKey.encoded), ByteBuffer.wrap(friendPublicKey.encoded))
                Collections.sort(keys)
                hash.update(keys[0])
                hash.update(keys[1])
                val derivedKeyBytes = hash.digest()
                val derivedKey: Key = SecretKeySpec(derivedKeyBytes, encryptionAlgorithm)

                return derivedKey
            }
        }

        return null
    }


}