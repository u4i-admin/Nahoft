package org.nahoft.nahoft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_user_guide.*

class UserGuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_guide)

        friends_user_guide.setOnClickListener {
            println("message button clicked")
            val intent = Intent(this, FriendsUserGuideActivity::class.java)
            startActivity(intent)
        }

        sending_messages_user_guide.setOnClickListener {
            val intent = Intent(this, SendingMessagesUserGuideActivity::class.java)
            startActivity(intent)
        }

        settings_user_guide.setOnClickListener {
            val intent = Intent(this, SettingsUserGuideActivity::class.java)
            startActivity(intent)
        }

        receiving_messages_user_guide.setOnClickListener {
            val intent = Intent(this, ReceivingMessageUserGuideActivity::class.java)
            startActivity(intent)
        }
    }
}