package org.nahoft.nahoft.activities

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_friends.*
import org.nahoft.codex.PersistenceEncryption
import org.nahoft.nahoft.*
import org.simpleframework.xml.core.Persister
import java.io.*
import java.lang.Exception

class FriendsActivity : AppCompatActivity() {

    private val permissionsRequestReadContacts = 100

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: FriendsRecyclerAdapter

//    private val lastVisibleItemPosition: Int
//        get() = linearLayoutManager.findLastVisibleItemPosition()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        linearLayoutManager = LinearLayoutManager(this)
        adapter = FriendsRecyclerAdapter(Persist.friendList)
        friendsRecyclerView.layoutManager = linearLayoutManager
        friendsRecyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        // Load friends from file and add any new contacts
        setupFriends()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        saveFriendsToFile()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionsRequestReadContacts) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                loadContacts()

            } else {
                println("No Permission For Contacts")
            }
        }
    }

    private fun setupFriends() {
        // Check contacts for new friends.
        loadContacts()
    }

    private fun loadContacts() {
        // Check if we have permission to see the user's contacts
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // We don't have permission, ask for it
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                permissionsRequestReadContacts
            )
        } else {
            // Permission granted!
            // Let's get the contacts and convert them to friends!
            getContacts()
        }
    }

    private fun getContacts() {
        val resolver: ContentResolver = contentResolver
        val cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null)

        if (cursor != null) {
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                    val name =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    val newFriend = Friend(id, name)

                    // TODO: Find a better way to do this,
                    //  after the first time this runs most contacts will already be in the friends list.

                    // Only add this friend if the list does not contain a frined with that ID already
                    if (!Persist.friendList.any { it.id == newFriend.id }) {
                        Persist.friendList.add(newFriend)
                    } else {
                        print("******We didn't add the contact $name, they are already in our friend list.")
                    }
                }

                cursor.close()
                saveFriendsToFile()
                adapter.notifyDataSetChanged()
            }
        } else {
            println("cursor is null")
        }
    }

    private fun saveFriendsToFile() {
        val serializer = Persister()
        val outputStream = ByteArrayOutputStream()

        val friendsObject = Friends(Persist.friendList)
        try { serializer.write(friendsObject, outputStream) } catch (e: Exception) {
            print("Failed to serialize our friends list: $e")
        }

        PersistenceEncryption().writeEncryptedFile(Persist.friendsFile, outputStream.toByteArray(), applicationContext)
    }
}