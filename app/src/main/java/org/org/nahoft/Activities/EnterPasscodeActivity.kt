package org.org.nahoft

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import kotlinx.android.synthetic.main.activity_enter_passcode.*
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
        Persist.encryptedSharedPreferences = EncryptedSharedPreferences.create(
            Persist.sharedPrefFilename,
            Persist.masterKeyAlias,
            this.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences

        getStatus()
        tryLogIn(Persist.status)

        // Button listeners
        cheat_button.setOnClickListener {
            tryLogIn(LoginStatus.NotRequired)
        }

        login_button.setOnClickListener {
            val enteredPassword = passcode_edit_text.text.toString()

            if (enteredPassword.equals(correctPasscode)) {
                Persist.status = LoginStatus.LoggedIn
            } else if (enteredPassword.equals(secondaryPasscode)) {
                Persist.status = LoginStatus.SecondaryLogin
            } else {
                Persist.status = LoginStatus.FailedLogin
            }

            saveStatus()
            tryLogIn(Persist.status)

            Toast.makeText(this, Persist.status.name, Toast.LENGTH_SHORT).show()
        }
    }

    fun getStatus() {

        val statusString = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefLoginStatusKey, null)

        if (statusString != null) {

            try {
                Persist.status = LoginStatus.valueOf(statusString)
            } catch (error: Exception) {
                print("Received invalid status from EncryptedSharedPreferences. User is logged out.")
                Persist.status = LoginStatus.LoggedOut
            }
        } else {
            Persist.status = LoginStatus.NotRequired
        }
    }

    fun saveStatus() {
        Persist.encryptedSharedPreferences
            .edit()
            .putString(Persist.sharedPrefLoginStatusKey, Persist.status.name)
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
