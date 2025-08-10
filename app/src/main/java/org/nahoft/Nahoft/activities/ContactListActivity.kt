//package org.nahoft.nahoft.activities
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.content.IntentFilter
//import android.content.pm.PackageManager
//import android.net.Uri
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.os.Parcelable
//import android.provider.ContactsContract
//import android.view.WindowManager
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.view.isVisible
//import androidx.core.widget.doOnTextChanged
//import androidx.recyclerview.widget.LinearLayoutManager
//import org.nahoft.codex.LOGOUT_TIMER_VAL
//import org.nahoft.codex.LogoutTimerBroadcastReceiver
//import org.nahoft.nahoft.*
//import org.nahoft.util.RequestCodes
//import org.nahoft.util.showAlert
//
//class ContactListActivity : AppCompatActivity() {
//    private val receiver by lazy {
//        LogoutTimerBroadcastReceiver {
//            adapter.cleanup()
//        }
//    }
//
//    private lateinit var linearLayoutManager: LinearLayoutManager
//    private lateinit var adapter: ContactsRecyclerAdapter
//    private lateinit var filteredContactList: ArrayList<Contact>
//    private var contactList: ArrayList<Contact>? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_contact_list)
//
//        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
//
//        registerReceiver(receiver, IntentFilter().apply {
//            addAction(LOGOUT_TIMER_VAL)
//        })
//
//        contact_guide_button.setOnClickListener {
//            val slideActivity = Intent(this, SlideActivity::class.java)
//            slideActivity.putExtra(Intent.EXTRA_TEXT, slideNameContactList)
//            startActivity(slideActivity)
//        }
//
//        if (Persist.accessIsAllowed()) {
//            val permissionCheck = ContextCompat.checkSelfPermission(
//                this, Manifest.permission.READ_CONTACTS
//            )
//            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
//                permissionDeniedTextView.isVisible = true
//                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), RequestCodes.requestPermissionCode)
//                showAlert(getString(R.string.read_contacts_permission_needed))
//            } else {
//                getContacts()
//                permissionDeniedTextView.isVisible = false
//            }
//            search_contacts.doOnTextChanged { text, _, _, _ ->
//                filteredContactList.clear()
//                filteredContactList.addAll(contactList?.filter { f ->
//                    f.name.contains(
//                        text.toString(),
//                        true
//                    )
//                } as ArrayList<Contact>)
//                adapter.notifyDataSetChanged()
//            }
//        } else {
//            sendToLogin()
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        if (!Persist.accessIsAllowed())
//        {
//            finish()
//            sendToLogin()
//        }
//    }
//
//    override fun onDestroy() {
//        try
//        {
//            unregisterReceiver(receiver)
//        }
//        catch (e: Exception)
//        {
//            //Nothing to unregister
//        }
//
//        super.onDestroy()
//    }
//
//    override fun onRestart() {
//        super.onRestart()
//        adapter.notifyDataSetChanged()
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == RequestCodes.requestPermissionCode && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            getContacts()
//            permissionDeniedTextView.isVisible = false
//        }
//    }
//
//    @SuppressLint("Range")
//    fun getContacts() {
//        val names = ArrayList<Contact>()
//        val cr = contentResolver
//        val cur = cr.query(
//            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
//            null, null, "display_name ASC")
//        if (cur!!.count > 0) {
//            while (cur.moveToNext()) {
//                //val id = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NAME_RAW_CONTACT_ID))
//                val name = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
//                val number = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
//                names.add(Contact(name, number))
//            }
//        }
//        contactList = names
//        showContactsList()
//    }
//
//    private fun showContactsList() {
//        linearLayoutManager = LinearLayoutManager(this)
//        filteredContactList = contactList?.clone() as ArrayList<Contact>
//        adapter = ContactsRecyclerAdapter(filteredContactList)
//        contactsRecyclerView.layoutManager = linearLayoutManager
//        contactsRecyclerView.adapter = adapter
//
//        adapter.onItemClick = { contact ->
//            saveAndShowInfoActivity(contact)
//        }
//    }
//
//    private fun saveAndShowInfoActivity(contact: Contact) {
//        val newFriend = saveFriend(contact.name, contact.number.replace(" ", ""))
//        if (newFriend != null) {
//            val friendInfoActivityIntent = Intent(this, FriendInfoActivity::class.java)
//            friendInfoActivityIntent.putExtra(RequestCodes.friendExtraTaskDescription, newFriend)
//            startActivity(friendInfoActivityIntent)
//            finish()
//        }
//    }
//
//    private fun saveFriend(friendName: String, phoneNumber: String) : Friend? {
//        val newFriend = Friend(friendName, phoneNumber, FriendStatus.Default, null)
//
//        // Only add the friend if one with the same name doesn't already exist.
//        if (Persist.friendList.any {friend -> friend.name == friendName })
//        {
//            showAlert(getString(R.string.alert_text_friend_already_exists))
//            return null
//        }
//        else if (Persist.friendList.any { friend -> friend.phone == phoneNumber}) {
//            showAlert(getString(R.string.alert_text_friend_phone_already_exists))
//            return null
//        }
//        else
//        {
//            Persist.friendList.add(newFriend)
//            Persist.saveFriendsToFile(this)
//            return newFriend
//        }
//    }
//
//    private fun sendToLogin() {
//        this.showAlert(getString(R.string.alert_text_passcode_required_to_proceed))
//        val loginIntent = Intent(applicationContext, LogInActivity::class.java)
//        startActivity(loginIntent)
//        finish()
//    }
//}