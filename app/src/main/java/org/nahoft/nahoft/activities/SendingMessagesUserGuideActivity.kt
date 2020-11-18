package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_sending_messages_user_guide.*
import org.nahoft.nahoft.R

class SendingMessagesUserGuideActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sending_messages_user_guide)

        // Show user guide in English
        sending_messages_ug_english_button.setOnClickListener {
            sending_messages_ug_textView.text = getString(R.string.sending_messages_user_guide_english)
        }

        // Show user guide in Persian
        sending_messages_ug_persian_button.setOnClickListener {
            sending_messages_ug_textView.text = getString(R.string.sending_messages_user_guide_persian)
        }
    }
}