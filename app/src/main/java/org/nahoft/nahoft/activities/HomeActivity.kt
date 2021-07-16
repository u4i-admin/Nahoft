package org.nahoft.nahoft.activities

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.nahoft.Persist.Companion.friendsFilename
import org.nahoft.nahoft.Persist.Companion.messagesFilename
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.util.showAlert
import java.io.File

class HomeActivity : AppCompatActivity()
{
    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            //
        }
    }

    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        // Prepare persisted data
        Persist.app = Nahoft()
        Persist.loadEncryptedSharedPreferences(this.applicationContext)

        makeSureAccessIsAllowed()

        // Logout Button
        if (status == LoginStatus.NotRequired) {
            logout_button.visibility = View.INVISIBLE
        } else {
            logout_button.visibility = View.VISIBLE
        }

        setupOnClicks()
        setupFriends()
        loadSavedMessages()
    }

    override fun onResume()
    {
        super.onResume()

        if (status == LoginStatus.NotRequired)
        {
            logout_button.visibility = View.INVISIBLE
        }
        else
        {
            logout_button.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun makeSureAccessIsAllowed()
    {
        Persist.getStatus()

        if (status == LoginStatus.NotRequired || status == LoginStatus.LoggedIn)
        {
            return
        }
        else
        {
            sendToLogin()
        }
    }

    private fun sendToLogin()
    {
        // If the status is not either NotRequired, or Logged in, request login
        this.showAlert(getString(R.string.alert_text_passcode_required_to_proceed))

        // Send user to the EnterPasscode Activity
        val loginIntent = Intent(applicationContext, LogInActivity::class.java)
        startActivity(loginIntent)
    }

    private fun loadSavedFriends()
    {
        // Load our existing friends list from our encrypted file
        if (Persist.friendsFile.exists())
        {
            val friendsToAdd = FriendViewModel.getFriends(Persist.friendsFile, applicationContext)

            for (newFriend in friendsToAdd)
            {
                // Only add this friend if the list does not contain a friend with that ID already
                if (!Persist.friendList.any { it.name == newFriend.name })
                {
                    Persist.friendList.add(newFriend)
                }
            }
        }
    }

    private fun setupFriends()
    {
        Persist.friendsFile = File(filesDir.absolutePath + File.separator + friendsFilename )
        loadSavedFriends()
    }

    private fun loadSavedMessages()
    {
        Persist.messagesFile = File(filesDir.absolutePath + File.separator + messagesFilename )

        // Load messages from file
        if (Persist.messagesFile.exists())
        {
            val messagesToAdd = MessageViewModel().getMessages(Persist.messagesFile, applicationContext)
            messagesToAdd?.let {
                Persist.messageList.clear()
                Persist.messageList.addAll(it)
            }

        }
    }

    // Logout Button Handler
    private fun logoutButtonClicked()
    {
        status = LoginStatus.LoggedOut
        Persist.saveLoginStatus()

        val returnToLoginIntent = Intent(this, LogInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        startActivity(returnToLoginIntent)

        finish()
    }

    private fun setupOnClicks()
    {
        logout_button.setOnClickListener {
            logoutButtonClicked()
        }

        // Read
        read_button.setOnClickListener {
            val messagesIntent = Intent(this, ReadActivity::class.java)
            startActivity(messagesIntent)
        }

        create_button.setOnClickListener {
            val createIntent = Intent(this, CreateActivity::class.java)
            startActivity(createIntent)
        }

        // User Guide
        user_guide_button.setOnClickListener {
            val userGuideIntent = Intent(this, HelpActivity::class.java)
            startActivity(userGuideIntent)
        }

        // Friends
        friends_button.setOnClickListener {
            val friendsIntent = Intent(this, FriendListActivity::class.java)
            startActivity(friendsIntent)
        }

        // Settings
        settings_button.setOnClickListener {
            val settingsIntent = Intent(this, SettingPasscodeActivity::class.java)
            startActivity(settingsIntent)
        }
    }

}
