package org.nahoft.nahoft.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_enter_passcode.*
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.sharedPrefPasscodeKey
import org.nahoft.nahoft.Persist.Companion.sharedPrefSecondaryPasscodeKey
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.nahoft.R
import java.lang.Exception


class EnterPasscodeActivity : AppCompatActivity () {

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_passcode)

        // Load status from preferences
        Persist.loadEncryptedSharedPreferences(this.applicationContext)
        this.getStatus()
        tryLogIn(status)

        login_button.setOnClickListener {
            val enteredPassword = passcode_edit_text.text.toString()
            val maybePasscode = Persist.encryptedSharedPreferences.getString(sharedPrefPasscodeKey, null)
            val maybeSecondary = Persist.encryptedSharedPreferences.getString(sharedPrefSecondaryPasscodeKey, null)

            when (enteredPassword) {
                maybePasscode -> {
                    status = LoginStatus.LoggedIn
                }
                maybeSecondary -> {
                    status = LoginStatus.SecondaryLogin
                }
                else -> {
                    status = LoginStatus.FailedLogin
                }
            }

            saveStatus()
            tryLogIn(status)
        }
    }

    private fun getStatus() {

        val statusString = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefLoginStatusKey, null)

        if (statusString != null) {

            try {
                status = LoginStatus.valueOf(statusString)
            } catch (error: Exception) {
                print("Received invalid status from EncryptedSharedPreferences. User is logged out.")
                status = LoginStatus.LoggedOut
            }
        } else {
            status = LoginStatus.NotRequired
        }
    }

    private fun saveStatus() {
        Persist.encryptedSharedPreferences
    .edit()
    .putString(Persist.sharedPrefLoginStatusKey, status.name)
    .apply()
}

    private fun tryLogIn(status: LoginStatus) {
        when (status) {
            // If the user has logged in successfully or if they didn't set a passcode
            // Send them to the home screen
            LoginStatus.LoggedIn, LoginStatus.NotRequired -> startActivity(Intent(this, HomeActivity::class.java))
            // Secondary passcode entered delete user data
            // TODO: Test to see if this works
            LoginStatus.SecondaryLogin -> {
                Persist.clearAllData()
                startActivity(Intent(this, HomeActivity::class.java))
            }
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
