package org.nahoft.nahoft

import android.app.Application
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.nahoft.nahoft.activities.LoginStatus
import java.io.File

class Persist {

    companion object {

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

        fun updateFriendEntry(editedFriend: Friend) {
            // Look in our friendList for a Friend with the same id as editedFriend
            // Please not that equals is overridden for the Friend class to only compare ids
            val matchingFriendIndex = friendList.indexOf(editedFriend)
            if (matchingFriendIndex > -1) {
                // If we find a matching friend, replace that Friend with the new editedFriend
                friendList.set(matchingFriendIndex, editedFriend)
            } else {
                // Otherwise just add editedFriend to the list
                friendList.add(editedFriend)
            }
        }

        fun saveKey(key:String, value:String) {
            encryptedSharedPreferences
                .edit()
                .putString(key, value)
                .apply()
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

        // TODO: Another pair of eyes, did we get everything?
        fun clearAllData() {

            if (friendsFile.exists()) { friendsFile.delete() }
            if (messagesFile.exists()) { messagesFile.delete() }
            friendList.clear()
            messageList.clear()


            // Remove Everything from EncryptedSharedPreferences
            encryptedSharedPreferences
                .edit()
                .clear()
                .apply()
        }
    }

}