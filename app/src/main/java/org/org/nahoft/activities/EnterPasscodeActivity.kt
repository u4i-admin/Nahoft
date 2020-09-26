package org.org.nahoft.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import kotlinx.android.synthetic.main.activity_enter_passcode.*
import kotlinx.android.synthetic.main.activity_passcode.*
import org.org.nahoft.HomeActivity
import org.org.nahoft.Persist
import org.org.nahoft.Persist.Companion.sharedPrefPasscodeKey
import org.org.nahoft.Persist.Companion.sharedPrefSecondaryPasscodeKey
import org.org.nahoft.R
import java.lang.Exception


class EnterPasscodeActivity : AppCompatActivity () {

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

        this.getStatus()
        tryLogIn(Persist.status)

        login_button.setOnClickListener {
            val enteredPassword = passcode_edit_text.text.toString()
            val maybePasscode = Persist.encryptedSharedPreferences.getString(sharedPrefPasscodeKey, null)
            val maybeSecondary = Persist.encryptedSharedPreferences.getString(sharedPrefSecondaryPasscodeKey, null)

            if (maybePasscode != null &&maybeSecondary != null) {
                // Populate our text inputs
                enter_passcode_input.setText(maybePasscode)
                verify_passcode_input.setText(maybePasscode)
                secondary_passcode_input.setText(maybeSecondary)
                verify_secondary_passcode_input.setText(maybeSecondary)
            }

            when (enteredPassword) {
                maybePasscode -> {
                    Persist.status = LoginStatus.LoggedIn
                }
                maybeSecondary -> {
                    Persist.status = LoginStatus.SecondaryLogin
                }
                else -> {
                    Persist.status = LoginStatus.FailedLogin
                }
            }

            saveStatus()
            tryLogIn(Persist.status)

            Toast.makeText(this, Persist.status.name, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getStatus() {

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

    private fun saveStatus() {
        Persist.encryptedSharedPreferences
    .edit()
    .putString(Persist.sharedPrefLoginStatusKey, Persist.status.name)
    .apply()
}

    private fun tryLogIn(status: LoginStatus) {
        when (status) {
            // If the user has logged in successfully or if they didn't set a passcode
            // Send them to the home screen
            LoginStatus.LoggedIn, LoginStatus.NotRequired -> startActivity(Intent(this, HomeActivity::class.java))
            //TODO: Change println to delete user data
            LoginStatus.SecondaryLogin -> println("Secondary Login Successful")
            else -> println("Login Status is $status")
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
