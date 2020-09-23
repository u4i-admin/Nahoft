package org.org.nahoft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_home.*
import java.io.File

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        messages_button.setOnClickListener {
            println("message button clicked")
            val messagesIntent = Intent(this, MessagesActivity::class.java)
            startActivity(messagesIntent)
        }

        user_guide_button.setOnClickListener {
            val userGuideIntent = Intent(this, UserGuideActivity::class.java)
            startActivity(userGuideIntent)
        }

        friends_button.setOnClickListener {
            val friendsIntent = Intent(this, FriendsActivity::class.java)
            startActivity(friendsIntent)
        }

        settings_button.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        loadSavedFriends()
    }

    fun loadSavedFriends() {

        Nahoft.friendsFile = File(filesDir.absolutePath + File.separator + FileConstants.datasourceFilename )

        // Load our existing friends list from our encrypted file
        if (Nahoft.friendsFile.exists()) {
            val friendsToAdd = FriendViewModel.getFriends(Nahoft.friendsFile, applicationContext)
            Nahoft.friendList.addAll(friendsToAdd)
        }
    }
}

