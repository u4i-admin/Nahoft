package org.nahoft.nahoft.activities

import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_add_friend.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.FriendStatus
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.friendList
import org.nahoft.nahoft.R
import org.nahoft.util.showAlert

class AddFriendActivity : AppCompatActivity() {

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanup()
            showAlert("Add Friend Activity Logout Timer Broadcast Received", length = Toast.LENGTH_LONG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        // Listen for App timeout
        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        saveFriendButton.setOnClickListener {
            saveFriendClick()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun saveFriendClick() {
        val friendName = nameTextField.text.toString()

        if (friendName == "") {
            return
        }

        val newFriend = Friend(friendName, FriendStatus.Default, null)

        // Only add the friend if one with the same name doesn't already exist.
        if (friendList.contains(newFriend)) {
            showAlert(getString(R.string.alert_text_friend_already_exists))
            return
        }

        friendList.add(newFriend)
        Persist.saveFriendsToFile(this)
        finish()
    }

    private fun cleanup(){
        nameTextField.text = null
        showAlert("Add Friend Logout Timer Broadcast Received", length = Toast.LENGTH_LONG)
    }
}