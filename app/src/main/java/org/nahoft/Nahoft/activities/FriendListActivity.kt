package org.nahoft.nahoft.activities

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_friend_list.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.nahoft.ui.ItemDragListener
import org.nahoft.nahoft.ui.ItemTouchHelperCallback
import org.nahoft.util.RequestCodes
import org.nahoft.util.showAlert

class FriendListActivity : AppCompatActivity(), ItemDragListener {

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            adapter.cleanup()
        }
    }

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: FriendsRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_list)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        linearLayoutManager = LinearLayoutManager(this)
        adapter = FriendsRecyclerAdapter(Persist.friendList)
        friendsRecyclerView.layoutManager = linearLayoutManager
        friendsRecyclerView.adapter = adapter
        setupItemTouchHelper()

        add_friend_button.setOnClickListener {
           showAddFriendDialog()
        }
    }

    override fun onDestroy()
    {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onRestart()
    {
        super.onRestart()
        adapter.notifyDataSetChanged()
    }

    private fun setupItemTouchHelper()
    {
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter, this))
        itemTouchHelper.attachToRecyclerView(friendsRecyclerView)
    }

    private fun showAddFriendDialog()
    {
        val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AddFriendAlertDialogTheme))
        builder.setTitle(resources.getString(R.string.enter_nickname))

        // Set the input - EditText
        val inputEditText = EditText(this)
        inputEditText.setBackgroundResource(R.drawable.grey_56_btn_bkgd)
        inputEditText.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        inputEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        inputEditText.isSingleLine = true
        builder.setView(inputEditText)

        // Set the Add and Cancel Buttons
        builder.setPositiveButton(resources.getString(R.string.add_button))
        {
            dialog, _->
                if (!inputEditText.text.isEmpty())
                {
                    val friendName = inputEditText.text.toString()
                    val newFriend = saveFriend(friendName)
                    val friendInfoActivityIntent = Intent(this, FriendInfoActivity::class.java)
                    friendInfoActivityIntent.putExtra(RequestCodes.friendExtraTaskDescription, newFriend)
                    startActivity(friendInfoActivityIntent)
                }
        }

        builder.setNeutralButton(resources.getString(R.string.cancel_button)) {
            dialog, _->
                dialog.cancel()
        }

        .create()
        .show()
    }

    private fun saveFriend(friendName: String) : Friend?
    {
        val newFriend = Friend(friendName, FriendStatus.Default, null)

        // Only add the friend if one with the same name doesn't already exist.
        if (Persist.friendList.contains(newFriend)) {
            showAlert(getString(R.string.alert_text_friend_already_exists))
            return null
        }

        Persist.friendList.add(newFriend)
        Persist.saveFriendsToFile(this)
        return newFriend
    }
}