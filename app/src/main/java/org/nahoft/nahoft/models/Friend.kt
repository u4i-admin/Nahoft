package org.nahoft.nahoft.models

import android.content.Context
import org.nahoft.nahoft.R
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "friend", strict = false)
data class Friend constructor(

    @field:Element(name = "name")
    @param:Element(name = "name")
    var name: String,

//    @field:Element(name = "phone", required = false)
//    @param:Element(name = "phone", required = false)
//    var phone: String? = null,

    @field:Element(name = "status")
    @param:Element(name = "status")
    var status: FriendStatus = FriendStatus.Default,

    @field:Element(name = "publicKeyEncoded", required = false)
    @param:Element(name = "publicKeyEncoded", required = false)
    var publicKeyEncoded: ByteArray? = null
) : Serializable {

    // Friends represent the same person if they have the same name
    override fun equals(other: Any?): Boolean {

        val friend = other as? Friend
        return this.name == friend?.name
    }

    override fun toString(): String {
        return name // What to display in the Spinner list.
    }

    fun getStatusString(context: Context): String
    {
        return when (this.status)
        {
            FriendStatus.Default -> context.getString(R.string.friend_status_default)
            FriendStatus.Requested -> context.getString(R.string.friend_status_requested)
            FriendStatus.Invited -> context.getString(R.string.friend_status_invited)
            FriendStatus.Verified -> context.getString(R.string.friend_status_verified)
            FriendStatus.Approved -> context.getString(R.string.friend_status_approved)
        }
    }
}

enum class FriendStatus: StatusIcon {
    Default {
        override fun getIcon(): Int {
            return R.drawable.status_icon_default
        }
    },
    Invited {
        override fun getIcon(): Int {
            return R.drawable.status_icon_invited
        }
    },
    Requested {
        override fun getIcon(): Int {
            return R.drawable.status_icon_requested
        }
    },
    Verified {
        override fun getIcon(): Int {
            return R.drawable.status_icon_verified
        }
    },
    Approved {
        override fun getIcon(): Int {
            return R.drawable.status_icon_approved
        }
    }
}

interface StatusIcon {
    fun getIcon(): Int
}