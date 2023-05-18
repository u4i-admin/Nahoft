package org.nahoft.nahoft

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.message_item_row.view.*
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Encryption
import org.nahoft.util.inflate

class MessagesRecyclerAdapter(private val messages: ArrayList<Message>) : RecyclerView.Adapter<MessagesRecyclerAdapter.MessageViewHolder>()
{
    var onItemLongClick: ((Message) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder
    {
        val inflatedView = parent.inflate(R.layout.message_item_row, false)
        return MessageViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int)
    {
        val messageItem = messages[position]
        holder.bindMessage(messageItem)
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(messageItem)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int = messages.size

    fun cleanup()
    {
        messages.clear()
    }

    // ViewHolder
    inner class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener
    {

        private var message: Message? = null
        private var view: View = v

        init {
            v.setOnClickListener(this)
            v.setOnLongClickListener {
                message?.let { it1 ->
                    onItemLongClick?.invoke(it1)
                }
                return@setOnLongClickListener true
            }
        }

        override fun onClick(v: View?)
        {
            // Go to message view

//            this.message?.let {
//                val messageArguments = MessageActivity.Arguments(message = it)
//                messageArguments.startActivity(this.view.context)
//            }
        }

        fun bindMessage(newMessage: Message)
        {
            this.message = newMessage
            this.view.message_text_view.text = loadMessageContent() // newMessage.sender?.name
            this.view.date_text_view.text = newMessage.getDateStringForDetail()
            if (newMessage.fromMe) {
                this.view.layoutDirection = View.LAYOUT_DIRECTION_RTL
                this.view.status_image_view.text = "Me"
            } else {
                this.view.message_text_view.setBackgroundResource(R.drawable.transparent_overlay_message_white)
                this.view.status_image_view.text = newMessage.sender?.name?.substring(0, 1)
            }
        }

        private fun loadMessageContent(): String?
        {
            val senderKeyBytes = message?.sender?.publicKeyEncoded

            if (senderKeyBytes != null)
            {
                val senderKey = PublicKey(senderKeyBytes)

                return try {
                    message?.let { Encryption().decrypt(senderKey, it.cipherText) }
                } catch (exception: SecurityException) {
                    // applicationContext.showAlert(getString(R.string.alert_text_unable_to_decrypt_message))
                    // message?.let { Persist.deleteMessage(getContext(), it) }
                    "Unable to decrypt message"
                }
            }
            else
            {
                // applicationContext.showAlert(getString(R.string.alert_text_unable_to_decrypt_message))
                return "Unable to decrypt message"
            }
        }
    }
}