package org.nahoft.nahoft.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_friends.*
import org.nahoft.nahoft.FriendsRecyclerAdapter
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.nahoft.ui.ItemDragListener
import org.nahoft.nahoft.ui.ItemTouchHelperCallback

class FriendsActivity : AppCompatActivity(), ItemDragListener {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: FriendsRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        linearLayoutManager = LinearLayoutManager(this)
        adapter = FriendsRecyclerAdapter(Persist.friendList)
        friendsRecyclerView.layoutManager = linearLayoutManager
        friendsRecyclerView.adapter = adapter
        setupItemTouchHelper()

        add_friend_button.setOnClickListener {
            val addFriendIntent = Intent(this, AddFriendActivity::class.java)
            startActivity(addFriendIntent)
        }
    }

    override fun onResume() {
        super.onResume()

        adapter.notifyDataSetChanged()
    }

    // TODO: call  adapter.cleanup() when broadcast received

    // Friends Help Button
    fun showDialogButtonFriendsHelp(view: View) {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.dialog_button_friends_help_title))
            .setMessage(resources.getString(R.string.dialog_button_friends_help))
            .setPositiveButton(resources.getString(R.string.ok_button)) {
                    dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter, this))
        itemTouchHelper.attachToRecyclerView(friendsRecyclerView)
    }

}