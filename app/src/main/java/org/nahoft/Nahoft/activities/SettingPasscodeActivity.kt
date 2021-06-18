package org.nahoft.nahoft.activities

import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_setting_passcode.*
import kotlinx.android.synthetic.main.activity_setting_passcode.passcode_switch
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.util.showAlert

class SettingPasscodeActivity : AppCompatActivity() {

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_passcode)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        updateSwitch()
        setDefaultView()

        passcode_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // The switch is toggled on
                passcodeSwitchIsChecked()
                handlePasscodeRequirementChange(isChecked)
            } else {
                // The switch is toggled off
                passcodeSwitchIsUnChecked()
                }
        }

        destruction_code_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // The destruction code switch is toggled on
                destructionCodeIsChecked()
                updateDestructionCodeInputs(isChecked)
            } else {
                // The switch is toggled off
                passcodeSwitchIsUnChecked()
            }
        }

        passcode_submit_button.setOnClickListener {
            handleSaveButtonClick()
        }

        destruction_code_switch.setOnClickListener {
            handleSaveButtonClick()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Persist.saveLoginStatus()
    }

    override fun onDestroy() {
        Persist.saveLoginStatus()
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun updateSwitch() {
        if (Persist.status == LoginStatus.NotRequired)
        {
            updatePasscodeInputs(false)
        } else {
            updatePasscodeInputs(true)
        }
    }

    private fun setDefaultView() {
        passcode_off_icon_text_switch_layout.isVisible = true
        passcode_entry_layout.isVisible = false
        destruction_off_icon_label_switch_layout.isVisible = true
        destruction_code_layout.isVisible = false
    }

    private fun passcodeSwitchIsChecked () {
        passcodeSwitchImageView.setImageResource(R.drawable.nahoft_icons_passcode_on_56)
        passcode_entry_layout.isVisible = true
        des_icon.setImageResource(R.drawable.nahoft_icons_des_off_56)
        destruction_code_switch.isVisible = true
        destruction_code_switch.isClickable = true
        destruction_code_layout.isVisible = false
    }

    private fun passcodeSwitchIsUnChecked () {
        passcodeSwitchImageView.setImageResource(R.drawable.nahoft_icons_pascode_off_56)
        passcode_entry_layout.isVisible = false
        des_icon.setImageResource(R.drawable.nahoft_icons_des_off_56)
        destruction_code_layout.isVisible = false
    }

    private fun destructionCodeIsChecked() {
        passcodeSwitchImageView.setImageResource(R.drawable.nahoft_icons_passcode_on_56)
        passcode_switch.isVisible = true
        passcode_switch.isChecked = true
        passcode_entry_layout.isVisible = false
        des_icon.setImageResource(R.drawable.nahoft_icons_des_on_56)
        destruction_code_layout.isVisible = true
    }

    private fun handlePasscodeRequirementChange(required: Boolean) {
        if (required) {
       // If there aren't already saved passcodes prompt user
       // We will not update the status to logged in until the user has entered valid passcodes
       } else {
           // Status is NotRequired
           Persist.status = LoginStatus.NotRequired
       }
       updatePasscodeInputs(required)
    }

    private fun updatePasscodeInputs(passcodeRequired: Boolean) {
       if (passcodeRequired) {
           // Check for passcodes in shared preferences
           val maybePasscode = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefPasscodeKey, null)

           // Passcode
           if (maybePasscode != null) {
               passcode_switch.isChecked = true
               // Populate our text inputs
               enter_passcode_input.setText(maybePasscode)
               verify_passcode_input.setText(maybePasscode)
           }

           // Make sure that our passcodes are enabled
           enter_passcode_input.isEnabled = true
           verify_passcode_input.isEnabled = true
           passcode_submit_button.isEnabled = true

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

           passcode_submit_button.isEnabled = false
       }
    }

    private fun updateDestructionCodeInputs(passcodeRequired: Boolean) {
        if (passcodeRequired) {
        val maybeSecondary = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefSecondaryPasscodeKey, null)

        // Secondary Passcode
        if (maybeSecondary != null) {
            destruction_code_switch.isChecked = true
            // Populate our text inputs
            enter_secondary_passcode_input.setText(maybeSecondary)
            verify_secondary_passcode_input.setText(maybeSecondary)
        }
            // Make sure that our passcodes are enabled
            enter_secondary_passcode_input.isEnabled = true
            verify_secondary_passcode_input.isEnabled = true
            destruction_code_submit_button.isEnabled = true

        } else {
            destruction_code_switch.isChecked = false

            // Disable passcode inputs and clear them out

            enter_secondary_passcode_input.text?.clear()
            enter_secondary_passcode_input.isEnabled = false
            verify_secondary_passcode_input.text?.clear()
            verify_secondary_passcode_input.isEnabled = false

            destruction_code_submit_button.isEnabled = false
        }
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
                Persist.status = LoginStatus.LoggedIn

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
            Persist.status = LoginStatus.LoggedIn

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
    }
}

