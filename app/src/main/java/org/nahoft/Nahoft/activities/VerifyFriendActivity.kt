package org.nahoft.nahoft.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_verify_friend.*
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.FriendStatus
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.util.RequestCodes

class VerifyFriendActivity : AppCompatActivity()
{
    private lateinit var pendingFriend: Friend

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_friend)

        // Get our pending friend
        val maybeFriend = intent.getSerializableExtra(RequestCodes.friendExtraTaskDescription) as? Friend

        if (maybeFriend?.publicKeyEncoded != null) {
            pendingFriend = maybeFriend
        } else {
            finish()
        }

        // Accept Button
        accept_verification_button.setOnClickListener {
            verifySecurityNumber()
        }

        // Reject Button
        reject_verification_button.setOnClickListener {
            rejectSecurityNumber()
        }

        // Reset Button
        reset_friend_button.setOnClickListener {
            resetTapped()
        }

        setupTextViews()
        setupButtons()
    }

    private fun setupTextViews()
    {
        friend_security_number_label.text = getString(R.string.label_verify_friend_number, pendingFriend.name)

        // Display friend public key as encoded text.
        val codex = Codex()
        val encodedKey = codex.encodeKey(PublicKey(pendingFriend.publicKeyEncoded).toBytes())
        friend_security_number_text.text = encodedKey
        // Display friend public key as security number (Uppercase and Grouped by 4s)
        //val friendKeyString = PublicKey(pendingFriend.publicKeyEncoded).toString().toUpperCase()
        //friend_security_number_text.text = friendKeyString.chunked(4).joinToString(" ")

        // Display user public key as encoded text
        val userEncodedKey = codex.encodeKey(Encryption().ensureKeysExist().publicKey.toBytes())
        user_security_number_text.text = userEncodedKey

        // Display user public key as security number (Uppercase and Grouped by 4s)
        //val userPublicKeyString = Encryption().ensureKeysExist().publicKey.toString().toUpperCase()
        //user_security_number_text.text = userPublicKeyString.chunked(4).joinToString(" ")
    }

    private fun setupButtons()
    {
        if (pendingFriend.status == FriendStatus.Approved)
        {
            accept_verification_button.visibility = View.VISIBLE
            reject_verification_button.visibility = View.VISIBLE
            reset_friend_button.visibility = View.GONE
        }
        else
        {
            accept_verification_button.visibility = View.GONE
            reject_verification_button.visibility = View.GONE
            reset_friend_button.visibility = View.VISIBLE
        }
    }

    private fun verifySecurityNumber()
    {
        Persist.updateFriend(this, pendingFriend, FriendStatus.Verified, pendingFriend.publicKeyEncoded)
        finish()
    }

    private fun rejectSecurityNumber()
    {
        pendingFriend.publicKeyEncoded = null
        Persist.updateFriend(this, pendingFriend, FriendStatus.Default)
        finish()
    }

    private fun resetTapped()
    {
        val alertBuilder = AlertDialog.Builder(this)
            .setTitle(R.string.alert_title_confirm_friend_reset)
            .setMessage(R.string.alert_text_confirm_friend_reset)
            .setPositiveButton(R.string.button_label_reset){ _, _ ->
                resetFriend()
            }
            .setNegativeButton(R.string.button_label_cancel) { _, _ ->
                //
            }

        val resetAlert = alertBuilder.create()
        resetAlert.show()
    }

    private fun resetFriend()
    {
        Persist.resetFriend(this, pendingFriend)
        finish()
    }

    private fun cleanup(){
        friend_security_number_label.text = ""
        friend_security_number_text.text = ""
        pendingFriend = Friend("", FriendStatus.Default, null)
    }
}