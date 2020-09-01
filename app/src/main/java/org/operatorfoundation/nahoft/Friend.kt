package org.operatorfoundation.nahoft

data class Friend(val id: String) {
    var status: FriendStatus = FriendStatus.Default
}

enum class FriendStatus {
    Default,
    Invited,
    Requested,
    Verified,
    Approved
}