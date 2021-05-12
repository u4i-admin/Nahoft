package org.nahoft.nahoft.activities

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_friend_selection.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.util.RequestCodes
import org.nahoft.util.showAlert


class FriendSelectionActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: FriendSelectionRecyclerAdapter

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            adapter.cleanup()
            showAlert("Friends Activity Logout Timer Broadcast Received", length = Toast.LENGTH_LONG)
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, FriendSelectionActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_selection)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        // Only show friends that have been verified
        val approvedFriends = ArrayList<Friend>()

        for (friend in Persist.friendList) {

            if (friend.status == FriendStatus.Verified) {
                approvedFriends.add(friend)
            }
        }

        linearLayoutManager = LinearLayoutManager(this)
        adapter = FriendSelectionRecyclerAdapter(approvedFriends) {

            // This is the onClick listener for our Recycler
            val result = Intent()
            result.putExtra(RequestCodes.friendExtraTaskDescription, it)
            setResult(RESULT_OK, result)

            finish()
        }

        friend_selection_recycler_view.layoutManager = linearLayoutManager
        friend_selection_recycler_view.adapter = adapter
    }

    override fun onRestart() {
        super.onRestart()
        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }
}

