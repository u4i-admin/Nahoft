package org.org.nahoft

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import kotlinx.android.synthetic.main.activity_enter_passcode.*
import org.org.codex.PersistenceEncryption
import org.org.codex.PersistenceEncryption.Companion.sharedPrefLoginStatusKey
import org.org.nahoft.Nahoft.Companion.encryptedSharedPreferences
import org.org.nahoft.Nahoft.Companion.status
import java.lang.Exception


class EnterPasscodeActivity : AppCompatActivity () {

    /// TEST PURPOSES ONLY
    val correctPasscode = "password"
    val secondaryPasscode = "secondpassword"
    /// TEST PURPOSES ONLY

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_passcode)

        // Load status from preferences
        encryptedSharedPreferences = EncryptedSharedPreferences.create(
            PersistenceEncryption.sharedPrefFilename,
            PersistenceEncryption.masterKeyAlias,
            this.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences

        getStatus()
        tryLogIn(status)

        // Button listeners
        cheat_button.setOnClickListener {
            tryLogIn(LoginStatus.NotRequired)
        }

        login_button.setOnClickListener {
            val enteredPassword = passcode_edit_text.text.toString()

            if (enteredPassword.equals(correctPasscode)) {
                status = LoginStatus.LoggedIn
            } else if (enteredPassword.equals(secondaryPasscode)) {
                status = LoginStatus.SecondaryLogin
            } else {
                status = LoginStatus.FailedLogin
            }

            saveStatus()
            tryLogIn(status)

            Toast.makeText(this, status.name, Toast.LENGTH_SHORT).show()
        }
    }

    fun getStatus() {

        val statusString = encryptedSharedPreferences.getString(sharedPrefLoginStatusKey, null)

        if (statusString != null) {

            try {
                status = LoginStatus.valueOf(statusString)
            } catch (error: Exception) {
                print("Received invalid status from EncryptedSharedPreferences. User is logged out.")
                status = LoginStatus.LoggedOut
            }
        }
    }

    fun saveStatus() {
        encryptedSharedPreferences
            .edit()
            .putString(sharedPrefLoginStatusKey, status.name)
            .apply()
    }

    fun tryLogIn(status: LoginStatus) {
        when (status) {
            // If the user has logged in successfully or if they didn't set a passcode
            // Send them to the home screen
            LoginStatus.LoggedIn, LoginStatus.NotRequired -> startActivity(Intent(this, HomeActivity::class.java))
            //TODO: Change println to delete user data
            LoginStatus.SecondaryLogin -> println("Secondary Login Successful")
        }
    }
}

enum class LoginStatus {

    NotRequired,
    LoggedIn,
    LoggedOut,
    SecondaryLogin,
    FailedLogin,
}
