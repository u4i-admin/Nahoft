package org.nahoft.nahoft.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_messages.*
import org.nahoft.codex.PersistenceEncryption
import org.nahoft.nahoft.*
import org.simpleframework.xml.core.Persister
import java.io.ByteArrayOutputStream
import java.lang.Exception

class MessagesActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: MessagesRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Setup the messages RecyclerView
        linearLayoutManager = LinearLayoutManager(this)
        adapter = MessagesRecyclerAdapter(Persist.messageList)
        messages_recycler_view.layoutManager = linearLayoutManager
        messages_recycler_view.adapter = adapter

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


