package org.nahoft.nahoft

import android.app.Application
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.nahoft.codex.PersistenceEncryption
import org.nahoft.nahoft.activities.LoginStatus
import org.simpleframework.xml.core.Persister
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import org.libsodium.jni.keys.PublicKey

class Persist {

    companion object {

        val publicKeyPreferencesKey = "NahoftPublicKey"
        val sharedPrefLoginStatusKey = "NahoftLoginStatus"
        val sharedPrefPasscodeKey = "NahoftPasscode"
        val sharedPrefSecondaryPasscodeKey = "NahoftSecondaryPasscode"
        val sharedPrefFilename = "NahoftEncryptedPreferences"
        val sharedPrefKeyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val masterKeyAlias = MasterKeys.getOrCreate(sharedPrefKeyGenParameterSpec)

        const val friendsFilename = "fData.xml"
        const val messagesFilename = "mData.xml"

        // Initialized by EnterPasscodeActivity(main)
        lateinit var status: LoginStatus
        lateinit var encryptedSharedPreferences: EncryptedSharedPreferences

        // Initialized by HomeActivity
        lateinit var friendsFile: File
        lateinit var messagesFile: File
        lateinit var app: Application

        var friendList = ArrayList<Friend>()
        var messageList = ArrayList<Message>()

        fun saveLoginStatus() {
            encryptedSharedPreferences
                .edit()
                .putString(sharedPrefLoginStatusKey, status.name)
                .apply()
        }

        fun updateFriend(context: Context, friendToUpdate: Friend, newStatus: FriendStatus, encodedPublicKey: ByteArray? = null) {

            val oldFriend = friendList.find { it.name == friendToUpdate.name }

            encodedPublicKey?.let {
                val publicKey = PublicKey(encodedPublicKey)
                if (publicKey == null) {
                    // Fail early instead of persisting a bad public key
                    return
                }
            }

            oldFriend?.let {
                oldFriend.status = newStatus
                encodedPublicKey?.let { oldFriend.publicKeyEncoded = encodedPublicKey }
            }

            saveFriendsToFile(context)
        }

        fun saveKey(key:String, value:String) {
            encryptedSharedPreferences
                .edit()
                .putString(key, value)
                .apply()
        }

        private fun deleteKey(key:String) {
            encryptedSharedPreferences
                .edit()
                .remove(key)
                .apply()
        }

        fun deletePasscode() {
            deleteKey(sharedPrefPasscodeKey)
            deleteKey(sharedPrefSecondaryPasscodeKey)
        }

        fun loadEncryptedSharedPreferences(context: Context) {
            encryptedSharedPreferences = EncryptedSharedPreferences.create(
                sharedPrefFilename,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        }

        fun deleteMessage(context: Context, message: Message) {
            messageList.remove(message)
            saveMessagesToFile(context)
        }

        fun clearAllData() {
            if (friendsFile.exists()) { friendsFile.delete() }
            if (messagesFile.exists()) { messagesFile.delete() }
            friendList.clear()
            messageList.clear()


            // Overwrite the keys to EncryptedSharedPreferences
            val keyHex = "0000000000000000000000000000000000000000000000000000000000000000"

            encryptedSharedPreferences
                .edit()
                .putString("NahoftPrivateKey", keyHex)
                .putString(publicKeyPreferencesKey, keyHex)
                .apply()

            // Remove Everything from EncryptedSharedPreferences
            encryptedSharedPreferences
                .edit()
                .clear()
                .apply()

            status = LoginStatus.NotRequired
        }

        fun saveFriendsToFile(context: Context) {
            val serializer = Persister()
            val outputStream = ByteArrayOutputStream()
            val friendsObject = Friends(friendList)

            try { serializer.write(friendsObject, outputStream) } catch (error: Exception) {
                print("Failed to serialize our friends list: $error")
            }

            PersistenceEncryption().writeEncryptedFile(friendsFile, outputStream.toByteArray(), context)
        }

        fun saveMessagesToFile(context: Context) {
            val serializer = Persister()
            val outputStream = ByteArrayOutputStream()
            val messagesObject = Messages(messageList)
            try { serializer.write(messagesObject, outputStream) } catch (error: Exception) {
                print("Failed to serialize our messages list: $error")
                return
            }

            PersistenceEncryption().writeEncryptedFile(messagesFile, outputStream.toByteArray(), context)
        }

        fun resetFriend(context: Context, friend: Friend)
        {
            messageList.removeIf { it.sender == friend }
            friend.publicKeyEncoded = null
            updateFriend(context, friend, FriendStatus.Default)

            saveMessagesToFile(context)
            saveFriendsToFile(context)
        }

        fun removeFriendAt(context: Context, friend: Friend)
        {
            val byeFriend = friendList.find { it.name == friend.name }
            messageList.removeIf { it.sender == byeFriend }
            byeFriend?.publicKeyEncoded = null
            friendList.remove(byeFriend)

            saveMessagesToFile(context)
            saveFriendsToFile(context)
        }
    }

}