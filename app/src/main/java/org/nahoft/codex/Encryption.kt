package org.nahoft.codex

import org.libsodium.jni.Sodium
import org.libsodium.jni.SodiumConstants
import org.libsodium.jni.crypto.Random as SodiumRandom
import org.libsodium.jni.keys.KeyPair as SodiumKeyPair
import org.libsodium.jni.keys.PrivateKey as SodiumPrivateKey
import org.libsodium.jni.keys.PublicKey as SodiumPublicKey
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.publicKeyPreferencesKey

// Note: The AndroidKeystore does not support ECDH key agreement between EC keys.
// The secure enclave does not appear to support EC keys at all, at this time.
// Therefore, we store keys in the EncryptedSharedPreferences instead of the KeyStore.
// This can be revised when the AndroidKeystore supports the required functionality.
class Encryption {
    // Encrypted Shared Preferences
    private val privateKeyPreferencesKey = "NahoftPrivateKey"

    // Generate a new keypair for this device and store it in EncryptedSharedPreferences
    private fun generateKeypair(): SodiumPublicKey {
        val seed = SodiumRandom().randomBytes(SodiumConstants.SECRETKEY_BYTES)
        val keyPair = SodiumKeyPair(seed)

        // Save the keys to EncryptedSharedPreferences
        Persist.encryptedSharedPreferences
            .edit()
            .putString(publicKeyPreferencesKey, keyPair.publicKey.toString())
            .putString(privateKeyPreferencesKey, keyPair.privateKey.toString())
            .apply()

        return keyPair.publicKey
    }

    // Generate a new keypair for this device and store it in EncryptedSharedPreferences
    private fun loadPublicKey(): SodiumPublicKey? {

        val publicKeyHex = Persist.encryptedSharedPreferences.getString(
            publicKeyPreferencesKey,
            null
        )

        if (publicKeyHex == null) {
            return null
        }

        return SodiumPublicKey(publicKeyHex)
    }

    fun ensureKeysExist(): SodiumPublicKey
    {
        return loadPublicKey() ?: generateKeypair()
    }

    @Throws(SecurityException::class)
    fun encrypt(encodedPublicKey: ByteArray, plaintext: String): ByteArray
    {
        val sRandom = SodiumRandom()
        val plaintTextBytes = plaintext.encodeToByteArray()
        val nonce = sRandom.randomBytes(SodiumConstants.NONCE_BYTES)
        val friendPublicKey = SodiumPublicKey(encodedPublicKey)

        // Make sure keys exist
        ensureKeysExist()
        var privateKeyHex = Persist.encryptedSharedPreferences.getString(privateKeyPreferencesKey, null)
        if (privateKeyHex == null) throw SecurityException("Private Key Not Found")
        val privateKey = SodiumPrivateKey(privateKeyHex)
        val privateKeyBytes = privateKey.toBytes()

        try
        {
            // Uses XSalsa20Poly1305
            // Returns nonce + ciphertext
            val result = SodiumWrapper().encrypt(
                plaintTextBytes,
                nonce,
                friendPublicKey.toBytes(),
                privateKeyBytes
            )

            // Key Hygiene
            Sodium.randombytes(privateKeyBytes, privateKeyBytes.size)
            privateKeyHex = null
            System.gc()
            System.runFinalization()

            if (result.size <= nonce.size)
            {
                throw SecurityException("Failed to encrypt the message.")
            }
            else
            {
                return result
            }

        }
        catch (exception: SecurityException)
        {
            throw exception
        }
    }

    @Throws(SecurityException::class)
    fun decrypt(friendPublicKey: SodiumPublicKey, ciphertext: ByteArray): String
    {
        if (!Persist.accessIsAllowed())
        { return "" }
        else
        {
            loadPublicKey() ?: throw SecurityException()
            var privateKeyHex = Persist.encryptedSharedPreferences.getString(privateKeyPreferencesKey, null)
            if (privateKeyHex == null) throw SecurityException("Private Key Not Found")
            val privateKey = SodiumPrivateKey(privateKeyHex)
            val privateKeyBytes = privateKey.toBytes()

            try
            {
                val result = SodiumWrapper().decrypt(
                    ciphertext,
                    friendPublicKey.toBytes(),
                    privateKeyBytes
                )
                return String(result)
            }
            catch (exception: SecurityException)
            {
                throw exception
            }
            finally 
            {
                // Key Hygiene
                Sodium.randombytes(privateKeyBytes, privateKeyBytes.size)
                privateKeyHex = null
                System.gc()
                System.runFinalization()
            }
        }
    }
}

class Keys(val privateKey: SodiumPrivateKey, val publicKey: SodiumPublicKey)
