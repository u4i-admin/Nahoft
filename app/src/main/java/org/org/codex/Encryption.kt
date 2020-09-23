package org.org.codex

import android.content.Context
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.org.codex.PersistenceEncryption.Companion.masterKeyAlias
import org.org.codex.PersistenceEncryption.Companion.sharedPrefFilename
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.*
import java.security.spec.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec

// Note: The AndroidKeystore does not support ECDH key agreement between EC keys.
// The secure enclave does not appear to support EC keys at all, at this time.
// Therefore, we store keys in the EncryptedSharedPreferences instead of the KeyStore.
// This can be revised when the AndroidKeystore supports the required functionality.
class Encryption(context: Context) {

    private val encryptionAlgorithm = "ChaCha20"
    private val paddingType = "PKCS1Padding"
    private val blockingMode = "NONE"
    private val ecGenParameterSpecName = "secp256r1"
    private val keySize = 256

    // Encrypted Shared Preferences
    val privateKeyPreferencesKey = "NahoftPrivateKey"
    val publicKeyPreferencesKey = "NahoftPublicKey"

    val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        sharedPrefFilename,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun generateKeypair(): KeyPair {

        // Generate ephemeral keypair and store using Android's KeyStore
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        // Set our key properties
        val keyGenParameterSpec = ECGenParameterSpec(ecGenParameterSpecName)
        keyPairGenerator.initialize(keyGenParameterSpec)

        val keyPair = keyPairGenerator.generateKeyPair()

        print("Public key format:")
        print(keyPair.public.format)
        print("Public key algorithm: ")
        print(keyPair.public.algorithm)
        print("Encoded public key size: ")
        print(keyPair.public.encoded.size)
        println("Generated a keypair, the public key hex is: ")
        println(keyPair.public.encoded.toHexString())

        // Save the keys to EncryptedSharedPreferences
        encryptedSharedPreferences
            .edit()
            .putString(privateKeyPreferencesKey, keyPair.private.encoded.toHexString())
            .putString(publicKeyPreferencesKey, keyPair.public.encoded.toHexString())
            .apply()

        return  keyPair
    }

    fun createTestKeypair(): KeyPair {
        return ensureKeysExist()
    }

    // Return the ECPublicKey from encoded raw bytes
    // The format of the encodedPublicKey is X.509
    fun publicKeyFromByteArray(encodedPublicKey: ByteArray): PublicKey? {

        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val keySpec = X509EncodedKeySpec(encodedPublicKey)

        return keyFactory.generatePublic(keySpec)
    }

    fun privateKeyFromByteArray(encodedPublicKey: ByteArray): PrivateKey? {

        val keySpec = PKCS8EncodedKeySpec(encodedPublicKey)
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        return keyFactory.generatePrivate(keySpec)
    }


    // Return KeyPair if found in EncryptedSharedPreferences
    fun keysExist(): KeyPair? {

        var privateKey: PrivateKey? = null
        var publicKey: PublicKey? = null

        // Private Key
        val privateKeyHex = encryptedSharedPreferences.getString(privateKeyPreferencesKey, null)

        if (privateKeyHex != null) {
            try {
                val privateKeyEncoded = privateKeyHex.hexStringToByteArray()
                privateKey = privateKeyFromByteArray(privateKeyEncoded)
            } catch (error: Exception) {
                println("Failed to decode key string into ByteArray: $error")
                return null
            }
        }

        // Public Key
        val publicKeyHex = encryptedSharedPreferences.getString(publicKeyPreferencesKey, null)

        if (publicKeyHex != null) {
            try {
                val publicKeyEncoded = publicKeyHex.hexStringToByteArray()
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

    fun encrypt(encodedPublicKey: ByteArray, plaintext: String): ByteArray? {

        val publicKey = publicKeyFromByteArray(encodedPublicKey)

        if (publicKey != null) {
            val derivedKey = getDerivedKey(publicKey)

            if (derivedKey != null) {
                val cipher = getCipher()

                if (cipher != null) {
                    cipher.init(Cipher.ENCRYPT_MODE, derivedKey)
                    return cipher.doFinal(plaintext.toByteArray())
                }
            }
        } else {
            print("Failed to encrypt a message, Friend's public key could not be decoded.")
            return null
        }

        return null
    }

    fun decrypt(friendPublicKey: PublicKey, ciphertext: ByteArray): String? {

        val derivedKey = getDerivedKey(friendPublicKey)

        if (derivedKey != null) {
            val cipher = getCipher()

            if (cipher != null) {
                cipher.init(Cipher.DECRYPT_MODE, derivedKey)
                return String(cipher.doFinal(ciphertext))
            }
        }

        return null
    }

    fun getDerivedKey(friendPublicKey: PublicKey): Key? {
        // Perform key agreement
        val keyPair = keysExist()

        if (keyPair != null) {
            val keyAgreement: KeyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(keyPair.private)
            keyAgreement.doPhase(friendPublicKey, true)

            // Read shared secret
            val sharedSecret: ByteArray = keyAgreement.generateSecret()

            // Derive a key from the shared secret and both public keys
            val hash = MessageDigest.getInstance("SHA-256")
            hash.update(sharedSecret)

            // Simple deterministic ordering
            val keys: List<ByteBuffer> = Arrays.asList(ByteBuffer.wrap(keyPair.public.encoded), ByteBuffer.wrap(friendPublicKey.encoded))
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

@ExperimentalUnsignedTypes // just to make it clear that the experimental unsigned types are used
fun ByteArray.toHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }

private val HEX_CHARS = "0123456789ABCDEF"
fun String.hexStringToByteArray() : ByteArray {

    val result = ByteArray(length / 2)

    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i]);
        val secondIndex = HEX_CHARS.indexOf(this[i + 1]);

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }

    return result
}