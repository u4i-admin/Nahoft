package org.nahoft.nahoft.activities

import android.R.attr.text
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.core.view.isGone
import com.google.android.material.snackbar.Snackbar
import org.nahoft.Nahoft.utils.registerReceiverCompat
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.LoginStatus
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.nahoft.databinding.ActivitySettingPasscodeBinding
import org.nahoft.nahoft.slideNameSetting
import org.nahoft.util.showAlert


class SettingPasscodeActivity : AppCompatActivity()
{
    private lateinit var binding: ActivitySettingPasscodeBinding
    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingPasscodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        registerReceiverCompat(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        }, exported = false)

        val codex = Codex()
        val userCode = codex.encodeKey(Encryption().ensureKeysExist().publicKey.toBytes())
        binding.userPublicKeyEdittext.setText(userCode)

        setupButtons()
        setDefaultView()
    }

    override fun onBackPressed() {
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
        binding.passcodeSwitch.setOnCheckedChangeListener { _, isChecked ->
            handlePasscodeRequirementChange(isChecked)
        }

        binding.destructionCodeSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleDestructionCodeRequirementChange(isChecked)
        }

//        use_sms_as_default_switch.setOnCheckedChangeListener { _, isChecked ->
//            Persist.saveBooleanKey(Persist.sharedPrefUseSmsAsDefaultKey, isChecked)
//        }

        binding.passcodeSubmitButton.setOnClickListener {
            savePasscode()
        }

        binding.destructionCodeSubmitButton.setOnClickListener {
            saveDestructionCode()
        }

        binding.settingGuideButton.setOnClickListener {
            val slideActivity = Intent(this, SlideActivity::class.java)
            slideActivity.putExtra(Intent.EXTRA_TEXT, slideNameSetting)
            startActivity(slideActivity)
        }

        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.copyPublicKeyButton.setOnClickListener {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", binding.userPublicKeyEdittext.text))
            this.showAlert(getString(R.string.copied))
        }
    }

    private fun setDefaultView()
    {
        binding.destructionCodeEntryLayout.isGone = true
//        use_sms_as_default_switch.isChecked = Persist.loadBooleanKey(Persist.sharedPrefUseSmsAsDefaultKey)
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
        binding.passcodeSwitch.isChecked = true
        binding.passcodeEntryLayout.isGone = entryHidden

        // We will not update the status to logged in until the user has entered valid passcodes
        // Check for passcodes in shared preferences
        val maybePasscode = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefPasscodeKey, null)

        // Passcode
        if (maybePasscode != null)
        {
            // Populate our text inputs
            binding.enterPasscodeInput.setText(maybePasscode)
            binding.verifyPasscodeInput.setText(maybePasscode)
            binding.destructionCodeSwitch.isEnabled = true // Destruction code switch should only be enabled if a passcode exists
        }

        // Make sure that our passcodes are enabled
        binding.enterPasscodeInput.isEnabled = true
        binding.verifyPasscodeInput.isEnabled = true
        binding.passcodeSubmitButton.isEnabled = true

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
        binding.passcodeSwitch.isChecked = false
        binding.passcodeEntryLayout.isGone = true

        // Disable passcode inputs and clear them out
        binding.enterPasscodeInput.text?.clear()
        binding.enterPasscodeInput.isEnabled = false
        binding.verifyPasscodeInput.text?.clear()
        binding.verifyPasscodeInput.isEnabled = false
        binding.passcodeSubmitButton.isEnabled = false

        binding.destructionCodeSwitch.isEnabled = false
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
            binding.destructionCodeInput.setText(maybeDestructionCode)
            binding.verifyDestructionCodeInput.setText(maybeDestructionCode)
        }

        binding.destructionCodeSwitch.isChecked = true

        // Make sure that our passcodes are enabled
        binding.destructionCodeInput.isEnabled = true
        binding.verifyDestructionCodeInput.isEnabled = true
        binding.destructionCodeSubmitButton.isEnabled = true
        binding.destructionCodeEntryLayout.isGone = entryHidden
    }

    private fun updateViewDestructionCodeOff()
    {
        binding.destructionCodeSwitch.isChecked = false
        binding.destructionCodeEntryLayout.isGone = true

        // Disable passcode inputs and clear them out
        binding.destructionCodeInput.text?.clear()
        binding.destructionCodeInput.isEnabled = false
        binding.verifyDestructionCodeInput.text?.clear()
        binding.verifyDestructionCodeInput.isEnabled = false
        binding.destructionCodeSubmitButton.isEnabled = false
    }

    private fun handlePasscodeRequirementChange(required: Boolean)
    {
        if (required)
        {
            if (!isBiometricAvailable()) {
                val snack = Snackbar.make(findViewById(R.id.settingsActivityLayoutContainer), getString(R.string.you_have_to_set_a_lock_screen), Snackbar.LENGTH_LONG)
                snack.setAction(getString(R.string.click_to_set)) {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                    }
                    startActivity(enrollIntent)
                }
                snack.show()
                binding.passcodeSwitch.isChecked = false
                return
            }
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
        val passcode = binding.enterPasscodeInput.text.toString()
        val passcode2 = binding.verifyPasscodeInput.text.toString()

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
        binding.destructionCodeSwitch.isEnabled = true
        binding.passcodeEntryLayout.isGone = true
        showAlert(getString(R.string.alert_passcode_saved))
    }

    private fun saveDestructionCode()
    {
        val secondaryPasscode = binding.destructionCodeInput.text.toString()
        val secondaryPasscode2 = binding.verifyDestructionCodeInput.text.toString()
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

            binding.destructionCodeEntryLayout.isGone = true
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

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            else -> false
        }
    }

    private fun cleanup(){
        binding.enterPasscodeInput.text?.clear()
        binding.verifyPasscodeInput.text?.clear()
        binding.destructionCodeInput.text?.clear()
        binding.verifyDestructionCodeInput.text?.clear()
    }
}

