package org.nahoft.nahoft

import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "friends", strict = false)
data class Friends constructor(
    @field:ElementList(entry = "friend", inline = true)
    @param:ElementList(entry = "friend", inline = true)
    val list: List<Friend>? = null)
{
    fun verifiedSpinnerList(): Array<Friend>
    {
        val verifiedFriends = ArrayList<Friend>()

        // First element
        verifiedFriends.add(0, Friend(" "))
        for (friend in Persist.friendList) {

            if (friend.status == FriendStatus.Verified) {
                verifiedFriends.add(friend)
            }
        }

        return  verifiedFriends.toTypedArray()
    }

    fun allFriendsSpinnerList(): Array<Friend>
    {
        val verifiedFriends = ArrayList<Friend>()
        verifiedFriends.addAll(Persist.friendList)

        // First spinner element should be blank
        verifiedFriends.add(0, Friend(" "))

        return  verifiedFriends.toTypedArray()
    }
}