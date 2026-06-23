package org.nahoft.nahoft.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.R
import org.nahoft.nahoft.databinding.MessageItemRowBinding
import org.nahoft.nahoft.models.Message

class MessagesRecyclerAdapter(private val messages: ArrayList<Message>) : RecyclerView.Adapter<MessagesRecyclerAdapter.MessageViewHolder>()
{
    var onItemLongClick: ((Message) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder
    {
        val binding = MessageItemRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int)
    {
        val messageItem = messages[position]
        holder.bindMessage(messageItem)
    }

    override fun getItemCount(): Int = messages.size

    fun cleanup()
    {
        messages.clear()
    }

    // ViewHolder
    inner class MessageViewHolder(private val binding: MessageItemRowBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener
    {
        private var message: Message? = null

        init {
            binding.root.setOnClickListener(this)

            binding.root.setOnLongClickListener {
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
            binding.messageTextView.text = loadMessageContent() // newMessage.sender?.name
            binding.dateTextView.text = newMessage.getDateStringForDetail()
            if (newMessage.fromMe) {
                binding.root.layoutDirection = View.LAYOUT_DIRECTION_RTL
                binding.statusImageView.text = "Me"
            } else {
                binding.messageTextView.setBackgroundResource(R.drawable.transparent_overlay_message_white)
                binding.statusImageView.text = newMessage.sender?.name?.substring(0, 1)
            }
        }

        private fun loadMessageContent(): String?
        {
            val msg = message ?: return null

            // Unencrypted messages are stored as raw UTF-8 — no key needed
            if (!msg.isEncrypted)
            {
                return String(msg.cipherText, Charsets.UTF_8)
            }

            // Encrypted path
            val senderKeyBytes = msg.sender?.publicKeyEncoded ?: return "Unable to decrypt message"

            return try
            {
                Encryption().decrypt(PublicKey(senderKeyBytes), msg.cipherText)
            }
            catch (_: SecurityException)
            {
                "Unable to decrypt message"
            }
        }
    }
}