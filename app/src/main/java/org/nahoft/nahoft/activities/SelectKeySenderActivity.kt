package org.nahoft.nahoft.activities

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_select_key_sender.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.util.RequestCodes

class SelectKeySenderActivity : AppCompatActivity() {

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            adapter.cleanup()
        }
    }

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: SelectKeySenderRecyclerAdapter

/*    companion object {
        fun newIntent(context: Context) = Intent(context, SelectKeySenderActivity::class.java)
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_key_sender)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        // Only allow the user to select a friend that is in the default or invited state
        val possibleKeySenders = ArrayList<Friend>()

        for (friend in Persist.friendList) {
            if (friend.status == FriendStatus.Default || friend.status == FriendStatus.Invited) {
                possibleKeySenders.add(friend)
            }
        }

        linearLayoutManager = LinearLayoutManager(this)
        adapter = SelectKeySenderRecyclerAdapter(possibleKeySenders) {

            // On click listener for the recycler view
            val result = Intent()
            result.putExtra(RequestCodes.friendExtraTaskDescription, it)
            setResult(RESULT_OK, result)
            finish()
        }

        select_k_sender_recycler_view.layoutManager = linearLayoutManager
        select_k_sender_recycler_view.adapter = adapter
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

}