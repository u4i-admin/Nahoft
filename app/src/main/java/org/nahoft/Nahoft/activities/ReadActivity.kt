package org.nahoft.nahoft.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_read.*
import org.nahoft.nahoft.R

class ReadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        // View Messages Button
        view_messages_button.setOnClickListener {
            val messagesIntent = Intent(this, MessagesActivity::class.java)
            startActivity(messagesIntent) }

        // Import Text Activity
        import_text_button.setOnClickListener{
            val importTextIntent = Intent ( this, ImportTextActivity::class.java)
            startActivity(importTextIntent)
        }

        // Import Image Activity
        import_image_button.setOnClickListener{
            val importIntent = Intent(this, ImportImageActivity::class.java)
            startActivity(importIntent) }
    }
}