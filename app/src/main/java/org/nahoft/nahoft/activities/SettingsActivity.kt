package org.nahoft.nahoft.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import org.nahoft.nahoft.utils.registerReceiverCompat
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.models.LoginStatus
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.nahoft.databinding.ActivitySettingsBinding
import org.nahoft.nahoft.models.slideNameSetting
import org.nahoft.nahoft.fragments.AppearanceDialogFragment
import org.nahoft.util.AppIconManager
import org.nahoft.util.applySecureFlag
import org.nahoft.util.showAlert


class SettingsActivity : AppCompatActivity()
{
    private lateinit var binding: ActivitySettingsBinding
    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.headerSection.updatePadding(top = systemBars.top)
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }

        window.applySecureFlag()

        registerReceiverCompat(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        }, exported = false)

        val codex = Codex()
        val userCode = codex.encodeKey(Encryption().ensureKeysExist().toBytes())
        binding.tvPublicKey.text = userCode

        setDefaultView()
        updateAppearanceLabel()
        setupButtons()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("MissingSuperCall")
    override fun onBackPressed()
    {
        Persist.saveLoginStatus()
    }

    override fun onDestroy()
    {
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
        // Tapping anywhere on the row toggles the switch.
        // The SwitchCompat itself has clickable/focusable disabled in the layout
        // to prevent the touch event from being consumed twice.
        binding.passcodeRow.setOnClickListener {
            binding.passcodeSwitch.toggle()
        }

        binding.passcodeSwitch.setOnCheckedChangeListener { _, isChecked ->
            handlePasscodeRequirementChange(isChecked)
        }

        binding.destructionCodeRow.setOnClickListener {
            if (binding.destructionCodeSwitch.isEnabled)
            {
                binding.destructionCodeSwitch.toggle()
            }
        }

        binding.destructionCodeSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleDestructionCodeRequirementChange(isChecked)
        }

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
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", binding.tvPublicKey.text))
            this.showAlert(getString(R.string.copied))
        }

        binding.appAppearanceRow.setOnClickListener {
            showAppearanceDialog()
        }
    }

    private fun setDefaultView()
    {
        binding.destructionCodeEntryLayout.isGone = true
        if (Persist.status == LoginStatus.LoggedIn)
        {
            updateViewPasscodeOn(true)
        }
        else
        {
            updateViewPasscodeOff()
        }
    }

    private fun showAppearanceDialog()
    {
        val current = AppIconManager.getActiveIdentity(this)
        AppearanceDialogFragment.newInstance(current).apply {
            onIdentitySelected = { identity ->
                AppIconManager.setActiveIdentity(this@SettingsActivity, identity)
                updateAppearanceLabel()
            }
        }.show(supportFragmentManager, "appearance")
    }

    private fun updateAppearanceLabel()
    {
        val current = AppIconManager.getActiveIdentity(this)
        val sizePx = resources.getDimensionPixelSize(R.dimen.app_icon_preview_size)

        val bitmap = BitmapFactory.decodeResource(resources, current.dialogIconRes)

        if (bitmap != null)
        {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
            val circularIcon = RoundedBitmapDrawableFactory.create(resources, scaledBitmap).apply {
                isCircular = true
            }
            binding.ivAppAppearanceIcon.setImageDrawable(circularIcon)
        }

        binding.appAppearanceCurrent.text = getString(
            R.string.app_appearance_current,
            getString(current.labelRes)
        )
    }

    private fun updateViewPasscodeOn(entryHidden: Boolean)
    {
        binding.passcodeSwitch.isChecked = true
        binding.passcodeEntryLayout.isGone = entryHidden

        val maybePasscode = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefPasscodeKey, null)

        if (maybePasscode != null)
        {
            binding.enterPasscodeInput.setText(maybePasscode)
            binding.verifyPasscodeInput.setText(maybePasscode)
            binding.destructionCodeSwitch.isEnabled = true
        }

        binding.enterPasscodeInput.isEnabled = true
        binding.verifyPasscodeInput.isEnabled = true
        binding.passcodeSubmitButton.isEnabled = true

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

    private fun updateViewPasscodeOff()
    {
        binding.passcodeSwitch.isChecked = false
        binding.passcodeEntryLayout.isGone = true

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
            binding.destructionCodeInput.setText(maybeDestructionCode)
            binding.verifyDestructionCodeInput.setText(maybeDestructionCode)
        }

        binding.destructionCodeSwitch.isEnabled = true
        binding.destructionCodeSwitch.isChecked = true

        binding.destructionCodeInput.isEnabled = true
        binding.verifyDestructionCodeInput.isEnabled = true
        binding.destructionCodeSubmitButton.isEnabled = true
        binding.destructionCodeEntryLayout.isGone = entryHidden
    }

    private fun updateViewDestructionCodeOff()
    {
        binding.destructionCodeSwitch.isChecked = false
        binding.destructionCodeEntryLayout.isGone = true

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
            if (!isBiometricAvailable())
            {
                val snack = Snackbar.make(
                    findViewById(R.id.settingsActivityLayoutContainer),
                    getString(R.string.you_have_to_set_a_lock_screen),
                    Snackbar.LENGTH_LONG
                )
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
            Persist.status = LoginStatus.NotRequired
        }
    }

    private fun handleDestructionCodeRequirementChange(passcodeRequired: Boolean)
    {
        if (passcodeRequired)
        {
            updateViewDestructionCodeOn(null, false)
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

        if (passcode != passcode2) {
            this.showAlert(getString(R.string.alert_text_passcode_entries_do_not_match))
            return
        }

        if (!passcodeMeetsRequirements(passcode)) return

        Persist.status = LoginStatus.LoggedIn
        Persist.saveKey(Persist.sharedPrefPasscodeKey, passcode)
        Persist.saveLoginStatus()

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

        if (secondaryPasscode == "")
        {
            if (secondaryPasscode2 != "")
            {
                this.showAlert(getString(R.string.alert_text_secondary_passcode_field_empty))
                return
            }
        }
        else
        {
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

    private fun isPasscodeCorrectLength(passcode: String): Boolean
    {
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

    private fun isPasscodeNonSequential(passcode: String): Boolean
    {
        val digitArray = passcode.map { it.toString().toInt() }.toTypedArray()
        val max = digitArray.maxOrNull()
        val min = digitArray.minOrNull()

        if (max == null || min == null)
        {
            showAlert(getString(R.string.alert_text_invalid_passcode))
            return false
        }

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

    private fun isPasscodeNonRepeating(passcode: String): Boolean
    {
        val firstChar = passcode[0]

        for (index in 1 until passcode.length)
        {
            val char = passcode[index]
            if (char != firstChar) return true
        }

        showAlert(getString(R.string.alert_text_passcode_is_a_repeated_digit))
        return false
    }

    private fun isBiometricAvailable(): Boolean
    {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL))
        {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    private fun cleanup()
    {
        binding.enterPasscodeInput.text?.clear()
        binding.verifyPasscodeInput.text?.clear()
        binding.destructionCodeInput.text?.clear()
        binding.verifyDestructionCodeInput.text?.clear()
    }
}