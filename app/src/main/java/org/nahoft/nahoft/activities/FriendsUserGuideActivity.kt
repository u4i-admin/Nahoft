package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_friends_user_guide.*
import org.nahoft.nahoft.R

class FriendsUserGuideActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends_user_guide)

        // Show user guide in English
        friend_ug_english_button.setOnClickListener {
            friend_ug_textView.text = getString(R.string.friends_user_guide_english)
        }

        // Show user guide in Persian
        friend_ug_persian_button.setOnClickListener {
            friend_ug_textView.text = getString(R.string.friends_user_guide_persian)
        }
    }
}