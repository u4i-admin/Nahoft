package org.nahoft.nahoft.activities

import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_passcode.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.util.showAlert

class PasscodeActivity : AppCompatActivity() {

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passcode)

        updateSwitch()

        // Switch Listener
        passcode_switch.setOnCheckedChangeListener { _, isChecked ->
            handlePasscodeRequirementChange(isChecked)
        }

        // Save Button Listener
        save_passcode_button.setOnClickListener {
            handleSaveButtonClick()
        }

        delete_passcode_button.setOnClickListener {
            handleDeletePasscodeClick()
        }

    }

    override fun onStop() {

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })
        cleanup()
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        unregisterReceiver(receiver)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        Persist.saveLoginStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Persist.saveLoginStatus()
    }

    private fun updateSwitch() {
        if (status == LoginStatus.NotRequired)
        {
            updateInputs(false)
        } else {
            updateInputs(true)
        }
    }

    private fun updateInputs(passcodeRequired: Boolean) {
        if (passcodeRequired) {
            // Check for passcodes in shared preferences
            val maybePasscode = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefPasscodeKey, null)
            val maybeSecondary = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefSecondaryPasscodeKey, null)

            // Passcode
            if (maybePasscode != null) {
                passcode_switch.isChecked = true
                // Populate our text inputs
                enter_passcode_input.setText(maybePasscode)
                verify_passcode_input.setText(maybePasscode)
            }

            // Secondary Passcode
            if (maybeSecondary != null) {
                // Populate our text inputs
                enter_secondary_passcode_input.setText(maybeSecondary)
                verify_secondary_passcode_input.setText(maybeSecondary)
            }

            // Make sure that our passcodes are enabled
            enter_passcode_input.isEnabled = true
            verify_passcode_input.isEnabled = true
            enter_secondary_passcode_input.isEnabled = true
            verify_secondary_passcode_input.isEnabled = true

            save_passcode_button.isEnabled = true
            delete_passcode_button.isEnabled = true

        } else {
            passcode_switch.isChecked = false

            // Disable passcode inputs and clear them out
            enter_passcode_input.text?.clear()
            enter_passcode_input.isEnabled = false
            verify_passcode_input.text?.clear()
            verify_passcode_input.isEnabled = false
            enter_secondary_passcode_input.text?.clear()
            enter_secondary_passcode_input.isEnabled = false
            verify_secondary_passcode_input.text?.clear()
            verify_secondary_passcode_input.isEnabled = false

            save_passcode_button.isEnabled = false
            delete_passcode_button.isEnabled = false
        }
    }

    private fun handlePasscodeRequirementChange(required: Boolean) {

        if (required) {
            // If there aren't already saved passcodes prompt user
            // We will not update the status to logged in until the user has entered valid passcodes
        } else {
            // Status is NotRequired
            status = LoginStatus.NotRequired
        }

        updateInputs(required)
    }

    private fun handleDeletePasscodeClick() {
        Persist.deletePasscode()
        status = LoginStatus.NotRequired
        finish()
    }

    private fun handleSaveButtonClick() {
        val passcode = enter_passcode_input.text.toString()
        val passcode2 = verify_passcode_input.text.toString()
        val secondaryPasscode = enter_secondary_passcode_input.text.toString()
        val secondaryPasscode2 = verify_secondary_passcode_input.text.toString()

        if (passcode == "") {
            this.showAlert(getString(R.string.alert_text_passcode_field_empty))

           return
        }

        if (passcode2 == "") {
            this.showAlert(getString(R.string.alert_text_verify_passcode_field_empty))

            return
        }

        if (passcode != passcode2){
            this.showAlert(getString(R.string.alert_text_passcode_entries_do_not_match))

            return
        }

        if (!passcodeMeetsRequirements(passcode)) return

        Persist.saveKey(Persist.sharedPrefPasscodeKey, passcode)

        // If the user puts something in the verify secondary passcode field
        // but did not put something in the first secondary passcode field
        // show an error.
        if(secondaryPasscode == "") {

            if (secondaryPasscode2 != "") {
                this.showAlert(getString(R.string.alert_text_secondary_passcode_field_empty))

                return
            } else {

                // Set user status
                status = LoginStatus.LoggedIn

                // Return to previous screen
                finish()
            }

        } else { // User has entered a secondary passcode in the first field.

            if (secondaryPasscode2 == "") {
                this.showAlert(getString(R.string.alert_text_verify_secondary_passcode_empty))

                return
            }

            if (secondaryPasscode != secondaryPasscode2) {
                this.showAlert(getString(R.string.alert_text_secondary_passcode_entries_do_not_match))

                return
            }

            if (secondaryPasscode == passcode) {
                this.showAlert(getString(R.string.alert_text_secondary_passcode_and_passcode_may_not_match))

                return
            }

            if (!passcodeMeetsRequirements(secondaryPasscode)) return

            Persist.saveKey(Persist.sharedPrefSecondaryPasscodeKey, secondaryPasscode)

            // Set user status
            status = LoginStatus.LoggedIn

            // Return to previous screen
            finish()

        }

    }

    private fun passcodeMeetsRequirements(passcode: String): Boolean
    {
        return (isPasscodeCorrectLength(passcode) &&
                isPasscodeNonSequential(passcode) &&
                isPasscodeNonRepeating(passcode))
    }

    private fun isPasscodeCorrectLength(passcode: String): Boolean {

        if (passcode.length == 6)
        {
            return true
        }
        else
        {
            showAlert(getString(R.string.alert_text_incorrect_passcode_length))
            return false
        }
    }

    // Returns true if the passcode provided is non sequential numbers.
    private fun isPasscodeNonSequential(passcode: String): Boolean {

        val digitArray = passcode.map { it.toString().toInt() }.toTypedArray()
        val max = digitArray.maxOrNull()
        val min = digitArray.minOrNull()

        // This is unexpected behavior. The input type only allows numbers.
        if (max == null || min == null){
            showAlert(getString(R.string.alert_text_invalid_passcode))
            return false
        }

        // If the difference between the maximum and minimum element in the array is equal to (array.size - 1),
        // and each element in the array is distinct,
        // then the array contains consecutive integers.
        if (max - min == digitArray.size - 1)
        {
            if (digitArray.distinct().size == digitArray.size)
            {
                showAlert(getString(R.string.alert_text_passcode_is_sequential))
                return false
            }
        }

        return true
    }

    // Returns true if all the numbers in the passcode are not the same.

    private fun isPasscodeNonRepeating(passcode: String): Boolean {

        val firstChar = passcode[0]

        for (index in 1 until passcode.length){
            val char = passcode[index]

            if (char != firstChar){
                return true
            }
        }

        showAlert(getString(R.string.alert_text_passcode_is_a_repeated_digit))
        return false
    }

    fun cleanup(){
        enter_passcode_input.text.clear()
        verify_passcode_input.text.clear()
        enter_secondary_passcode_input.text.clear()
        verify_secondary_passcode_input.text.clear()
        //showAlert("Passcode Activity Logout Timer Broadcast Received", length = Toast.LENGTH_LONG)
    }

}