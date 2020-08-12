package org.operatorfoundation.nahoft

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_new_message.*

class NewMessageActivity : AppCompatActivity() {
//EditText message_text_view
//Button send_as_text_button
    val IMAGE_PICK_CODE = 1046
    val PERMISSIONS_REQUEST_READ_CONTACTS = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)


        friend_button.setOnClickListener {
            println("friend button clicked")
            loadContacts()
        }

        send_as_text_button.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, editMessageText.text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        send_as_image_button.setOnClickListener {
            pickImageFromGallery()
        }
    }

    fun pickImageFromGallery() {
        val pickImageIntent = Intent().apply {
            action = Intent.ACTION_PICK
            type = "image/*"
        }
        startActivityForResult(pickImageIntent, IMAGE_PICK_CODE)
    }
    fun loadContacts(){
        var builder = StringBuilder()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), PERMISSIONS_REQUEST_READ_CONTACTS)
        } else {
            builder = getContacts()
            val contactsText = builder.toString()
        }
    }

    fun getContacts(): StringBuilder {
        val resolver: ContentResolver = contentResolver;
        val builder = StringBuilder()
        val cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,null, null, null)

        if (cursor != null) {
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                    val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            val shareIntent: Intent = Intent().apply{
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, data?.data)
                type = "image/jpeg"
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                println("No Permission For Contacts")
            }
        }
    }
}
