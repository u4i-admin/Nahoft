package org.nahoft.nahoft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*
import org.nahoft.nahoft.activities.ApplicationShortcutActivity
import org.nahoft.nahoft.activities.PasscodeActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

       // Go To ApplicationShortcutActivity
       /* application_shortcut_button.setOnClickListener {

            val setShortcutIntent = Intent(this, ApplicationShortcutActivity::class.java)
           startActivity(setShortcutIntent)
      }*/

        // Go to PasscodeActivity
        passcode_button.setOnClickListener {
            val passcodeIntent = Intent(this, PasscodeActivity::class.java)
            startActivity(passcodeIntent)
        }

    }
}

