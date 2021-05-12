package org.nahoft.nahoft.activities

import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_messages.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.nahoft.ui.ItemDragListener
import org.nahoft.nahoft.ui.ItemTouchHelperCallback
import org.nahoft.util.showAlert

class MessagesActivity : AppCompatActivity(), ItemDragListener {

   /* private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
        }
    }*/

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
        setupItemTouchHelper()

        val dividerHeightInPixels = resources.getDimensionPixelSize(R.dimen.list_item_divider_height)
        val dividerDecoration = org.nahoft.nahoft.ui.DividerItemDecoration(ContextCompat.getColor(this, R.color.colorPrimary), dividerHeightInPixels)
        messages_recycler_view.addItemDecoration(dividerDecoration)
    }

/*    override fun onStop() {
        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })
        //adapter.cleanup()
        showAlert("Messages Activity Logout Timer Broadcast Received", length = Toast.LENGTH_LONG)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        //messages_recycler_view.adapter?.notifyDataSetChanged()
        unregisterReceiver(receiver)
    }*/

    override fun onDestroy() {
        super.onDestroy()
        Persist.saveMessagesToFile(this)
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter, this))
        itemTouchHelper.attachToRecyclerView(messages_recycler_view)
    }

}


