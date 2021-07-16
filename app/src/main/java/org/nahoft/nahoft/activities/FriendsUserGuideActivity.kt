package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_friends_user_guide.*
import org.nahoft.nahoft.R

class FriendsUserGuideActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends_user_guide)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        // Show user guide in English
        friends_user_guide_button_english.setOnClickListener {
            friends_user_guide_textView.text = getString(R.string.friends_user_guide_english)
        }

        // Show user guide in Persian
        friends_user_guide_button_persian.setOnClickListener {
            friends_user_guide_textView.text = getString(R.string.friends_user_guide_persian)
        }
    }
}