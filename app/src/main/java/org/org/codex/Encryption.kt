package org.org.codex

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64.encodeToString
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.nio.ByteBuffer
import java.security.*
import java.security.cert.CertificateFactory
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec

class Encryption(context: Context) {

    private val keystoreProvider = "AndroidKeyStore"
    private val encryptionAlgorithm = "ChaCha20"
    private val paddingType = "PKCS1Padding"
    private val blockingMode = "NONE"
    private val keySize = 256
    private val keyPassword = "nahoft"
    private val privateKeyAlias = "nahoftPrivateKey"
    private val publicKeyAlias = "nahoftPublicKey"
    private val testKeyAlias = "keyForTesting"

    // Encrypted Shared Preferences
    val privateKeyPreferencesKey = "NahoftPrivateKey"
    val publicKeyPreferencesKey = "NahoftPublicKey"
    val sharedPreferencesFilename = "NahoftEncryptedPreferences"
    val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
    val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
    val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        sharedPreferencesFilename,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun generateKeypair(): KeyPair {

        // Generate ephemeral keypair and store using Android's KeyStore
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        // Set our key properties
        val keyGenParameterSpec = ECGenParameterSpec("secp256r1")
        keyPairGenerator.initialize(keyGenParameterSpec)

        val keyPair = keyPairGenerator.generateKeyPair()

        println("Generated a keypair, the public key is: ")
        println(keyPair.public.toString())

        // Save the keys to EncryptedSharedPreferences
        val encoder = Base64.getEncoder()
        val privateKeyBase64String = encoder.encodeToString(keyPair.private.encoded)
        val publicKeyBase64String = encoder.encodeToString(keyPair.public.encoded)

        encryptedSharedPreferences
            .edit()
            .putString(privateKeyPreferencesKey, privateKeyBase64String)
            .putString(publicKeyPreferencesKey, publicKeyBase64String)
            .apply()

        return  keyPair
    }

    fun createTestKeypair(): KeyPair {
        return ensureKeysExist()
    }

    fun publicKeyFromByteArray(encodedPublicKey: ByteArray): PublicKey? {

        val keySpec = X509EncodedKeySpec(encodedPublicKey)
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        return keyFactory.generatePublic(keySpec)
    }

    fun privateKeyFromByteArray(encodedPublicKey: ByteArray): PrivateKey? {

        val keySpec = X509EncodedKeySpec(encodedPublicKey)
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        return keyFactory.generatePrivate(keySpec)
    }


    // Return KeyPair if found in EncryptedSharedPreferences
    fun keysExist(): KeyPair? {
        val decoder = Base64.getDecoder()
        var privateKey: PrivateKey? = null
        var publicKey: PublicKey? = null

        // Private Key
        val privateKeyBase64String = encryptedSharedPreferences.getString(privateKeyPreferencesKey, null)
        privateKeyBase64String?.let {
            try {
                val privateKeyEncoded = decoder.decode(privateKeyBase64String)
                privateKey = privateKeyFromByteArray(privateKeyEncoded)
            } catch (error: Exception) {
                println("Failed to decode key string into ByteArray: $error")
                return null
            }
        }

        // Public Key
        val publicKeyBase64String = encryptedSharedPreferences.getString(publicKeyPreferencesKey, null)
        publicKeyBase64String?.let {
            try {
                val publicKeyEncoded = decoder.decode(publicKeyBase64String)
                publicKey = publicKeyFromByteArray(publicKeyEncoded)
            } catch (error: java.lang.Exception) {
                println("Failed to decode key string into ByteArray: $error")
                return null
            }
        }

        return KeyPair(publicKey, privateKey)
    }

    fun ensureKeysExist(): KeyPair {

        val maybeKeyPair = generateKeypair()

        if (maybeKeyPair != null) {
            return  maybeKeyPair
        }

        return generateKeypair()
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