package org.nahoft.nahoft.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_friends.*
import org.nahoft.nahoft.FriendsRecyclerAdapter
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R

class FriendsActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: FriendsRecyclerAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        linearLayoutManager = LinearLayoutManager(this)
        adapter = FriendsRecyclerAdapter(Persist.friendList)
        friendsRecyclerView.layoutManager = linearLayoutManager
        friendsRecyclerView.adapter = adapter

        add_friend_button.setOnClickListener {
            val addFriendIntent = Intent(this, AddFriendActivity::class.java)
            startActivity(addFriendIntent)
        }
    }

}