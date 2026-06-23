package org.nahoft.nahoft.models

import org.nahoft.nahoft.Persist
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

        // Get only verified friends
        for (friend in Persist.Companion.friendList) {

            if (friend.status == FriendStatus.Verified) {
                verifiedFriends.add(friend)
            }
        }

        return  verifiedFriends.toTypedArray()
    }

    fun allFriendsSpinnerList(): Array<Friend>
    {
        val allFriends = ArrayList<Friend>()
        allFriends.addAll(Persist.Companion.friendList)

        // First spinner element should be blank
        allFriends.add(0, Friend(" "))

        return  allFriends.toTypedArray()
    }
}