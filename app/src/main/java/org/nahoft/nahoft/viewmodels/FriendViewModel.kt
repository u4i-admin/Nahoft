package org.nahoft.nahoft.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import org.nahoft.codex.PersistenceEncryption
import org.nahoft.nahoft.models.Friend
import org.nahoft.nahoft.models.Friends
import org.simpleframework.xml.core.Persister
import java.io.ByteArrayInputStream
import java.io.File
import java.lang.Exception

object FriendViewModel: ViewModel()
{

    private var friends: ArrayList<Friend>? = null

    fun getFriends(file: File, context: Context): ArrayList<Friend> {

        //if (friends == null) {
            loadFriends(file, context)
        //}

        return friends ?: arrayListOf()
    }

    private fun loadFriends(file: File, context: Context) {

        val persistenceEncryption = PersistenceEncryption()
        val decryptedBytes = persistenceEncryption.readEncryptedFile(file, context)

        if (decryptedBytes.isNotEmpty()) {
            val serializer = Persister()
            val inputStream = ByteArrayInputStream(decryptedBytes)
            val friends = try { serializer.read(Friends::class.java, inputStream)
            } catch (error: Exception) {
                print("Error loading friends: $error")
                null
            }

            this.friends = friends?.list?.let { ArrayList(it)}
        }
    }
}