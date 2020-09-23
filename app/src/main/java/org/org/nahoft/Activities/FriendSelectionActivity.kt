package org.org.nahoft

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_friend_selection.*


class FriendSelectionActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: FriendSelectionRecyclerAdapter

    private val lastVisibleItemPosition: Int
        get() = linearLayoutManager.findLastVisibleItemPosition()

    companion object {
        const val FRIEND_EXTRA_TASK_DESCRIPTION = "friend"

        fun newIntent(context: Context) = Intent(context, FriendSelectionActivity::class.java)
        //fun getSelectedFriend(data: Intent?): String? = data?.getStringExtra(FRIEND_EXTRA_TASK_DESCRIPTION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_selection)

        // Only show friends that have been verified
        var verifiedFriends = ArrayList<Friend>()

        for (friend in Persist.friendList) {

            if (friend.status == FriendStatus.Verified) {
                verifiedFriends.add(friend)
            }
        }

        linearLayoutManager = LinearLayoutManager(this)
        adapter = FriendSelectionRecyclerAdapter(verifiedFriends) {

            // This is the onClick listener for our Recycler
            val result = Intent()
            result.putExtra(FRIEND_EXTRA_TASK_DESCRIPTION, it)
            setResult(Activity.RESULT_OK, result)

            finish()
        }

        friendsSelectionRecyclerView.layoutManager = linearLayoutManager
        friendsSelectionRecyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        print(">>>>>>>>>>>>>>>>>>>>>>Friend selection activity. Current friend list size:\n>>>>>>>>>>>>>>>>>>>>>>>")
        print(Persist.friendList.size)
    }

}
