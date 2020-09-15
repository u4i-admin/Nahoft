package org.operatorfoundation.nahoft

import android.graphics.drawable.Drawable
import android.media.Image
import java.security.PublicKey

data class Friend(val id: String, var name: String) {
    var status: FriendStatus = FriendStatus.Default
    var publicKey: PublicKey? = null
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
            return R.drawable.verified_friend
        }
    },
    Approved {
        override fun getIcon(): Int {
            return R.drawable.approved_friend
        }
    }
}

interface StatusIcon {
    fun getIcon(): Int
}