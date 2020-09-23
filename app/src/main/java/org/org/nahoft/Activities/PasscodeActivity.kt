package org.org.nahoft.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.security.crypto.EncryptedSharedPreferences
import kotlinx.android.synthetic.main.activity_passcode.*
import org.org.codex.PersistenceEncryption
import org.org.codex.PersistenceEncryption.Companion.sharedPrefPasscodeKey
import org.org.codex.PersistenceEncryption.Companion.sharedPrefSecondaryPasscodeKey
import org.org.nahoft.LoginStatus
import org.org.nahoft.Nahoft
import org.org.nahoft.Nahoft.Companion.status
import org.org.nahoft.R
import java.lang.Exception

class PasscodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passcode)

        // Shared Preferences
        Nahoft.encryptedSharedPreferences = EncryptedSharedPreferences.create(
            PersistenceEncryption.sharedPrefFilename,
            PersistenceEncryption.masterKeyAlias,
            this.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences

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

    override fun onBackPressed() {
        super.onBackPressed()

        saveStatus()
    }

    override fun onDestroy() {
        super.onDestroy()

        saveStatus()
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
            val maybePasscode = Nahoft.encryptedSharedPreferences.getString(sharedPrefPasscodeKey, null)
            val maybeSecondary = Nahoft.encryptedSharedPreferences.getString(sharedPrefSecondaryPasscodeKey, null)

            if (maybePasscode != null &&maybeSecondary != null) {
                // Populate our text inputs
                enter_passcode_input.setText(maybePasscode!!)
                verify_passcode_input.setText(maybePasscode!!)
                secondary_passcode_input.setText(maybeSecondary!!)
                verify_secondary_passcode_input.setText(maybeSecondary!!)
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

        // TODO: Verify and Save Passcodes
    }

    fun saveStatus() {
        Nahoft.encryptedSharedPreferences
            .edit()
            .putString(PersistenceEncryption.sharedPrefLoginStatusKey, status.name)
            .apply()
    }
}