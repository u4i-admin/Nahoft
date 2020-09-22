package org.org.nahoft

import android.app.Application
import java.io.File

class Nahoft: Application() {

    companion object {

        // TODO: lateinit here is somewhat dangerous
        lateinit var friendsFile: File

        var friendList = ArrayList<Friend>()
    }

}