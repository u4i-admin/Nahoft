package org.nahoft.nahoft.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_message.*
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.Message
import org.nahoft.nahoft.R

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
    }

    private fun loadMessageContent() {

        message_sender_text_view.text = getString(R.string.sender_label, message.sender?.name)

        val senderKeyBytes = message.sender?.publicKeyEncoded

        if (senderKeyBytes != null) {
            val senderKey = PublicKey(senderKeyBytes)
            val plaintext = Encryption(this).decrypt(senderKey, message.cipherText)

            if (plaintext != null) {
                message_body_text_view.text = plaintext
            }

        } else {
            print("Failed to get sender public key for a message")
            return
        }

    }
}