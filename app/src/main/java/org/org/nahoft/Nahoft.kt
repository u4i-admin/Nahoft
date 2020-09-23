package org.org.nahoft

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import java.io.File

class Nahoft: Application() {

    companion object {

        // Initialized by EnterPasscodeActivity(main)
        lateinit var status: LoginStatus
        lateinit var encryptedSharedPreferences: EncryptedSharedPreferences

        // Initialized by HomeActivity
        lateinit var friendsFile: File

        var friendList = ArrayList<Friend>()
    }


}