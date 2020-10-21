package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_verify_friend.*
import kotlinx.android.synthetic.main.friend_recyclerview_item_row.*
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.FriendStatus
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.publicKeyPreferencesKey
import org.nahoft.nahoft.R
import org.nahoft.util.RequestCodes

class VerifyFriendActivity() : AppCompatActivity() {

    lateinit var pendingFriend: Friend

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_friend)

        // Get our pending friend
        val maybeFriend = intent.getSerializableExtra(RequestCodes.friendExtraTaskDescription) as? Friend

        if (maybeFriend != null  && maybeFriend.publicKeyEncoded != null) {
            pendingFriend = maybeFriend
        } else {
            finish()
        }

        setupTextViews()

        // Accept Button
        accept_verification_button.setOnClickListener {
            verifySecurityNumber()
        }

        // Reject Button
        reject_verification_button.setOnClickListener {
            rejectSecurityNumber()
        }
    }

    fun setupTextViews() {
        // Display friend public key as security number (Uppercase and Grouped by 4s)
        friend_security_number_label.text = getString(R.string.label_verify_friend_number, pendingFriend.name)


        val friendKeyString = PublicKey(pendingFriend.publicKeyEncoded).toString().toUpperCase()
        friend_security_number_text.text = friendKeyString.chunked(4).joinToString(" ")

        // Display user public key as security number (Uppercase and Grouped by 4s)
        val userPublicKeyString = Encryption(this).ensureKeysExist().publicKey.toString().toUpperCase()
        user_security_number_text.text = userPublicKeyString.chunked(4).joinToString(" ")
    }

    fun verifySecurityNumber() {
        Persist.updateFriend(this, pendingFriend, FriendStatus.Verified, pendingFriend.publicKeyEncoded)
        finish()
    }

    fun rejectSecurityNumber() {
        Persist.updateFriend(this, pendingFriend, FriendStatus.Default)
        finish()
    }
}