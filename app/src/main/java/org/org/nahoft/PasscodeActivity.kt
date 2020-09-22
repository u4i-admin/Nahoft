package org.org.nahoft

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_passcode.*


class PasscodeActivity : AppCompatActivity () {

    val correctPasscode = "password"
    val secondaryPasscode = "secondpassword"

    // TODO: Load status from encryptedSharedPreferences
    var status = LoginStatus.LoggedOut

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passcode)

        cheat_button.setOnClickListener {
            logIn(LoginStatus.NotSet)
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

            logIn(status)

            Toast.makeText(this, status.name, Toast.LENGTH_SHORT).show()
        }
    }

    fun logIn(status: LoginStatus) {
        when (status) {
            // If the user has logged in successfully or if they didn't set a passcode
            // Send them to the home screen
            LoginStatus.LoggedIn, LoginStatus.NotSet -> startActivity(Intent(this, HomeActivity::class.java))
            //TODO: Change println to delete user data
            LoginStatus.SecondaryLogin -> println("Secondary Login Successful")
        }
    }
}

enum class LoginStatus {

    NotSet,
    LoggedIn,
    LoggedOut,
    SecondaryLogin,
    FailedLogin,
}
