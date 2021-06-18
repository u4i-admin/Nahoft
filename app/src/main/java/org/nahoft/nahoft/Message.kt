package org.nahoft.nahoft

import android.content.Context
import kotlinx.android.synthetic.main.activity_import_text.*
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Root(name = "message", strict = false)
data class Message constructor(

    @field:Element(name = "timestamp")
    @param:Element(name = "timestamp")
    val timestampString: String,

    @field:Element(name = "cipherText")
    @param:Element(name = "cipherText")
    var cipherText: ByteArray,

    @field:Element(name = "sender", required = false)
    @param:Element(name = "sender", required = false)
    var sender: Friend? = null

) : Serializable
{
    constructor(cipherText: ByteArray, sender: Friend) : this(timestampString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("M/d/y H:m")), cipherText, sender)

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

    fun save(context: Context): Message
    {
        Persist.messageList.add(this)
        Persist.saveMessagesToFile(context)

        return this
    }
}