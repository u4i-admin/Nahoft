package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_importing_user_guide.*
import org.nahoft.nahoft.R

class CreateUserGuideActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_importing_user_guide)

        // Show user guide in English
        import_ug_english_button.setOnClickListener {
            importing_ug_textView.text = getString(R.string.importing_user_guide_english)
        }

        // Show user guide in Persian
        import_ug_persian_button.setOnClickListener {
            importing_ug_textView.text = getString(R.string.importing_user_guide_persian)
        }
    }
}