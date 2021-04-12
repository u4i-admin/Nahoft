package org.nahoft.nahoft.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_enter_passcode.*
import kotlinx.android.synthetic.main.activity_verify_friend.*
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.sharedPrefFailedLoginAttemptsKey
import org.nahoft.nahoft.Persist.Companion.sharedPrefFailedLoginTimeKey
import org.nahoft.nahoft.Persist.Companion.sharedPrefPasscodeKey
import org.nahoft.nahoft.Persist.Companion.sharedPrefSecondaryPasscodeKey
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.nahoft.R
import org.nahoft.showAlert
import java.lang.Exception
import java.util.concurrent.TimeUnit

class EnterPasscodeActivity : AppCompatActivity (), TextWatcher {

    var failedLoginAttempts = 0
    var lastFailedLoginTimeMillis: Long? = null

    private val editTextArray: ArrayList<EditText> = ArrayList(NUM_OF_DIGITS)
    companion object {

        const val NUM_OF_DIGITS = 6
    }

    private var numTemp = "0"

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_passcode)

        //create array
        val layout: LinearLayout = findViewById(R.id.passcodeContainer)
        for (index in 0 until (layout.childCount)) {
            val view: View = layout.getChildAt(index)
            if (view is EditText) {
                editTextArray.add(index, view)
                editTextArray[index].addTextChangedListener(this)
            }

            editTextArray[index].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    //backspace
                    if (index != 0) { //Don't implement for first digit
                        editTextArray[index - 1].requestFocus()
                        editTextArray[index - 1]
                            .setSelection(editTextArray[index - 1].length())
                    }
                }
                false
            }
        }

        editTextArray[0].requestFocus() //After the views are initialized we focus on the first view

        // Load encryptedSharedPreferences
        Persist.loadEncryptedSharedPreferences(this.applicationContext)

        // Load status from preferences
        this.getStatus()

        tryLogIn(status)

        login_button.setOnClickListener {
            handleLoginPress()
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    fun handleLoginPress() {
        val enteredPasscode = getEnteredPasscode()
        if (enteredPasscode != null) {
            failedLoginAttempts = Persist.encryptedSharedPreferences.getInt(
                sharedPrefFailedLoginAttemptsKey, 0)
            val savedTimeStamp = Persist.encryptedSharedPreferences.getLong(
                sharedPrefFailedLoginTimeKey, 0)
            if (savedTimeStamp == 0.toLong()) { lastFailedLoginTimeMillis = null}
            else {lastFailedLoginTimeMillis = savedTimeStamp}

            verifyCode(enteredPasscode)
        }
    }

    // Checks encryptedSharedPreferences for a valid login status and saves it to the status property
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
            LoginStatus.LoggedIn, LoginStatus.NotRequired -> {
                // TODO: Clear out all 6 edit texts after successful login.
                val homeActivityIntent = Intent(this, HomeActivity::class.java)

                // Check to see if we received a send intent
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let{
                    // Received string message
                    homeActivityIntent.putExtra(Intent.EXTRA_TEXT, it)
                }

                // See if we received an image message
                val extraStream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                if (extraStream != null){
                    val extraUri = Uri.parse(extraStream.toString())
                    homeActivityIntent.putExtra(Intent.EXTRA_STREAM, extraUri)
                }
                else
                {
                    println("Extra Stream is Null")
                }

                startActivity(homeActivityIntent)
            }

            // Secondary passcode entered delete user data
            LoginStatus.SecondaryLogin -> {
                Persist.clearAllData()
                startActivity(Intent(this, HomeActivity::class.java))
            }
            else -> println("Login Status is $status")
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        numTemp = s.toString()
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        numTemp = s.toString()
    }

    override fun afterTextChanged(s: Editable?) {
        (0 until editTextArray.size)
            .forEach { index ->
                if (s === editTextArray[index].editableText) {

                    if (s != null && !s.isBlank()) {
                        //if more than 1 char
                        if (s.length > 1) {
                            //save the second char of s to newTemp
                            val newTemp = s.toString().substring(s.length - 1, s.length)
                            if (newTemp != numTemp) {
                                //put newTemp in editText
                                editTextArray[index].setText(newTemp)
                            } else {
                                //put the first char of s in the editText
                                editTextArray[index].setText(s.toString().substring(0, s.length - 1))
                            }
                        } else if (index != editTextArray.size - 1) {
                            //not last edit text
                            editTextArray[index + 1].requestFocus()
                            editTextArray[index + 1].setSelection(editTextArray[index + 1].length())
                            return
                        }
                        /*else {
                            //will verify code the moment the last character is inserted and all digits have a number
                            verifyCode(getEnteredPasscode())
                        }*/
                    } else {
                        return
                    }
                }
            }
    }

    // Returns the Passcode the user entered if it has the correct number of digits, else null
    private fun getEnteredPasscode(): String? {
        var verificationCode = ""

        // Checks all of the passcode editTexts for strings and adds it to verificationCode
        for (index in editTextArray.indices) {
            val digit = editTextArray[index].text.toString().trim { it <= ' ' }
            verificationCode += digit
        }

        // Returns verification code if it is NUM_OF_DIGITS long
        if (verificationCode.trim { it <= ' ' }.length == NUM_OF_DIGITS) {
            return verificationCode
        }

        return null
    }

    private fun verifyCode(verificationCode: String) {
        if (verificationCode.isNotEmpty()) {
            //Check to see if the user is allowed to try to login.
            if (!loginAllowed()) { return }

            // check to see if the user has saved a passcode or a secondary passcode.
            val maybePasscode = Persist.encryptedSharedPreferences.getString(sharedPrefPasscodeKey, null)
            val maybeSecondary = Persist.encryptedSharedPreferences.getString(sharedPrefSecondaryPasscodeKey, null)

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

    private fun getLockoutMinutes(): Int {

        if (failedLoginAttempts >= 11) { return 1000 }
        else if (failedLoginAttempts == 10) { return 60 }
        else if (failedLoginAttempts == 9) { return 30 }
        else if (failedLoginAttempts == 8) { return 15 }
        else if (failedLoginAttempts == 7) { return 5 }
        else if (failedLoginAttempts == 6) { return 1 }
        else { return 0 }
    }

    private fun showLoginFailureAlert() {
        if (failedLoginAttempts >= 9) {
            showAlert(getString(R.string.alert_text_eleven_login_attempts))
            println("Failed Login $failedLoginAttempts times, all information has been erased")

            //Delete everything like you would if user had entered a secondary passcode.
            Persist.clearAllData()
            startActivity(Intent(this, HomeActivity::class.java))

        } else if (failedLoginAttempts == 8) {
            showAlert(getString(R.string.alert_text_eigth_login_attempt))
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

        }
        else {
            //showAlert(getString(R.string.alert_text_zero_to_five_login_attempts))
            println("Failed Login $failedLoginAttempts times")
        }
    }

    private fun loginAllowed(): Boolean {
        //how long is the user locked out for?
        val minutesToWait = getLockoutMinutes()
        val millisToWait = minutesToWait * 1000 * 60

        if (minutesToWait == 0) { return true }
        else if (minutesToWait >= 100) { //This should never happen all data should have already been deleted when the login failed the eleventh time.
            //Delete everything like you would if user had entered a secondary passcode.
            showAlert(getString(R.string.alert_text_eleven_login_attempts))
            Persist.clearAllData()
            startActivity(Intent(this, HomeActivity::class.java))

            return false
        }

        //get the current time
        val currentTimeMillis = System.currentTimeMillis()

        //compare the current time to the last failed attempt time
        if (lastFailedLoginTimeMillis != null){

            val elapsedTimeMillis = currentTimeMillis - lastFailedLoginTimeMillis!!
            if (elapsedTimeMillis >= millisToWait) { return true }
            else
            {
                val remainingMillis = millisToWait - elapsedTimeMillis
                val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
                val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60

                showAlert(getString(R.string.alert_text_minutes_to_wait_until_user_can_attempt_to_login_again, remainingMinutes, remainingSeconds))
                println("showAlert is from the loginAllowed function")

                return false
            }
        }
        else {
            println("ERROR: Last failed login timestamp is null, but user has more than 5 failed login attempts.")
            return false
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
