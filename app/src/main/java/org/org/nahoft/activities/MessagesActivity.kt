package org.org.nahoft.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_messages.*
import org.org.codex.PersistenceEncryption
import org.org.nahoft.Friends
import org.org.nahoft.NewMessageActivity
import org.org.nahoft.Persist
import org.org.nahoft.R
import org.simpleframework.xml.core.Persister
import java.io.ByteArrayOutputStream
import java.lang.Exception

class MessagesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Compose new message button
        new_message.setOnClickListener {
            val newMessageIntent = Intent(this, NewMessageActivity::class.java)
            startActivity(newMessageIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        saveMessagesToFile()
    }

    private fun saveMessagesToFile() {
        val serializer = Persister()
        val outputStream = ByteArrayOutputStream()

        val messagesObject = Friends(Persist.friendList)
        try { serializer.write(messagesObject, outputStream) } catch (e: Exception) {
            print("Failed to serialize our messages list: $e")
            return
        }

        PersistenceEncryption().writeEncryptedFile(Persist.messagesFile, outputStream.toByteArray(), applicationContext)
    }
}


