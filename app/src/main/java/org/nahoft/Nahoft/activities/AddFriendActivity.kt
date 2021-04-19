package org.nahoft.nahoft.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_add_friend.*
import kotlinx.android.synthetic.main.friend_recyclerview_item_row.*
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.FriendStatus
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.friendList
import org.nahoft.nahoft.R
import org.nahoft.showAlert

class AddFriendActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        saveFriendButton.setOnClickListener {
            saveFriendClick()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    fun cleanup(){
        nameTextField.text = null
    }
}