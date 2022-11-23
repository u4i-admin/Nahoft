package org.nahoft.nahoft

import android.content.Context
import android.text.format.DateUtils
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.io.Serializable
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Root(name = "message", strict = false)
data class Message constructor(

    @field:Element(name = "timestamp_string")
    @param:Element(name = "timestamp_string")
    val timestampString: String,

    @field:Element(name = "timestamp")
    @param:Element(name = "timestamp")
    val timestamp: Calendar,

    @field:Element(name = "cipherText")
    @param:Element(name = "cipherText")
    var cipherText: ByteArray,

    @field:Element(name = "from-me", required = false)
    @param:Element(name = "from-me", required = false)
    var fromMe: Boolean,

    @field:Element(name = "sender", required = false)
    @param:Element(name = "sender", required = false)
    var sender: Friend? = null
) : Serializable
{
    constructor(cipherText: ByteArray, sender: Friend, fromMe: Boolean) : this(
        timestampString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.getDefault())),
        timestamp = Calendar.getInstance(),
        cipherText,
        fromMe,
        sender)

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (!cipherText.contentEquals(other.cipherText)) return false

        return true
    }

    override fun hashCode(): Int
    {
        return cipherText.contentHashCode()
    }

    fun getDateStringForList(): String
    {
        if (DateUtils.isToday(timestamp.timeInMillis))
        {
            val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            return dateFormat.format(timestamp.time)
        }
        else
        {
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DATE, -1)
            val yesterdayDate = yesterday.get(Calendar.DATE)
            val todayDate = timestamp.get(Calendar.DATE)

            if (yesterdayDate == todayDate)
            {
                return "Yesterday"
            }
            else
            {
                return timestampString
            }
        }
    }

    fun getDateStringForDetail(): String
    {
        if (DateUtils.isToday(timestamp.timeInMillis))
        {
            val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val timeString = dateFormat.format(timestamp.time)
            return "Today " + timeString
        }
        else
        {
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DATE, -1)
            val yesterdayDate = yesterday.get(Calendar.DATE)
            val todayDate = timestamp.get(Calendar.DATE)

            if (yesterdayDate == todayDate)
            {
                val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeString = dateFormat.format(timestamp.time)
                return "Yesterday" + timeString
            }
            else
            {
                return timestampString
            }
        }
    }

    fun save(context: Context): Message
    {
        Persist.messageList.add(this)
        Persist.saveMessagesToFile(context)

        return this
    }
}