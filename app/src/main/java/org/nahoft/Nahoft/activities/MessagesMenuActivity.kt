package org.nahoft.nahoft.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_messages_menu.*
import kotlinx.android.synthetic.main.activity_messages_menu.messages_menu_help_button
import org.nahoft.nahoft.R

class MessagesMenuActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages_menu)

        fun showDialogButtonMessagesMenuHelp() {
            MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle(resources.getString(R.string.dialog_button_messages_menu_help_title))
                .setMessage(resources.getString(R.string.dialog_button_messages_menu_help))
                .setNeutralButton(resources.getString(R.string.ok_button)) {
                        dialog, _ ->
                    dialog.cancel()
                }
                .show()
        }

        // Messages Menu Help Button

        messages_menu_help_button.setOnClickListener {showDialogButtonMessagesMenuHelp()}

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