package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_messages.*
import org.nahoft.nahoft.*
import org.nahoft.nahoft.ui.ItemDragListener
import org.nahoft.nahoft.ui.ItemTouchHelperCallback

class MessagesActivity : AppCompatActivity(), ItemDragListener {

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

    override fun onResume() {
        super.onResume()

        messages_recycler_view.adapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        Persist.saveMessagesToFile(this)
        adapter.cleanup()
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter, this))
        itemTouchHelper.attachToRecyclerView(messages_recycler_view)
    }

}


