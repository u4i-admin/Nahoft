package org.operatorfoundation.nahoft

import android.graphics.drawable.Drawable
import android.media.Image

data class Friend(val id: String, var name: String) {
    var status: FriendStatus = FriendStatus.Default
}

enum class FriendStatus: StatusIcon {
    Default {
        override fun getIcon(): Drawable {
            TODO("return default icon")
        }
    },
    Invited {
        override fun getIcon(): Drawable {
            TODO("return invited icon")
        }
    },
    Requested {
        override fun getIcon(): Drawable {
            TODO("return requested icon")
        }
    },
    Verified {
        override fun getIcon(): Drawable {
            TODO("return verified icon")
        }
    },
    Approved {
        override fun getIcon(): Drawable {
            TODO("return approved icon")
        }
    }
}

interface StatusIcon {
    fun getIcon(): Drawable
}