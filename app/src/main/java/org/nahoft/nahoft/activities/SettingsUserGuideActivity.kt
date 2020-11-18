package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings_user_guide.*
import org.nahoft.nahoft.R

class SettingsUserGuideActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_user_guide)
        // Show user guide in English
        settings_ug_english_button.setOnClickListener {
            settings_ug_textView.text = getString(R.string.passcodes_user_guide_english)
        }

        // Show user guide in Persian
        settings_ug_persian_button.setOnClickListener {
            settings_ug_textView.text = getString(R.string.passcodes_user_guide_persian)
        }
    }
}