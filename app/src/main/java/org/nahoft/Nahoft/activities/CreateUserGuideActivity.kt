package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_create_user_guide.*
import org.nahoft.nahoft.R

class CreateUserGuideActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user_guide)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        // Show user guide in English
        create_user_guide_button_english.setOnClickListener {
            create_user_guide_textView.text = getString(R.string.create_user_guide_english)
        }

        // Show user guide in Persian
        create_user_guide_button_persian.setOnClickListener {
            create_user_guide_textView.text = getString(R.string.create_user_guide_persian)
        }
    }
}