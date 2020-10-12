package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_add_friend.*
import kotlinx.android.synthetic.main.activity_friends.*
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.FriendStatus
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.friendList
import org.nahoft.nahoft.R

class AddFriendActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        saveFriendButton.setOnClickListener {
            saveFriendClick()
        }
    }

    fun saveFriendClick() {
        val friendName = nameTextField.text.toString()

        if (friendName == "") {
            return
        }

        val newFriend = Friend(friendName, FriendStatus.Default, null)
        friendList.add(newFriend)
        Persist.saveFriendsToFile(this)
    }
}