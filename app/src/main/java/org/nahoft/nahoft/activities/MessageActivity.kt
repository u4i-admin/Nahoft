package org.nahoft.nahoft.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.android.synthetic.main.activity_message.*
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.Message
import org.nahoft.nahoft.Persist.Companion.deleteMessage
import org.nahoft.nahoft.R
import org.nahoft.showAlert

class MessageActivity : AppCompatActivity() {

    class Arguments(val message: Message) {
        companion object {
            const val messageKey = "Message"

            fun createFromIntent(intent: Intent): Arguments {
                return Arguments(message = intent.getSerializableExtra(messageKey) as Message)
            }
        }

        fun startActivity(context: Context) {
            val intent = Intent(context, MessageActivity::class.java)
            intent.putExtra(messageKey, message)
            context.startActivity(intent)
        }
    }

    lateinit var message: Message

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        val arguments = Arguments.createFromIntent(intent)
        message = arguments.message


        loadMessageContent()

        deleteButton.setOnClickListener{
            deleteMessage(this, message)
            finish()
        }

    }

    private fun loadMessageContent()
    {
        message_sender_text_view.text = getString(R.string.sender_label, message.sender?.name)

        val senderKeyBytes = message.sender?.publicKeyEncoded

        if (senderKeyBytes != null)
        {
            val senderKey = PublicKey(senderKeyBytes)

            try {
                val plaintext = Encryption(this).decrypt(senderKey, message.cipherText)
                message_body_text_view.text = plaintext
            } catch (exception: SecurityException) {
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

    override fun onDestroy() {
        super.onDestroy()
        cleanUp()
    }

    private fun cleanUp () {
        message = Message("", ByteArray(2))
    }
}