package org.nahoft.nahoft

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "friend", strict = false)
data class Friend constructor(

    @field:Element(name = "id")
    @param:Element(name = "id")
    val id: String,

    @field:Element(name = "name")
    @param:Element(name = "name")
    var name: String,

    @field:Element(name = "status")
    @param:Element(name = "status")
    var status: FriendStatus = FriendStatus.Default,

    @field:Element(name = "publicKeyEncoded", required = false)
    @param:Element(name = "publicKeyEncoded", required = false)
    var publicKeyEncoded: ByteArray? = null) : Serializable {

    // Friends represent the same person if they have the same id
    override fun equals(other: Any?): Boolean {

        val friend = other as? Friend
        return this.id == friend?.id
    }
}

enum class FriendStatus: StatusIcon {
    Default {
        override fun getIcon(): Int {
            return R.drawable.ic_person_plain
        }
    },
    Invited {
        override fun getIcon(): Int {
            return R.drawable.invited_friend
        }
    },
    Requested {
        override fun getIcon(): Int {
            return R.drawable.requested_friend
        }
    },
    Verified {
        override fun getIcon(): Int {
            return R.drawable.approved_friend
        }
    },
    Approved {
        override fun getIcon(): Int {
            return R.drawable.verified_friend
        }
    }
}

interface StatusIcon {
    fun getIcon(): Int
}