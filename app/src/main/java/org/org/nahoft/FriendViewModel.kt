package org.org.nahoft

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import org.org.codex.PersistenceEncryption
import org.simpleframework.xml.core.Persister
import java.io.ByteArrayInputStream
import java.io.File
import java.lang.Exception

object FriendViewModel: ViewModel() {

    private var friends: ArrayList<Friend>? = null

    fun getFriends(file: File, context: Context): ArrayList<Friend> {

        if (friends == null) {
            loadFriends(file, context)
        }

        return friends ?: arrayListOf()
    }

    private fun loadFriends(file: File, context: Context) {

        var persistenceEncryption = PersistenceEncryption()
        val decryptedBytes = persistenceEncryption.readEncryptedFile(file, context)

        if (decryptedBytes != null) {
            val serializer = Persister()
            val inpuStream = ByteArrayInputStream(decryptedBytes)
            val friends = try { serializer.read(Friends::class.java, inpuStream)
            } catch (e: Exception) {null}

            friends?.list?.let {
                this.friends = ArrayList(it)
            }
        }
    }
}