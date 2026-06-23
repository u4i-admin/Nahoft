package org.nahoft.nahoft.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import org.nahoft.codex.PersistenceEncryption
import org.nahoft.nahoft.models.Message
import org.nahoft.nahoft.models.Messages
import org.simpleframework.xml.core.Persister
import java.io.ByteArrayInputStream
import java.io.File
import java.lang.Exception

class MessageViewModel: ViewModel()
{

    private var messages: ArrayList<Message>? = null

    fun getMessages(file: File, context: Context): ArrayList<Message>?
    {

        if (messages == null) {
            loadMessages(file, context)
        }

        return messages ?: arrayListOf()
    }

    private fun loadMessages(file: File, context: Context)
    {
        val persistenceEncryption = PersistenceEncryption()
        val decryptedBytes = persistenceEncryption.readEncryptedFile(file, context)

        if (decryptedBytes.isNotEmpty())
        {
            val serializer = Persister()
            val inputStream = ByteArrayInputStream(decryptedBytes)
            val messages = try
            { serializer.read(Messages::class.java, inputStream)
            }
            catch (error: Exception)
            {
                print("Error loading messages: $error")
                null
            }

            this.messages = messages?.list?.let { ArrayList(it) }
        }
    }
}