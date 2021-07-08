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
        user_guide_about_button_english.setOnClickListener {
            user_guide_about_textView.text = getString(R.string.user_guide_about_english)
        }

        // Show user guide in Persian
        user_guide_about_button_persian.setOnClickListener {
            user_guide_about_textView.text = getString(R.string.user_guide_about_persian)
        }
    }
}