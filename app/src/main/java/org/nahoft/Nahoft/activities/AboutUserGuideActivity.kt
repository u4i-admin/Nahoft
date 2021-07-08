package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_about_user_guide.*
import org.nahoft.nahoft.R

class AboutUserGuideActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_user_guide)

        // Show user guide in English
        about_user_guide_button_english.setOnClickListener {
            about_user_guide_textView.text = getString(R.string.about_user_guide_english)
        }

        // Show user guide in Persian
        about_user_guide_button_persian.setOnClickListener {
            about_user_guide_textView.text = getString(R.string.about_user_guide_persian)
        }
    }
}