package org.operatorfoundation.codex

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

var ivKey = "ivKey"
var saltKey = "saltKey"
var encryptedDataKey = "encrypted"
var keystoreProvider = "AndroidKeyStore"


class PersistenceEncryption {

    private val cipherTransformation = "AES/GCM/NoPadding"
    private val keyAlias = "PersistenceKeyAlias"
    private val tagLength = 128 // in bits

    init {
        createKeyStoreKey()
    }

    private fun createKeyStoreKey() {

        val keyStore = KeyStore.getInstance(keystoreProvider)
        keyStore.load(null)

        // Check to see if a key with this alias already exists
        if (keyStore.containsAlias(keyAlias)) {
            print("Persistence key already exists.")
            return
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keystoreProvider)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            // use a new IV each time, 4 different ciphertext for same plaintext on each call
            .setRandomizedEncryptionRequired(true)
            // invalidated if lock screen is disabled
            // Enabling the screen lock requirements will revoke keys
            // as soon as the user removes or changes the lock screen pin or passcode.
            // ToDo: Considering whether we want this
            // .setUserAuthenticationRequired(true)
            // This makes the key unavailable once the device has detected it is no longer on the person.
            .setUserAuthenticationValidWhileOnBody(true)
            /*
            If your app needs to encrypt data in the background,
            you should not use time-bound keys or require that the device is unlocked,
            as you will not be able to accomplish this without a user present.
            */
            .setUnlockedDeviceRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun writeEncryptedFile(file: File, contents: ByteArray, context: Context) {

        if (file.exists()) { file.delete() }

        // Although you can define your own key generation parameter specification, it's
        // recommended that you use the value specified here.
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)

        // Creates a file with this name. Note that the file name cannot contain
        // path separators.
        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().apply {

            write(contents)
            flush()
            close()
        }
    }

    fun readEncryptedFile(file: File, context: Context): ByteArray {

        // Although you can define your own key generation parameter specification, it's
        // recommended that you use the value specified here.
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val inputStream = encryptedFile.openFileInput()
        val byteArrayOutputStream = ByteArrayOutputStream()
        var nextByte: Int = inputStream.read()
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte)
            nextByte = inputStream.read()
        }

        return byteArrayOutputStream.toByteArray()
    }

    fun encrypt(dataToEncrypt: ByteArray): HashMap<String, ByteArray> {

        val map = HashMap<String, ByteArray>()

        // Retrieve the Key from the KeyStore
        val keyStore = KeyStore.getInstance(keystoreProvider)
        keyStore.load(null)

        val secretKeyEntry = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey

        // Encrypt the data
        val cipher = Cipher.getInstance(cipherTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val ivBytes = cipher.iv
        val encryptionBytes = cipher.doFinal(dataToEncrypt)

        // FIXME: keys should also be encrypted
        map[ivKey] = ivBytes
        map[encryptedDataKey] = encryptionBytes

        return map
    }

    fun decrypt(map: HashMap<String, ByteArray>): ByteArray? {

        var decryptedBytes: ByteArray? = null

        // Retrieve the key from the KeyStore
        val keyStore = KeyStore.getInstance(keystoreProvider)
        keyStore.load(null)

        val secretKeyEntry = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey

        // FIXME Encrypted Keys
        // Extract the needed info from the map
        if (map.containsKey(ivKey) && map.containsKey(encryptedDataKey)) {

            val ivBytes = map[ivKey]
            val encryptedBytes = map[encryptedDataKey]
            if (ivBytes is ByteArray && encryptedBytes is ByteArray) {

                // Decrypt the data
                val cipher = Cipher.getInstance(cipherTransformation)
                val algorithmParameterSpec = GCMParameterSpec(tagLength, ivBytes)

                cipher.init(Cipher.DECRYPT_MODE, secretKey, algorithmParameterSpec)
                decryptedBytes = cipher.doFinal(encryptedBytes)
            }
        } else {
            print("Cannot decrypt the map received, it does not contain the expected keys.")
        }

        return decryptedBytes
    }
}