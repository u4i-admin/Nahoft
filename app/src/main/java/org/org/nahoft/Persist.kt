package org.org.nahoft

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.org.nahoft.activities.LoginStatus
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

        var friendList = ArrayList<Friend>()
        var messageList = ArrayList<Message>()
    }

    fun saveStatus() {
        encryptedSharedPreferences
            .edit()
            .putString(sharedPrefLoginStatusKey, status.name)
            .apply()
    }

    fun saveKey(key:String, value:String) {
        encryptedSharedPreferences
            .edit()
            .putString(key, value)
            .apply()
    }
}