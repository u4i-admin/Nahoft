package org.nahoft.nahoft.activities

import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_messages.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.MessagesRecyclerAdapter
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R

class MessagesActivity : AppCompatActivity()
{

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            adapter.cleanup()
        }
    }

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: MessagesRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        // Setup the messages RecyclerView
        linearLayoutManager = LinearLayoutManager(this)
        adapter = MessagesRecyclerAdapter(Persist.messageList)
        messages_recycler_view.layoutManager = linearLayoutManager
        messages_recycler_view.adapter = adapter
    }

    override fun onResume()
    {
        super.onResume()
        messages_recycler_view.adapter?.notifyDataSetChanged()
    }

    override fun onDestroy()
    {
        try
        {
            unregisterReceiver(receiver)
        }
        catch (e: Exception)
        {
            //Nothing to unregister
        }

        Persist.saveMessagesToFile(this)
        super.onDestroy()
    }

}


