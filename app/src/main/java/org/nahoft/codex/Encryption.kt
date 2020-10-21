package org.nahoft.codex

import android.content.Context
import androidx.security.crypto.MasterKeys
import org.libsodium.jni.SodiumConstants
import org.libsodium.jni.crypto.Random
import org.libsodium.jni.keys.KeyPair
import org.libsodium.jni.keys.PrivateKey
import org.libsodium.jni.keys.PublicKey
import org.libsodium.jni.crypto.Box
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.publicKeyPreferencesKey
import java.lang.Exception

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
    fun loadKeypair(): Keys? {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

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

    fun ensureKeysExist(): Keys {
        val maybeKeyPair = loadKeypair()

        if (maybeKeyPair != null) {
            return  maybeKeyPair
        } else {
            return generateKeypair()
        }
    }

    fun encrypt(encodedPublicKey: ByteArray, plaintext: String): ByteArray? {
        val friendPublicKey = PublicKey(encodedPublicKey)
        val keypair = ensureKeysExist()

        if (friendPublicKey != null) {
            val box = Box(friendPublicKey, keypair.privateKey)

            if (box != null) {
//                val nonce = Random().randomBytes(SodiumConstants.NONCE_BYTES)
                val nonce = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)
                val ciphertext = box.encrypt(nonce, plaintext.toByteArray())

                println("your public key: " + friendPublicKey.toBytes().toList())
                println("my public key: " + keypair.publicKey.toBytes().toList())
                println("nonce: " + nonce.toList())
                println("ciphertext: " + ciphertext.toList())

                val nonceList = nonce.toList()
                val ciphertextList = ciphertext.toList()

                return nonce + ciphertext
            }
        } else {
            print("Failed to encrypt a message, Friend's public key could not be decoded.")
            return null
        }

        return null
    }

    fun decrypt(friendPublicKey: PublicKey, ciphertext: ByteArray): String? {
        val keypair = ensureKeysExist()
        val box = Box(friendPublicKey, keypair.privateKey)

        if (box != null) {
            val nonce = ciphertext.slice(0..SodiumConstants.NONCE_BYTES-1).toByteArray()
            val payload = ciphertext.slice(SodiumConstants.NONCE_BYTES..ciphertext.lastIndex).toByteArray()

            print("\nFriend Public Key: \n ${friendPublicKey.toBytes().toList()}")
            print("\nPublic Key: \n ${keypair.publicKey.toBytes().toList()}")
            print("\n Private Key: \n ${keypair.privateKey.toBytes().toList()}")
            print("\nNonce: \n ${nonce.toList()}\n")
            print("\nCiphertext: \n ${ciphertext.toList()}")
            println("your public key: " + friendPublicKey.toBytes().toList())
            println("my public key: " + keypair.publicKey.toBytes().toList())
            println("nonce: " + nonce.toList())
            println("ciphertext: " + payload.toList())

            try {
                val result = String(box.decrypt(nonce, payload))
                return result
            } catch (error: Exception) {
                return null
            }
        }

        return null
    }

    fun getBox(friendPublicKey: PublicKey): Box {
        // Perform key agreement
        val keypair = ensureKeysExist()

        return Box(friendPublicKey, keypair.privateKey)
    }
}

class Keys(val privateKey: PrivateKey, val publicKey: PublicKey)
class DerivedKeys(val encryptKey: ByteArray, val decryptKey: ByteArray)
