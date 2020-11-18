package org.nahoft.nahoft.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_select_message_sender.*
import org.nahoft.nahoft.*
import org.nahoft.util.RequestCodes

class SelectMessageSenderActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: SelectMessageSenderRecyclerAdapter

    companion object {
        fun newIntent(context: Context) = Intent(context, SelectMessageSenderActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_message_sender)

        // Only try to decrypt messages from friends with the Approved status
        val verifiedFriends = ArrayList<Friend>()

        for (friend in Persist.friendList) {
            if (friend.status == FriendStatus.Verified) {
                verifiedFriends.add(friend)
            }
        }

        linearLayoutManager = LinearLayoutManager(this)
        adapter = SelectMessageSenderRecyclerAdapter(verifiedFriends) {

            // On click listener for the recycler view
            val result = Intent()
            result.putExtra(RequestCodes.friendExtraTaskDescription, it)
            setResult(RESULT_OK, result)
            finish()
        }

        select_m_sender_recycler_view.layoutManager = linearLayoutManager
        select_m_sender_recycler_view.adapter = adapter
    }

}