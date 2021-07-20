package org.nahoft.nahoft.activities

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.InputType
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_friend_list.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.util.RequestCodes
import org.nahoft.util.showAlert

class FriendListActivity : AppCompatActivity()
{
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

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        linearLayoutManager = LinearLayoutManager(this)
        adapter = FriendsRecyclerAdapter(Persist.friendList)
        friendsRecyclerView.layoutManager = linearLayoutManager
        friendsRecyclerView.adapter = adapter

        add_friend_button.setOnClickListener {
           showAddFriendDialog()
        }
    }

    override fun onDestroy()
    {
        try
        {
            unregisterReceiver(receiver)
        }
        catch (e: Exception)
        {
            //Nothing to unregister
        }

        super.onDestroy()
    }

    override fun onRestart()
    {
        super.onRestart()
        adapter.notifyDataSetChanged()
    }

    private fun showAddFriendDialog()
    {
        val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_AddFriendAlertDialog))
        val title = SpannableString(getString(R.string.enter_nickname))

        // alert dialog title align center
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)

        // Set the input - EditText
        val inputEditText = EditText(this)
        inputEditText.setBackgroundResource(R.drawable.btn_bkgd_light_grey_outline_8)
        inputEditText.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        inputEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        inputEditText.isSingleLine = true
        inputEditText.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        builder.setView(inputEditText)

        // Set the Add and Cancel Buttons
        builder.setPositiveButton(resources.getString(R.string.add_button))
        {
            dialog, _->
                if (!inputEditText.text.isEmpty())
                {
                    val friendName = inputEditText.text.toString()
                    val newFriend = saveFriend(friendName)
                    if (newFriend != null) {
                        val friendInfoActivityIntent = Intent(this, FriendInfoActivity::class.java)
                        friendInfoActivityIntent.putExtra(RequestCodes.friendExtraTaskDescription, newFriend)
                        startActivity(friendInfoActivityIntent)
                    }
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
        if (Persist.friendList.contains(newFriend))
        {
            showAlert(getString(R.string.alert_text_friend_already_exists))
            return null
        }
        else
        {
            Persist.friendList.add(newFriend)
            Persist.saveFriendsToFile(this)
            return newFriend
        }
    }
}