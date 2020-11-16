package org.nahoft.nahoft.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_messages.*
import kotlinx.android.synthetic.main.activity_messages_menu.*
import org.nahoft.nahoft.R

class MessagesMenuActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages_menu)


        // View Messages Button
        view_messages_button.setOnClickListener {
            val messagesIntent = Intent(this, MessagesActivity::class.java)
            startActivity(messagesIntent)
        }

        // Compose Message Button
        // Compose new message button
        compose_messages_button.setOnClickListener {
            val newMessageIntent = Intent(this, NewMessageActivity::class.java)
            startActivity(newMessageIntent)
        }

        // Import Image Text Activity
        import_button.setOnClickListener{
            val importIntent = Intent(this, ImportImageTextActivity::class.java)
            startActivity(importIntent)}
    }
}