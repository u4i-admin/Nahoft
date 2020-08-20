package org.operatorfoundation.nahoft

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Adapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_friends_selection.*
import org.operatorfoundation.RecyclerAdapter


class FriendsSelectionActivity : AppCompatActivity() {

    val IMAGE_PICK_CODE = 1046
    val PERMISSIONS_REQUEST_READ_CONTACTS = 100

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter

    private val lastVisibleItemPosition: Int
    get() = linearLayoutManager.findLastVisibleItemPosition()

    private var friendList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("**Friends Selection Activity")
        setContentView(R.layout.activity_friends_selection)

        //Can Add Back In Later
        //friendList.add("Friend 1")
        //friendList.add("Friend 2")
        linearLayoutManager = LinearLayoutManager(this)
        adapter = RecyclerAdapter(friendList)
        friendsSelectionRecyclerView.layoutManager = linearLayoutManager
        friendsSelectionRecyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        //TO DO GET CONTACTS ADD THEM TO FRIEND LIST
        //loadContacts()
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
}

