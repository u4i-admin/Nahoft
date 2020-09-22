package org.org.nahoft

import android.app.Application
import java.io.File

class Nahoft: Application() {

    companion object {
        lateinit var friendsFile: File
        var friendList = ArrayList<Friend>()
    }
    override fun onCreate() {
        super.onCreate()

        friendsFile = File(filesDir.absolutePath + File.separator + FileConstants.datasourceFilename )

        // Load our existing friends list from our encrypted file
        if (friendsFile.exists()) {
            val friendsToAdd = FriendViewModel.getFriends(friendsFile, applicationContext)
            friendList.addAll(friendsToAdd)
        }
    }
}