package org.nahoft.nahoft.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*
import org.nahoft.nahoft.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        passcode_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // The switch is toggled on
                passcode_switch.setBackgroundResource(R.drawable.nahoft_icons_passcode_on_56)
            } else {
                // The switch is toggled off
                passcode_switch.setBackgroundResource(R.drawable.nahoft_icons_pascode_off_56)
            }
        }

        // Go to PasscodeActivity
        passcode_button.setOnClickListener {
            val passcodeIntent = Intent(this, PasscodeActivity::class.java)
            startActivity(passcodeIntent)
        }

    }
}

