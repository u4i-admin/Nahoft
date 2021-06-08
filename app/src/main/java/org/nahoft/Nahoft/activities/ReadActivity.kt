package org.nahoft.nahoft.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_read.*
import org.nahoft.nahoft.R

class ReadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        // View Messages Button
        view_messages_button.setOnClickListener {
            val messagesIntent = Intent(this, MessagesActivity::class.java)
            startActivity(messagesIntent)
        }

        // Import Image Text Activity
        import_image_button.setOnClickListener{
            val importIntent = Intent(this, ImportImageTextActivity::class.java)
            startActivity(importIntent)}
    }

    private fun showDialogButtonMessagesMenuHelp() {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.dialog_button_messages_menu_help_title))
            .setMessage(resources.getString(R.string.dialog_button_messages_menu_help))
            .setPositiveButton(resources.getString(R.string.ok_button)) {
                    dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }
}