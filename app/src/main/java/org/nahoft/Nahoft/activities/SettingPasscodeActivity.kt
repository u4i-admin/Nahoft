package org.nahoft.nahoft.activities

import android.R.attr.text
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import kotlinx.android.synthetic.main.activity_setting_passcode.*
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.nahoft.slideNameSetting
import org.nahoft.util.showAlert


class SettingPasscodeActivity : AppCompatActivity() {

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_passcode)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        val codex = Codex()
        val userCode = codex.encodeKey(Encryption().ensureKeysExist().publicKey.toBytes())
        user_public_key_edittext.setText(userCode)

        setupButtons()
        setDefaultView()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Persist.saveLoginStatus()
    }

    override fun onDestroy() {
        Persist.saveLoginStatus()

        try
        {
            unregisterReceiver(receiver)
        }
        catch (e: Exception)
        {
            //Nothing to unregister
        }

        super.onDestroy()
    }

    private fun setupButtons()
    {
        passcode_switch.setOnCheckedChangeListener { _, isChecked ->
            handlePasscodeRequirementChange(isChecked)
        }

        destruction_code_switch.setOnCheckedChangeListener { _, isChecked ->
            handleDestructionCodeRequirementChange(isChecked)
        }

        use_sms_as_default_switch.setOnCheckedChangeListener { _, isChecked ->
            Persist.saveBooleanKey(Persist.sharedPrefUseSmsAsDefaultKey, isChecked)
        }

        passcode_submit_button.setOnClickListener {
            savePasscode()
        }

        destruction_code_submit_button.setOnClickListener {
            saveDestructionCode()
        }

        setting_guide_button.setOnClickListener {
            val slideActivity = Intent(this, SlideActivity::class.java)
            slideActivity.putExtra(Intent.EXTRA_TEXT, slideNameSetting)
            startActivity(slideActivity)
        }

        button_back.setOnClickListener {
            finish()
        }

        copy_public_key_button.setOnClickListener {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", user_public_key_edittext.text))
            this.showAlert(getString(R.string.copied))
        }
    }

    private fun setDefaultView()
    {
        destruction_code_entry_layout.isGone = true
        use_sms_as_default_switch.isChecked = Persist.loadBooleanKey(Persist.sharedPrefUseSmsAsDefaultKey)
        if (Persist.status == LoginStatus.LoggedIn)
        {
            updateViewPasscodeOn(true)
        }
        else
        {
            updateViewPasscodeOff()
        }
    }

    private fun updateViewPasscodeOn (entryHidden: Boolean)
    {
        passcode_switch.isChecked = true
        passcode_entry_layout.isGone = entryHidden

        // We will not update the status to logged in until the user has entered valid passcodes
        // Check for passcodes in shared preferences
        val maybePasscode = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefPasscodeKey, null)

        // Passcode
        if (maybePasscode != null)
        {
            // Populate our text inputs
            enter_passcode_input.setText(maybePasscode)
            verify_passcode_input.setText(maybePasscode)
            destruction_code_switch.isEnabled = true // Destruction code switch should only be enabled if a passcode exists
        }

        // Make sure that our passcodes are enabled
        enter_passcode_input.isEnabled = true
        verify_passcode_input.isEnabled = true
        passcode_submit_button.isEnabled = true

        // Check for Destruction Code
        val maybeDestructionCode = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefSecondaryPasscodeKey, null)

        if (maybeDestructionCode != null)
        {
            updateViewDestructionCodeOn(maybeDestructionCode, entryHidden)
        }
        else
        {
            updateViewDestructionCodeOff()
        }
    }

    private fun updateViewPasscodeOff () {
        passcode_switch.isChecked = false
        passcode_entry_layout.isGone = true

        // Disable passcode inputs and clear them out
        enter_passcode_input.text?.clear()
        enter_passcode_input.isEnabled = false
        verify_passcode_input.text?.clear()
        verify_passcode_input.isEnabled = false
        passcode_submit_button.isEnabled = false

        destruction_code_switch.isEnabled = false
        updateViewDestructionCodeOff()
    }

    private fun updateViewDestructionCodeOn(maybeDestructionCode: String? = null, entryHidden: Boolean)
    {
        // Secondary Passcode
        if (maybeDestructionCode == null)
        {
            val storedDestructionCode = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefSecondaryPasscodeKey, null)
            if (storedDestructionCode != null)
            {
                updateViewDestructionCodeOn(storedDestructionCode, entryHidden)
                return
            }
        }
        else
        {
            // Populate our text inputs
            destruction_code_input.setText(maybeDestructionCode)
            verify_destruction_code_input.setText(maybeDestructionCode)
        }

        destruction_code_switch.isChecked = true

        // Make sure that our passcodes are enabled
        destruction_code_input.isEnabled = true
        verify_destruction_code_input.isEnabled = true
        destruction_code_submit_button.isEnabled = true
        destruction_code_entry_layout.isGone = entryHidden
    }

    private fun updateViewDestructionCodeOff()
    {
        destruction_code_switch.isChecked = false
        destruction_code_entry_layout.isGone = true

        // Disable passcode inputs and clear them out
        destruction_code_input.text?.clear()
        destruction_code_input.isEnabled = false
        verify_destruction_code_input.text?.clear()
        verify_destruction_code_input.isEnabled = false
        destruction_code_submit_button.isEnabled = false
    }

    private fun handlePasscodeRequirementChange(required: Boolean)
    {
        if (required)
        {
            updateViewPasscodeOn(false)
        }
        else
        {
            updateViewPasscodeOff()
           // Status is NotRequired
           Persist.status = LoginStatus.NotRequired
       }
    }

    private fun handleDestructionCodeRequirementChange(passcodeRequired: Boolean)
    {
        if (passcodeRequired)
        {
            updateViewDestructionCodeOn(null,false)
        }
        else
        {
            updateViewDestructionCodeOff()
        }
    }

    private fun savePasscode()
    {
        val passcode = enter_passcode_input.text.toString()
        val passcode2 = verify_passcode_input.text.toString()

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

        // Set user status
        Persist.status = LoginStatus.LoggedIn
        Persist.saveKey(Persist.sharedPrefPasscodeKey, passcode)
        Persist.saveLoginStatus()

        // Allow Destruction Code
        destruction_code_switch.isEnabled = true
        passcode_entry_layout.isGone = true
        showAlert(getString(R.string.alert_passcode_saved))
    }

    private fun saveDestructionCode()
    {
        val secondaryPasscode = destruction_code_input.text.toString()
        val secondaryPasscode2 = verify_destruction_code_input.text.toString()
        val maybePasscode = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefPasscodeKey, null)

        if (maybePasscode == null)
        {
            showAlert(getString(R.string.alert_text_passcode_required_for_destruction_code))
            return
        }

        // If the user puts something in the verify secondary passcode field
        // but did not put something in the first secondary passcode field
        // show an error.
        if(secondaryPasscode == "")
        {
            if (secondaryPasscode2 != "")
            {
                this.showAlert(getString(R.string.alert_text_secondary_passcode_field_empty))
                return
            }
        }
        else
        { // User has entered a secondary passcode in the first field.
            if (secondaryPasscode2 == "")
            {
                this.showAlert(getString(R.string.alert_text_verify_secondary_passcode_empty))
                return
            }

            if (secondaryPasscode != secondaryPasscode2)
            {
                this.showAlert(getString(R.string.alert_text_secondary_passcode_entries_do_not_match))
                return
            }


            if (secondaryPasscode == maybePasscode)
            {
                this.showAlert(getString(R.string.alert_text_secondary_passcode_and_passcode_may_not_match))
                return
            }

            if (!passcodeMeetsRequirements(secondaryPasscode)) return

            Persist.saveKey(Persist.sharedPrefSecondaryPasscodeKey, secondaryPasscode)

            destruction_code_entry_layout.isGone = true
            showAlert(getString(R.string.alert_destruction_code_saved))
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
        enter_passcode_input.text?.clear()
        verify_passcode_input.text?.clear()
        destruction_code_input.text?.clear()
        verify_destruction_code_input.text?.clear()
    }
}

