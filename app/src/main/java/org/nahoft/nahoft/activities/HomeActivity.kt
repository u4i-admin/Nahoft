package org.nahoft.nahoft.activities

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_home.*
import org.libsodium.jni.SodiumConstants
import org.libsodium.jni.crypto.Box
import org.libsodium.jni.crypto.Random
import org.libsodium.jni.keys.KeyPair
import org.libsodium.jni.keys.PrivateKey
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.codex.KeyOrMessage
import org.nahoft.nahoft.*
import org.nahoft.nahoft.Persist.Companion.friendsFilename
import org.nahoft.nahoft.Persist.Companion.messagesFilename
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.showAlert
import org.nahoft.stencil.Stencil
import org.nahoft.util.RequestCodes
import java.io.File
import java.util.*

class HomeActivity : AppCompatActivity() {
    private var decodePayload: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Testing only
        tests()

        Persist.app = Nahoft()

        if (status == LoginStatus.NotRequired) {
            logout_button.visibility = View.INVISIBLE
        } else {
            logout_button.visibility = View.VISIBLE
        }

        messages_button.setOnClickListener {
            val messagesIntent = Intent(this, MessagesActivity::class.java)
            startActivity(messagesIntent)
        }

        user_guide_button.setOnClickListener {
            val userGuideIntent = Intent(this, UserGuideActivity::class.java)
            startActivity(userGuideIntent)
        }

        friends_button.setOnClickListener {
            val friendsIntent = Intent(this, FriendsActivity::class.java)
            startActivity(friendsIntent)
        }

        settings_button.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        // Load friends from file and add any new contacts
        getStatus()
        setupFriends()
        loadSavedMessages()

