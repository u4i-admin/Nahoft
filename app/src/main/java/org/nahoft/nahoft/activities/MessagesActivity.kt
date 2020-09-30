package org.nahoft.nahoft.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
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

        val dividerHeightInPixels = resources.getDimensionPixelSize(R.dimen.list_item_divider_height)
        val dividerDecoration = org.nahoft.nahoft.ui.DividerItemDecoration(ContextCompat.getColor(this, R.color.colorPrimary), dividerHeightInPixels)
        messages_recycler_view.addItemDecoration(dividerDecoration)


        // Compose new message button
        new_message.setOnClickListener {
            val newMessageIntent = Intent(this, NewMessageActivity::class.java)
            startActivity(newMessageIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Persist.saveMessagesToFile(this)
    }

}


