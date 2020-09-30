package org.nahoft.nahoft

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.message_item_row.view.*
import org.nahoft.inflate
import org.nahoft.nahoft.activities.MessageActivity
import java.time.format.DateTimeFormatter

class MessagesRecyclerAdapter(private val messages: ArrayList<Message>) : RecyclerView.Adapter<MessagesRecyclerAdapter.MessageViewHolder>()  {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflatedView = parent.inflate(R.layout.message_item_row, false)
        return MessageViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageItem = messages[position]
        holder.bindMessage(messageItem)
    }

    override fun getItemCount(): Int = messages.size

    // ViewHolder
    class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {

        private var message: Message? = null
        private var view: View = v

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            // Go to message view

            this.message?.let {
                val messageArguments = MessageActivity.Arguments(message = it)
                messageArguments.startActivity(this.view.context)
            }

        }

        fun bindMessage(newMessage: Message) {
            this.message = newMessage
            this.view.sender_name_text_view.text = newMessage.sender?.name

            val formattedDate = newMessage.timestamp.format(DateTimeFormatter.ofPattern("M/d/y H:m"))
            this.view.date_text_view.text = formattedDate
        }

    }

}