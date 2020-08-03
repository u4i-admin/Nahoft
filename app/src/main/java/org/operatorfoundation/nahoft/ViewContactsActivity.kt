package org.operatorfoundation.nahoft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class ViewContactsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_contacts)

        messages_button.setOnClickListener {
            println("message button clicked")
            val intent = Intent(this, MessagesActivity::class.java)
            startActivity(intent)
        }
    }
}