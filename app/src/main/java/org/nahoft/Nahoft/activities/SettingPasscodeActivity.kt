package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_passcode.*
import kotlinx.android.synthetic.main.activity_setting_passcode.*
import kotlinx.android.synthetic.main.activity_setting_passcode.passcode_switch
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R

class SettingPasscodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_passcode)

        setDefaultView()

        passcode_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // The switch is toggled on
                passcodeSwitchIsChecked()
            } else {
                // The switch is toggled off
                passcodeSwitchIsUnChecked()
                }
        }

        destruction_code_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // The destruction code switch is toggled on
                destructionCodeSwitchOnView()
            } else {
                // The switch is toggled off
                passcodeSwitchIsUnChecked()
            }
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

    private fun destructionCodeSwitchOnView() {
        passcodeSwitchImageView.setImageResource(R.drawable.nahoft_icons_passcode_on_56)
        passcode_switch.isVisible = true
        passcode_switch.isChecked = true
        passcode_entry_layout.isVisible = false
        des_icon.setImageResource(R.drawable.nahoft_icons_des_on_56)
        destruction_code_layout.isVisible = true
    }
}