        // Receive shared messages
        when (intent?.action) {
            Intent.ACTION_SEND -> {

                // Received shared data check LoginStatus
                if (Persist.status == LoginStatus.NotRequired) {
                    // We may not have intialized shared preferences yet, let's do it now
                    Persist.loadEncryptedSharedPreferences(this.applicationContext)
                } else if (Persist.status != LoginStatus.LoggedIn) {
                    //FIXME: If the status is not either NotRequired, or Logged in, request login
                    this.showAlert("Passcode required to proceed")
                }

                if ("text/plain" == intent.type) {
                    handleSharedText(intent)
                } else if (intent.type?.startsWith("image/") == true) {
                    handleSharedImage(intent)
                }
            }
        }
    }

    fun logoutClicked(view: android.view.View) {
        status = LoginStatus.LoggedOut
        Persist.saveLoginStatus()

        val returnToLoginIntent = Intent(this, EnterPasscodeActivity::class.java)
        startActivity(returnToLoginIntent)
    }

    private fun getStatus() {

        val statusString = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefLoginStatusKey, null)

        if (statusString != null) {

            try {
                Persist.status = LoginStatus.valueOf(statusString)
            } catch (error: Exception) {
                print("Received invalid status from EncryptedSharedPreferences. User is logged out.")
                Persist.status = LoginStatus.LoggedOut
            }
        } else {
            Persist.status = LoginStatus.NotRequired
        }
    }

    private fun loadSavedFriends() {
        // Load our existing friends list from our encrypted file
        if (Persist.friendsFile.exists()) {
            val friendsToAdd = FriendViewModel.getFriends(Persist.friendsFile, applicationContext)

            for (newFriend in friendsToAdd) {
                // Only add this friend if the list does not contain a friend with that ID already
                if (!Persist.friendList.any { it.id == newFriend.id }) {
                    Persist.friendList.add(newFriend)
                }
            }
        }
    }

    private fun loadSavedMessages() {
        Persist.messagesFile = File(filesDir.absolutePath + File.separator + messagesFilename )

        // Load messages from file
        if (Persist.messagesFile.exists()) {
            val messagesToAdd = MessageViewModel().getMessages(Persist.messagesFile, applicationContext)
            messagesToAdd?.let {
                Persist.messageList.addAll(it)
            }

        }
    }

    private fun handleSharedText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            // Update UI to reflect text being shared
            val decodeResult = Codex().decode(it)

            if (decodeResult != null) {
                this.decodePayload = decodeResult.payload

                if (decodeResult.type == KeyOrMessage.EncryptedMessage) {
                    // We received a message, have the user select who it is from
                    val selectSenderIntent = Intent(this, SelectMessageSenderActivity::class.java)
                    startActivityForResult(selectSenderIntent, RequestCodes.selectMessageSenderCode)
                } else {
                    // We received a key, have the user select who it is from
                    val selectSenderIntent = Intent(this, SelectKeySenderActivity::class.java)
                    startActivityForResult(selectSenderIntent, RequestCodes.selectKeySenderCode)
                }
            } else {
                this.showAlert("Something went wrong. We were unable to decode the message.")
            }
        }
    }

    private fun handleSharedImage(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            // Update UI to reflect image being shared

            // DEV ONLY DO NOT TRANSLATE, DELETE LATER
            this.showAlert("Received shared image. $it")
            // DEV ONLY DO NOT TRANSLATE, DELETE LATER

            // Decode the message and save it locally for use after sender is selected
            this.decodePayload = Stencil().decode(this, it)

            // We received a message, have the user select who it is from
            val selectSenderIntent = Intent(this, SelectMessageSenderActivity::class.java)
            startActivityForResult(selectSenderIntent, RequestCodes.selectMessageSenderCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RequestCodes.selectMessageSenderCode) {
                val sender = data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription)?.let { it as Friend }

                if (sender != null && decodePayload != null) {
                    // Create Message Instance
                    val date = Calendar.getInstance().time
                    val cipherText = decodePayload

                    val newMessage = Message(date, cipherText!!, sender)
                    Persist.messageList.add(newMessage)
                    Persist.saveMessagesToFile(this)

                    // Go to message view
                    val messageArguments = MessageActivity.Arguments(message = newMessage)
                    messageArguments.startActivity(this)
                }

            } else if (requestCode == RequestCodes.selectKeySenderCode) {
                val sender = data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription)?.let { it as Friend }

                // Update this friend with a new key and a new status
                if (sender != null && decodePayload != null) {

                    when (sender.status) {
                        FriendStatus.Default -> {
                            Persist.updateFriend(context = this, friendToUpdate = sender, newStatus = FriendStatus.Requested, encodedPublicKey = decodePayload!!)
                            this.showAlert(getString(R.string.alert_text_received_invitation, sender.name))
                        }

                        FriendStatus.Invited -> {
                            Persist.updateFriend(context = this, friendToUpdate = sender, newStatus = FriendStatus.Approved, encodedPublicKey = decodePayload!!)
                            // TODO Translate
                            this.showAlert("${sender.name} accepted your invitation. You can now communicate securely.")
                        }

                        else ->
                            this.showAlert("Something went wrong, we were unable to update your friend status.")
                    }
                } else {
                    this.showAlert("Something went wrong, we were unable to update your friend status.")
                }
            }
        }
    }

    // TODO: Move this
    private val permissionsRequestReadContacts = 100

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
        Persist.friendsFile = File(filesDir.absolutePath + File.separator + friendsFilename )
        loadSavedFriends()

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
            ActivityCompat.requestPermissions(this,
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

                    // Only add this friend if the list does not contain a friend with that ID already
                    if (!Persist.friendList.any { it.id == newFriend.id }) {
                        Persist.friendList.add(newFriend)
                    }
                }

                cursor.close()
                Persist.saveFriendsToFile(this)
            }
        } else {
            println("cursor is null")
        }
    }

    fun tests() {
        test_encrypt_decrypt()
        test_hardcoded_encrypt_decrypt()

        val keyPair = Encryption(this).ensureKeysExist()
        val encodedPrivateKey = keyPair.privateKey.toBytes()
        val encodedPublicKey = keyPair.publicKey.toBytes()
        val privateKey = PrivateKey(encodedPrivateKey)
        val publicKey = PublicKey(encodedPublicKey)

        if (keyPair.privateKey == privateKey) {
            print("private keys match")
        }

        if (keyPair.publicKey == publicKey) {
            print("public keys match")
        }

        print("Test complete.")

    }

    fun test_encrypt_decrypt() {
        val plaintext = "a"

        val seed1 = Random().randomBytes(SodiumConstants.SECRETKEY_BYTES)
        val keyPair1 = KeyPair(seed1)

        val seed2 = Random().randomBytes(SodiumConstants.SECRETKEY_BYTES)
        val keyPair2 = KeyPair(seed2)

        val box = Box(keyPair2.publicKey, keyPair1.privateKey)
        val nonce = Random().randomBytes(SodiumConstants.NONCE_BYTES)
        val ciphertext = box.encrypt(nonce, plaintext.toByteArray())
        val encrypted = nonce + ciphertext

        val box2 = Box(keyPair1.publicKey, keyPair2.privateKey)
        val nonce2 = encrypted.slice(0..SodiumConstants.NONCE_BYTES - 1).toByteArray()
        val ciphertext2 = encrypted.slice(SodiumConstants.NONCE_BYTES..encrypted.lastIndex).toByteArray()

        try
        {
            val plaintext2 = String(box2.decrypt(nonce2, ciphertext2))
            println(plaintext2)
        }
        catch(e: Exception)
        {
            println(e)
        }
    }

    fun test_hardcoded_encrypt_decrypt() {
        val plaintext = "a"

        val friendPublicKey = PublicKey(byteArrayOf(95, 60, 7, 93, -108, 19, -34, 112, 16, -114, -4, 26, -122, -85, -67, 67, -55, 98, -24, 108, -38, 59, 72, 127, 38, -102, 107, 22, 122, -100, -90, -68))
        val friendPrivateKey = PrivateKey("")
        val nonce = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)
        val myPublicKey = PublicKey(byteArrayOf(-83, -98, -51, 15, 44, -91, 65, 6, -76, 32, -78, -101, 81, -16, -51, -24, 40, -52, -126, 67, 101, -47, 12, -41, -107, -78, -121, -66, 63, 57, -90, -116))
        val myPrivateKey = PrivateKey("3a2ac7a3ad2b0e2acb608a8905b300f31f0d900c22a3fa61df7be23136437f79")

        val box = Box(friendPublicKey, myPrivateKey)
        val ciphertext = box.encrypt(nonce, plaintext.toByteArray())

        val encrypted = nonce + ciphertext

        val box2 = Box(myPublicKey, friendPrivateKey)
        val nonce2 = encrypted.slice(0..SodiumConstants.NONCE_BYTES - 1).toByteArray()
        val ciphertext2 = encrypted.slice(SodiumConstants.NONCE_BYTES..encrypted.lastIndex).toByteArray()

        try
        {
            val plaintext2 = String(box2.decrypt(nonce2, ciphertext2))
            println(plaintext2)
        }
        catch(e: Exception)
        {
            println(e)
        }
    }
}
