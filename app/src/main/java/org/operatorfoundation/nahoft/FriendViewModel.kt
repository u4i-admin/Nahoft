package org.operatorfoundation.nahoft

import android.content.Context
import androidx.lifecycle.ViewModel
import org.operatorfoundation.codex.PersistenceEncryption
import org.simpleframework.xml.core.Persister
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.lang.Exception

class FriendViewModel: ViewModel() {

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
//        var decryptedBytes: ByteArray? = null
//
//        ObjectInputStream(FileInputStream(file)).use {
//
//            val data = it.readObject()
//
//            // If the data is the type (Map) that we are expecting
//            when (data) {
//
//                is Map<*, *> -> {
//                    decryptedBytes = persistenceEncryption.decrypt(data as HashMap<String, ByteArray>)
//                }
//            }
//        }

        if (decryptedBytes != null) {
            val serializer = Persister()
            val inpuStream = ByteArrayInputStream(decryptedBytes)
            val friends = try { serializer.read(Friends::class.java, inpuStream) } catch (e: Exception) {null}

            friends?.list?.let {
                this.friends = ArrayList(it)
            }
        }
    }
}