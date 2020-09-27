package org.nahoft.Nahoft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*
import org.nahoft.Nahoft.activities.PasscodeActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Go To SetLanguageActivity
        set_language_button.setOnClickListener {
            val setLanguageIntent = Intent(this, SetLanguageActivity::class.java)
            startActivity(setLanguageIntent)
        }

        // Go To ApplicationShortcutActivity
        application_icon_button.setOnClickListener {
            val setShortcutIntent = Intent(this, ApplicationShortcutActivity::class.java)
            startActivity(setShortcutIntent)
        }

        // Go to SettingsActivity
        passcode_button.setOnClickListener {
            val passcodeIntent = Intent(this, PasscodeActivity::class.java)
            startActivity(passcodeIntent)
        }
    }
}

