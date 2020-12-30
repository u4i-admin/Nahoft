package org.nahoft.codex

import android.content.Context
import org.libsodium.jni.SodiumConstants
import org.libsodium.jni.crypto.Random
import org.libsodium.jni.keys.KeyPair
import org.libsodium.jni.keys.PrivateKey
import org.libsodium.jni.keys.PublicKey
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.publicKeyPreferencesKey

// Note: The AndroidKeystore does not support ECDH key agreement between EC keys.
// The secure enclave does not appear to support EC keys at all, at this time.
// Therefore, we store keys in the EncryptedSharedPreferences instead of the KeyStore.
// This can be revised when the AndroidKeystore supports the required functionality.
class Encryption(val context: Context) {
    // Encrypted Shared Preferences
    private val privateKeyPreferencesKey = "NahoftPrivateKey"

    // Generate a new keypair for this device and store it in EncryptedSharedPreferences
    private fun generateKeypair(): Keys {
        val seed = Random().randomBytes(SodiumConstants.SECRETKEY_BYTES)
        val keyPair = KeyPair(seed)

        // Save the keys to EncryptedSharedPreferences
        Persist.encryptedSharedPreferences
            .edit()
            .putString(publicKeyPreferencesKey, keyPair.publicKey.toString())
            .putString(privateKeyPreferencesKey, keyPair.privateKey.toString())
            .apply()

        return Keys(keyPair.privateKey, keyPair.publicKey)
    }

    // Generate a new keypair for this device and store it in EncryptedSharedPreferences
    private fun loadKeypair(): Keys? {

        val privateKeyHex = Persist.encryptedSharedPreferences.getString(
            privateKeyPreferencesKey,
            null
        )
        val publicKeyHex = Persist.encryptedSharedPreferences.getString(
            publicKeyPreferencesKey,
            null
        )

        if (privateKeyHex == null || publicKeyHex == null) {
            return null
        }

        val publicKey = PublicKey(publicKeyHex)
        val privateKey = PrivateKey(privateKeyHex)

        return Keys(privateKey, publicKey)
    }

    fun ensureKeysExist(): Keys
    {
        return loadKeypair() ?: generateKeypair()
    }

    @Throws(SecurityException::class)
    fun encrypt(encodedPublicKey: ByteArray, plaintext: String): ByteArray
    {
        val plaintTextBytes = plaintext.encodeToByteArray()
        val nonce = Random().randomBytes(SodiumConstants.NONCE_BYTES)
        val friendPublicKey = PublicKey(encodedPublicKey)
        val privateKey = ensureKeysExist().privateKey

        try {
            // Uses XSalsa20Poly1305
            // Returns nonce + ciphertext
            val result = SodiumWrapper().encrypt(
                plaintTextBytes,
                nonce,
                friendPublicKey.toBytes(),
                privateKey.toBytes())

            if (result.size <= nonce.size) {
                throw SecurityException("Failed to encrypt the message.")
            } else {
                println("--------------Encrypted A Message")
                val hex = result.joinToString("") { String.format("%02X", (it.toInt() and 0xFF)) }
                println(hex)
                println("Encryption Result:" + result.size)
                return result
            }

        } catch (exception: SecurityException) {
            throw exception
        }
    }

    @Throws(SecurityException::class)
    fun decrypt(friendPublicKey: PublicKey, ciphertext: ByteArray): String
    {
        val keypair = ensureKeysExist()

        try {
            val result = SodiumWrapper().decrypt(ciphertext, friendPublicKey.toBytes(), keypair.privateKey.toBytes())
            return String(result)
        } catch (exception: SecurityException) {
            throw exception
        }
    }
}

class Keys(val privateKey: PrivateKey, val publicKey: PublicKey)
