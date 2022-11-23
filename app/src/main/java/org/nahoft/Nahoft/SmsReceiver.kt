package org.nahoft.nahoft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import org.nahoft.codex.Codex
import org.nahoft.codex.KeyOrMessage
import org.nahoft.nahoft.activities.HomeActivity
import org.nahoft.util.showAlert

class SmsReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val data = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val smsSentByThisFriend = Persist.friendList.firstOrNull { PhoneNumberUtils.compare(it.phone, data[0].originatingAddress) }
        if (smsSentByThisFriend != null) {
            var fullMessage = ""
            for (sms in data) {
                fullMessage += sms.displayMessageBody
            }
            tryToDecryptMessage(context, smsSentByThisFriend, fullMessage)
        }
    }

    private fun tryToDecryptMessage(context: Context?, friend: Friend, encryptedMessage: String) {
        val decodeResult = Codex().decode(encryptedMessage)

        if (decodeResult != null)
        {
            when (decodeResult.type)
            {
                KeyOrMessage.EncryptedMessage ->
                {
                    // Create Message Instance
                    val newMessage = Message(decodeResult.payload, friend, false)
                    if (context != null) {
                        newMessage.save(context)
                    }
                }
//                KeyOrMessage.Key ->
//                {
//                    updateKeyAndStatus(context, friend, decodeResult.payload)
//                }
            }
        }
//        else
//        {
//            this.showAlert("Looks like that was a normal message. Not a special message")
//        }
    }

    private fun updateKeyAndStatus(context: Context?, keySender: Friend, keyData: ByteArray) {
        when (keySender.status)
        {
            FriendStatus.Default ->
            {
                if (context != null) {
                    Persist.updateFriend(
                        context,
                        friendToUpdate = keySender,
                        newStatus = FriendStatus.Requested,
                        encodedPublicKey = keyData
                    )
                }
                keySender.status = FriendStatus.Requested
            }

            FriendStatus.Invited ->
            {
                if (context != null) {
                    Persist.updateFriend(
                        context,
                        friendToUpdate = keySender,
                        newStatus = FriendStatus.Approved,
                        encodedPublicKey = keyData
                    )
                }
                keySender.status = FriendStatus.Approved
                keySender.publicKeyEncoded = keyData
            }
//
//            else ->
//                this.showAlert(getString(R.string.alert_text_unable_to_update_friend_status))
        }
    }
}