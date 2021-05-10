package org.nahoft.nahoft.activities

import android.content.IntentFilter
import android.os.Bundle
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        saveFriendButton.setOnClickListener {
            saveFriendClick()
        }
    }

    override fun onStop() {

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })
        cleanup()
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        unregisterReceiver(receiver)
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
        //showAlert("Add Friend Logout Timer Broadcast Received", length = Toast.LENGTH_LONG)
    }
}