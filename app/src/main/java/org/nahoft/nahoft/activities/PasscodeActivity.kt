package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_passcode.*
import org.nahoft.nahoft.*
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.showAlert

class PasscodeActivity : AppCompatActivity() {

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

    }

    override fun onResume() {
        super.onResume()

        updateSwitch()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        Persist.saveLoginStatus()
    }

    override fun onDestroy() {
        super.onDestroy()

        Persist.saveLoginStatus()
    }

    fun updateSwitch() {
        if (status == LoginStatus.NotRequired)
        {
            updateInputs(false)
        } else {
            updateInputs(true)
        }
    }

    fun updateInputs(passcodeRequired: Boolean) {
        if (passcodeRequired) {
            // Check for passcodes in shared preferences
            val maybePasscode = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefPasscodeKey, null)
            val maybeSecondary = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefSecondaryPasscodeKey, null)

            if (maybePasscode != null &&maybeSecondary != null) {
                // Populate our text inputs
                enter_passcode_input.setText(maybePasscode)
                verify_passcode_input.setText(maybePasscode)
                secondary_passcode_input.setText(maybeSecondary)
                verify_secondary_passcode_input.setText(maybeSecondary)
            }

            // Make sure that our passcodes are enabled
            enter_passcode_input.isEnabled = true
            verify_passcode_input.isEnabled = true
            secondary_passcode_input.isEnabled = true
            verify_secondary_passcode_input.isEnabled = true

            save_passcode_button.isEnabled = true

        } else {

            // Disable passcode inputs and clear them out
            enter_passcode_input.text?.clear()
            enter_passcode_input.isEnabled = false
            verify_passcode_input.text?.clear()
            verify_passcode_input.isEnabled = false
            secondary_passcode_input.text?.clear()
            secondary_passcode_input.isEnabled = false
            verify_secondary_passcode_input.text?.clear()
            verify_secondary_passcode_input.isEnabled = false

            save_passcode_button.isEnabled = false
        }
    }

    fun handlePasscodeRequirementChange(required: Boolean) {

        if (required) {
            // If there aren't already saved passcodes prompt user
            // We will not update the status to logged in until the user has entered valid passcodes
        } else {
            // Status is NotRequired
            status = LoginStatus.NotRequired
        }

        updateInputs(required)
    }

    fun handleSaveButtonClick() {
        val passcode = enter_passcode_input.text.toString()
        val passcode2 = verify_passcode_input.text.toString()
        val secondaryPasscode = secondary_passcode_input.text.toString()
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

        Persist.saveKey(Persist.sharedPrefPasscodeKey, passcode)

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

        } else {
            if (secondaryPasscode2 == "") {
                this.showAlert(getString(R.string.alert_text_verify_secondary_passcode_empty))

                return
            }

            if (secondaryPasscode != secondaryPasscode2) {
                this.showAlert(getString(R.string.alert_text_secondary_passcode_entries_do_not_match))

                return
            }

            Persist.saveKey(Persist.sharedPrefSecondaryPasscodeKey, secondaryPasscode)

            // Set user status
            status = LoginStatus.LoggedIn

            // Return to previous screen
            finish()

        }

    }

}