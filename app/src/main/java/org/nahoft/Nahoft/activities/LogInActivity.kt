package org.nahoft.nahoft.activities

import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import kotlinx.android.synthetic.main.activity_log_in.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.clearAllData
import org.nahoft.nahoft.Persist.Companion.sharedPrefFailedLoginAttemptsKey
import org.nahoft.nahoft.Persist.Companion.sharedPrefFailedLoginTimeKey
import org.nahoft.nahoft.Persist.Companion.sharedPrefPasscodeKey
import org.nahoft.nahoft.Persist.Companion.sharedPrefSecondaryPasscodeKey
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.nahoft.R
import org.nahoft.util.showAlert
import java.lang.NullPointerException
import java.util.concurrent.TimeUnit

class LogInActivity : AppCompatActivity()
{

    private var failedLoginAttempts = 0
    private var lastFailedLoginTimeMillis: Long? = null

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanup()
        }
    }

    override fun onBackPressed() {
        // User should not be able to back out of this activity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        // Load encryptedSharedPreferences
        Persist.loadEncryptedSharedPreferences(this.applicationContext)

        // Load status from preferences
        getStatus()
        tryLogIn(status)

        login_button.setOnClickListener {
            this.handleLoginPress()
        }

        passcodeEditText.setOnEditorActionListener { _, keyCode, event ->
          return@setOnEditorActionListener when (keyCode) {
             EditorInfo.IME_ACTION_DONE -> {
                 this.handleLoginPress()
                 true
             }
              else -> false
          }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun handleLoginPress()
    {
        val enteredPasscode = passcodeEditText.text.toString()

        if (enteredPasscode.isNotBlank())
        {
            failedLoginAttempts = Persist.encryptedSharedPreferences.getInt(sharedPrefFailedLoginAttemptsKey, 0)
            val savedTimeStamp = Persist.encryptedSharedPreferences.getLong(sharedPrefFailedLoginTimeKey, 0)

            if (savedTimeStamp == 0.toLong())
            {
                lastFailedLoginTimeMillis = null
            }
            else
            {
                lastFailedLoginTimeMillis = savedTimeStamp
            }

            verifyCode(enteredPasscode)
        }
    }

    // Checks encryptedSharedPreferences for a valid login status and saves it to the status property
    private fun getStatus()
    {

        val statusString =
            Persist.encryptedSharedPreferences.getString(Persist.sharedPrefLoginStatusKey, null)

        if (statusString != null)
        {
            try
            {
                status = LoginStatus.valueOf(statusString)
            }
            catch (error: Exception)
            {
                print("Received invalid status from EncryptedSharedPreferences. User is logged out.")
                status = LoginStatus.LoggedOut
            }
        }
        else
        {
            status = LoginStatus.NotRequired
        }
    }

    private fun saveStatus()
    {
        Persist.encryptedSharedPreferences
            .edit()
            .putString(Persist.sharedPrefLoginStatusKey, status.name)
            .apply()
    }

    private fun tryLogIn(status: LoginStatus)
    {
        when (status)
        {
            // If the user has logged in successfully or if they didn't set a passcode
            // Send them to the home screen
            LoginStatus.LoggedIn, LoginStatus.NotRequired ->
            {
                val extraString = intent.getStringExtra(Intent.EXTRA_TEXT)
                val extraStream = intent.getStringExtra(Intent.EXTRA_STREAM)

                if (extraString != null) // Check to see if we received a string message share
                {
                    // Received string message
                    val importTextActivityIntent = Intent(this, ImportTextActivity::class.java)
                    importTextActivityIntent.putExtra(Intent.EXTRA_TEXT, extraString)
                    startActivity(importTextActivityIntent)
                    return
                }
                else if (extraStream != null) // See if we received an image message share
                {
                    try
                    {
                        val extraUri = Uri.parse(extraStream)
                        val importImageActivityIntent = Intent(this, ImportImageActivity()::class.java)
                        importImageActivityIntent.putExtra(Intent.EXTRA_STREAM, extraUri)
                        startActivity(importImageActivityIntent)
                    }
                    catch (e: NullPointerException)
                    {
                        // The string was not a url don't try to share it
                    }

                }
                else
                {
                    val homeActivityIntent = Intent(this, HomeActivity::class.java)
                    startActivity(homeActivityIntent)
                }
            }

            // Destruction code entered delete user data
            LoginStatus.SecondaryLogin ->
            {
                Persist.clearAllData()
                startActivity(Intent(this, HomeActivity::class.java))
            }

            else -> println("Login Status is $status")
        }
    }

    private fun verifyCode(verificationCode: String)
    {
        if (verificationCode.isNotEmpty()) {
            //Check to see if the user is allowed to try to login.
            if (!loginAllowed()) {
                return
            }

            // check to see if the user has saved a passcode or a secondary passcode.
            val maybePasscode =
                Persist.encryptedSharedPreferences.getString(sharedPrefPasscodeKey, null)
            val maybeSecondary =
                Persist.encryptedSharedPreferences.getString(sharedPrefSecondaryPasscodeKey, null)

            // check to see if the passcode is correct.
            when (verificationCode) {
                maybePasscode -> {
                    status = LoginStatus.LoggedIn
                    failedLoginAttempts = 0
                    lastFailedLoginTimeMillis = null
                    Persist.saveLoginFailure(0)
                }
                maybeSecondary -> {
                    status = LoginStatus.SecondaryLogin
                }
                // Failed Login
                else -> {
                    status = LoginStatus.FailedLogin
                    failedLoginAttempts += 1
                    lastFailedLoginTimeMillis = System.currentTimeMillis()
                    Persist.saveLoginFailure(failedLoginAttempts)
                    showLoginFailureAlert()
                }
            }

            saveStatus()
            tryLogIn(status)
        }
    }

    private fun getLockoutMinutes(): Int
    {
        if (failedLoginAttempts >= 9) {
            return 1000
        } else if (failedLoginAttempts == 8) {
            return 15
        } else if (failedLoginAttempts == 7) {
            return 5
        } else if (failedLoginAttempts == 6) {
            return 1
        } else {
            return 0
        }
    }

    private fun showLoginFailureAlert() {
        if (failedLoginAttempts >= 9) {
            showAlert(getString(R.string.alert_text_ninth_login_attempt))
            println("Failed Login $failedLoginAttempts times, all information has been erased")

            //Delete everything like you would if user had entered a secondary passcode.
            clearAllData()
            startActivity(Intent(this, HomeActivity::class.java))

        } else if (failedLoginAttempts == 8) {
            showAlert(getString(R.string.alert_text_eighth_login_attempt))
            println("Failed Login $failedLoginAttempts times, 15 minute timeout")

        } else if (failedLoginAttempts == 7) {
            showAlert(getString(R.string.alert_text_seventh_login_attempt))
            println("Failed Login $failedLoginAttempts times, 5 minute timeout")

        } else if (failedLoginAttempts == 6) {
            showAlert(getString(R.string.alert_text_sixth_login_attempt))
            println("Failed Login $failedLoginAttempts times, 1 minute timeout")

        } else if (failedLoginAttempts <= 5 && failedLoginAttempts > 0) {
            showAlert(getString(R.string.alert_text_zero_to_five_login_attempts))
            println("Failed Login $failedLoginAttempts times")

        } else {
            println("Failed Login $failedLoginAttempts times")
        }
    }

    private fun loginAllowed(): Boolean {
        //how long is the user locked out for?
        val minutesToWait = getLockoutMinutes()
        val millisToWait = minutesToWait * 1000 * 60

        if (minutesToWait == 0) {
            return true
        } else if (minutesToWait >= 100) {
            //This should never happen all data should have already been deleted when the login failed the final time.
            //Delete everything like you would if user had entered a secondary passcode.
            showAlert(getString(R.string.alert_text_ninth_login_attempt))
            Persist.clearAllData()
            startActivity(Intent(this, HomeActivity::class.java))

            return false
        }

        //get the current time
        val currentTimeMillis = System.currentTimeMillis()

        //compare the current time to the last failed attempt time
        if (lastFailedLoginTimeMillis != null) {

            val elapsedTimeMillis = currentTimeMillis - lastFailedLoginTimeMillis!!
            if (elapsedTimeMillis >= millisToWait) {
                return true
            } else {
                val remainingMillis = millisToWait - elapsedTimeMillis
                val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
                val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60

                showAlert(
                    getString(
                        R.string.alert_text_minutes_to_wait_until_user_can_attempt_to_login_again,
                        remainingMinutes,
                        remainingSeconds
                    )
                )
                println("showAlert is from the loginAllowed function")

                return false
            }
        } else {
            println("ERROR: Last failed login timestamp is null, but user has more than 5 failed login attempts.")
            return false
        }
    }

    private fun cleanup()
    {
        passcodeEditText.text.clear()
    }
}

    enum class LoginStatus {

        NotRequired,
        LoggedIn,
        LoggedOut,
        SecondaryLogin,
        FailedLogin,
    }

