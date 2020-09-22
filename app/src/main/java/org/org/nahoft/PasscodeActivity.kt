package org.org.nahoft

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_passcode.*


class PasscodeActivity : AppCompatActivity () {

    val correctPassword = "password"
    val correctUsername = "Jessica"
    val secondaryPassword = "secondpassword"
    var status = LoginStatus.LoggedOut

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passcode)

        cheat_button.setOnClickListener {
            logIn()
        }

        login_button.setOnClickListener {
            val enteredUsername = user_name_edit_text.text.toString()
            val enteredPassword = passcode_edit_text.text.toString()

            if (enteredUsername.equals(correctUsername)) {

                if (enteredPassword.equals(correctPassword)) {
                    status = LoginStatus.LoggedIn
                } else if (enteredPassword.equals(secondaryPassword)) {
                    status = LoginStatus.SecondaryLogin
                } else {
                    status = LoginStatus.FailedLogin
                }
            } else {
                status = LoginStatus.FailedLogin
            }

            when (status) {
                LoginStatus.LoggedIn -> logIn()
                //TODO: Change println to manage failed login according to scope
                LoginStatus.FailedLogin -> println("Failed Log In")
                //TODO: Change println to delete user data
                LoginStatus.SecondaryLogin -> println("Secondary Login Successful")
            }


            Toast.makeText(this, status.name, Toast.LENGTH_SHORT).show()
        }
    }

    fun logIn() {
        val homeIntent = Intent(this, HomeActivity::class.java)
        startActivity(homeIntent)
    }
}

enum class LoginStatus {

    LoggedIn,
    LoggedOut,
    SecondaryLogin,
    FailedLogin,
}
