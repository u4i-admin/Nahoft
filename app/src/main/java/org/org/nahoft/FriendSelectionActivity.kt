package org.org.nahoft

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_friend_selection.*


class FriendSelectionActivity : AppCompatActivity() {

    val PERMISSIONS_REQUEST_READ_CONTACTS = 100

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: FriendSelectionRecyclerAdapter

    private val lastVisibleItemPosition: Int
        get() = linearLayoutManager.findLastVisibleItemPosition()

    private var friendList: ArrayList<Friend> = ArrayList()

    companion object {
        const val FRIEND_EXTRA_TASK_DESCRIPTION = "friend"

        fun newIntent(context: Context) = Intent(context, FriendSelectionActivity::class.java)
        //fun getSelectedFriend(data: Intent?): String? = data?.getStringExtra(FRIEND_EXTRA_TASK_DESCRIPTION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("**FriendSelectionActivity")
        setContentView(R.layout.activity_friend_selection)

        linearLayoutManager = LinearLayoutManager(this)
        adapter = FriendSelectionRecyclerAdapter(friendList) {
            // This is the onClick listener for our Recycler
            val result = Intent()
            result.putExtra(FRIEND_EXTRA_TASK_DESCRIPTION, it.name)
            setResult(Activity.RESULT_OK, result)

            finish()
        }

        friendsSelectionRecyclerView.layoutManager = linearLayoutManager
        friendsSelectionRecyclerView.adapter = adapter

    }

    override fun onStart() {
        super.onStart()
        //TO DO GET CONTACTS ADD THEM TO FRIEND LIST
        loadContacts()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                println("No Permission For Contacts")
            }
        }
    }

    fun loadContacts() {
        var builder = StringBuilder()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
        } else {
            builder = getContacts()
            val contactsText = builder.toString()
        }
    }

    fun getContacts(): StringBuilder {
        val resolver: ContentResolver = contentResolver;
        val builder = StringBuilder()
        val cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null)

        if (cursor != null) {
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                    val name =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    val newFriend = Friend(id, name)
                    friendList.add(newFriend)
                    println("found contact")
                    println(id)
                    println(name)
                    builder.append("Contact ID: ").append(id).append("Name: ").append(name)
                }
            }
        } else {
            println("cursor is null")
        }

        return builder
    }


}

