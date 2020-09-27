package org.nahoft.nahoft

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.io.Serializable
import java.util.*

@Root(name = "message", strict = false)
data class Message constructor(

    @field:Element(name = "timestamp")
    @param:Element(name = "timestamp")
    val timestamp: Date,

    @field:Element(name = "cipherText")
    @param:Element(name = "cipherText")
    var cipherText: ByteArray,

    @field:Element(name = "sender", required = false)
    @param:Element(name = "sender", required = false)
    var sender: Friend? = null

) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (!cipherText.contentEquals(other.cipherText)) return false

        return true
    }

    override fun hashCode(): Int {
        return cipherText.contentHashCode()
    }
}