package org.org.nahoft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        set_language.setOnClickListener {
            val setLanguageIntent = Intent(this, SetLanguage::class.java)
            startActivity(setLanguageIntent)
        }
    }
}

