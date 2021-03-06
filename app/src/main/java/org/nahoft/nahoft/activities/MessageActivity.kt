package org.nahoft.nahoft.activities

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_message.*
import kotlinx.android.synthetic.main.message_item_row.*
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Encryption
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.Message
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.Persist.Companion.deleteMessage
import org.nahoft.nahoft.R
import org.nahoft.util.showAlert

class MessageActivity : AppCompatActivity()
{

    class Arguments(val message: Message) {
        companion object {
            const val messageKey = "Message"

            fun createFromIntent(intent: Intent): Arguments {
                return Arguments(message = intent.getSerializableExtra(messageKey) as Message)
            }
        }

        fun startActivity(context: Context)
        {
            val intent = Intent(context, MessageActivity::class.java)
            intent.putExtra(messageKey, message)
            context.startActivity(intent)
        }
    }

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanUp()
        }
    }

    lateinit var message: Message

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        if (!Persist.accessIsAllowed()) { sendToLogin() }

        val arguments = Arguments.createFromIntent(intent)
        message = arguments.message

        loadMessageContent()

        deleteButton.setOnClickListener{
            deleteMessage(this, message)
            finish()
        }

    }

    override fun onDestroy() {
        try
        {
            unregisterReceiver(receiver)
        }
        catch (e: Exception)
        {
            //Nothing to unregister
        }

        super.onDestroy()
    }

    private fun loadMessageContent()
    {
        message_detail_sender_text_view.text = message.sender?.name
        message_detail_date_text_view.text = message.getDateStringForDetail()

        val senderKeyBytes = message.sender?.publicKeyEncoded

        if (senderKeyBytes != null)
        {
            val senderKey = PublicKey(senderKeyBytes)

            try
            {
                val plaintext = Encryption().decrypt(senderKey, message.cipherText)
                message_detail_body_text_view.text = plaintext
            }
            catch (exception: SecurityException)
            {
                applicationContext.showAlert(getString(R.string.alert_text_unable_to_decrypt_message))
                deleteMessage(this, message)
                finish()
            }

        }
        else
        {
            applicationContext.showAlert(getString(R.string.alert_text_unable_to_decrypt_message))
            return
        }
    }

    private fun cleanUp ()
    {
        message = Message(ByteArray(2), Friend(""))
        friend_info_name_text_view.text = ""
        message_detail_body_text_view.text = ""
    }

    private fun sendToLogin()
    {
        message_detail_body_text_view.text = ""
        // If the status is not either NotRequired, or Logged in, request login
        this.showAlert(getString(R.string.alert_text_passcode_required_to_proceed))

        // Send user to the Login Activity
        val loginIntent = Intent(applicationContext, LogInActivity::class.java)

        startActivity(loginIntent)
    }
}