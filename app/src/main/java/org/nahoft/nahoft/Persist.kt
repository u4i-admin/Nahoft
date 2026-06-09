package org.nahoft.nahoft

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.nahoft.codex.PersistenceEncryption
import org.simpleframework.xml.core.Persister
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import org.libsodium.jni.keys.PublicKey
import org.nahoft.nahoft.models.Friend
import org.nahoft.nahoft.models.FriendStatus
import org.nahoft.nahoft.models.Friends
import org.nahoft.nahoft.models.LoginStatus
import org.nahoft.nahoft.models.Message
import org.nahoft.nahoft.models.Messages
import org.nahoft.util.LockoutLogic
import timber.log.Timber
import java.security.SecureRandom
import java.util.Calendar as JavaCalendar
import java.io.RandomAccessFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class Persist
{
    companion object
    {
        const val sharedPrefImageSaveConsentShownKey = "NahoftImageSaveConsentShown"

        val publicKeyPreferencesKey = "NahoftPublicKey"
        val sharedPrefLoginStatusKey = "NahoftLoginStatus"
        val sharedPrefPasscodeKey = "NahoftPasscode"
        val sharedPrefSecondaryPasscodeKey = "NahoftSecondaryPasscode"
        val sharedPrefFailedLoginAttemptsKey = "NahoftFailedLogins"
        const val sharedPrefActiveIdentityKey = "NahoftActiveIdentity"

        // Expiry-based lockout keys
        val sharedPrefLockoutExpiryKey = "NahoftLockoutExpiry"
        val sharedPrefLockoutElapsedKey = "NahoftLockoutElapsed"

        const val sharedPrefTxFrequencyKHzKey = "NahoftTxFrequencyKHz"
        const val sharedPrefRxFrequencyKHzKey = "NahoftRxFrequencyKHz"
        const val sharedPrefMfskBaseFrequencyHzKey = "NahoftMfskBaseFrequencyHz"
        const val sharedPrefMfskUseFldigiEngineKey = "NahoftMfskUseFldigiEngine"

        val sharedPrefAlreadySeeTutorialKey = "NahoftAlreadySeeTutorial"

        val sharedPrefFilename = "NahoftEncryptedPreferences"

        val sharedPrefKeyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val masterKeyAlias = MasterKeys.getOrCreate(sharedPrefKeyGenParameterSpec)

        const val friendsFilename = "fData.xml"
        const val messagesFilename = "mData.xml"

        // Initialized by EnterPasscodeActivity(main)
        lateinit var status: LoginStatus
        lateinit var encryptedSharedPreferences: EncryptedSharedPreferences

        // Initialized by HomeActivity
        lateinit var friendsFile: File
        lateinit var messagesFile: File
        lateinit var app: Application
//        var sendWithSms by Delegates.notNull<Boolean>()

        var friendList = ArrayList<Friend>()
        var messageList = ArrayList<Message>()

        fun getStatus()
        {
            val statusString = encryptedSharedPreferences.getString(Persist.sharedPrefLoginStatusKey, null)

            status = if (statusString != null)
            {
                try
                {
                    LoginStatus.valueOf(statusString)
                }
                catch (error: Exception)
                {
                    print("Received invalid status from EncryptedSharedPreferences. User is logged out.")
                    LoginStatus.LoggedOut
                }
            }
            else
            {
                LoginStatus.NotRequired
            }
        }

        fun saveLoginStatus() {
            encryptedSharedPreferences
                .edit()
                .putString(sharedPrefLoginStatusKey, status.name)
                .apply()
        }

        /**
         * Returns the lockout duration in milliseconds for a given number of failed attempts.
         *
         * Lockout schedule:
         * - 0-5 attempts: no lockout
         * - 6 attempts: 1 minute
         * - 7 attempts: 5 minutes
         * - 8 attempts: 15 minutes
         * - 9+ attempts: 1000 minutes (triggers data wipe)
         */
        fun getLockoutDurationMillis(failedLoginAttempts: Int): Long
        {
            val minutes = when {
                failedLoginAttempts >= 9 -> 1000
                failedLoginAttempts == 8 -> 15
                failedLoginAttempts == 7 -> 5
                failedLoginAttempts == 6 -> 1
                else -> 0
            }

            return minutes * 60 * 1000L
        }


        /**
         * Checks if the current lockout has expired.
         *
         * Returns true if EITHER:
         * - Wall clock is past the expiry time, OR
         * - Enough real time (elapsedRealtime) has passed since lockout was set
         *
         * if the user travels and their wall clock moves backward,
         * they can still log in once enough real time has passed.
         *
         * Reboot detection: If elapsedRealtime() is less than the stored value,
         * the device has rebooted, fall back to wall clock only.
         */
        fun isLockoutExpired(failedLoginAttempts: Int): Boolean
        {
            return LockoutLogic.isLockoutExpired(
                lockoutDuration = getLockoutDurationMillis(failedLoginAttempts),
                lockoutExpiry = encryptedSharedPreferences.getLong(sharedPrefLockoutExpiryKey, 0L),
                elapsedAtLockout = encryptedSharedPreferences.getLong(sharedPrefLockoutElapsedKey, 0L),
                currentTimeMillis = System.currentTimeMillis(),
                currentElapsedRealtime = SystemClock.elapsedRealtime()
            )
        }

        /**
         * Returns true if the wall clock shows the lockout has expired,
         * but not enough real time has actually passed. This indicates
         * the user moved their clock forward to bypass the lockout.
         *
         * Returns false if:
         * - Device has rebooted (can't reliably detect manipulation)
         * - No active lockout
         * - No manipulation detected
         */
        fun isClockManipulationDetected(failedLoginAttempts: Int): Boolean
        {
            return LockoutLogic.isClockManipulationDetected(
                lockoutDuration = getLockoutDurationMillis(failedLoginAttempts),
                lockoutExpiry = encryptedSharedPreferences.getLong(sharedPrefLockoutExpiryKey, 0L),
                elapsedAtLockout = encryptedSharedPreferences.getLong(sharedPrefLockoutElapsedKey, 0L),
                currentTimeMillis = System.currentTimeMillis(),
                currentElapsedRealtime = SystemClock.elapsedRealtime()
            )
        }

        /**
         * Returns the remaining lockout time in milliseconds.
         * Uses the more accurate of wall clock or elapsed time remaining.
         * Returns 0 if lockout has expired.
         */
        fun getRemainingLockoutMillis(failedLoginAttempts: Int): Long
        {
            return LockoutLogic.getRemainingLockoutMillis(
                lockoutDuration = getLockoutDurationMillis(failedLoginAttempts),
                lockoutExpiry = encryptedSharedPreferences.getLong(sharedPrefLockoutExpiryKey, 0L),
                elapsedAtLockout = encryptedSharedPreferences.getLong(sharedPrefLockoutElapsedKey, 0L),
                currentTimeMillis = System.currentTimeMillis(),
                currentElapsedRealtime = SystemClock.elapsedRealtime()
            )
        }

        /**
         * Saves a login failure and sets up the lockout expiry.
         *
         * Stores:
         * - Failed attempt count
         * - Wall clock time when lockout expires
         * - SystemClock.elapsedRealtime() at lockout creation (for manipulation detection)
         *
         * When failedLoginAttempts is 0, clears all lockout-related keys.
         */
        fun saveLoginFailure(failedLoginAttempts: Int)
        {
            // Save number of failed login attempts
            encryptedSharedPreferences
                .edit()
                .putInt(sharedPrefFailedLoginAttemptsKey, failedLoginAttempts)
                .apply()

            if (failedLoginAttempts == 0)
            {
                // Clear lockout state on successful login
                encryptedSharedPreferences
                    .edit()
                    .remove(sharedPrefLockoutExpiryKey)
                    .remove(sharedPrefLockoutElapsedKey)
                    .apply()
            }
            else
            {
                val lockoutDuration = getLockoutDurationMillis(failedLoginAttempts)
                val lockoutExpiry = System.currentTimeMillis() + lockoutDuration
                val elapsedAtLockout = SystemClock.elapsedRealtime()

                encryptedSharedPreferences
                    .edit()
                    .putLong(sharedPrefLockoutExpiryKey, lockoutExpiry)
                    .putLong(sharedPrefLockoutElapsedKey, elapsedAtLockout)
                    .apply()
            }
        }

        fun accessIsAllowed(): Boolean
        {
            getStatus()

            // Return true if status is NotRequired or LoggedIn
            return status == LoginStatus.NotRequired || status == LoginStatus.LoggedIn
        }

        fun updateFriend(context: Context, friendToUpdate: Friend, newName: String = friendToUpdate.name, newStatus: FriendStatus = friendToUpdate.status, encodedPublicKey: ByteArray? = null) {

            val oldFriend = friendList.find { it.name == friendToUpdate.name }

            encodedPublicKey?.let {
                val publicKey = PublicKey(encodedPublicKey)
                if (publicKey == null) {
                    // Fail early instead of persisting a bad public key
                    return
                }
            }

            oldFriend?.let {
                oldFriend.status = newStatus
                oldFriend.name = newName

                encodedPublicKey?.let { oldFriend.publicKeyEncoded = encodedPublicKey }
            }

            saveFriendsToFile(context)
        }

        // Save something to Encrypted Shared Preferences
        fun saveKey(key:String, value:String) {
            encryptedSharedPreferences
                .edit()
                .putString(key, value)
                .apply()
        }

        fun saveBooleanKey(key:String, value:Boolean) {
            encryptedSharedPreferences
                .edit()
                .putBoolean(key, value)
                .apply()
        }

        fun loadBooleanKey(key: String): Boolean {
            return encryptedSharedPreferences.getBoolean(key, false)
        }

        fun saveIntKey(key: String, value: Int) {
            encryptedSharedPreferences
                .edit()
                .putInt(key, value)
                .apply()
        }

        fun loadIntKey(key: String, default: Int): Int {
            return encryptedSharedPreferences.getInt(key, default)
        }

        // Remove something from Encrypted Shared Preferences
        private fun deleteKey(key:String) {
            encryptedSharedPreferences
                .edit()
                .remove(key)
                .apply()
        }

        fun deletePasscode() {
            deleteKey(sharedPrefPasscodeKey)
            deleteKey(sharedPrefSecondaryPasscodeKey)
        }

        fun loadEncryptedSharedPreferences(context: Context) {
            encryptedSharedPreferences = EncryptedSharedPreferences.create(
                sharedPrefFilename,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        }

        fun deleteMessage(context: Context, message: Message) {
            messageList.remove(message)
            saveMessagesToFile(context)
        }

        fun secureDelete(file: File)
        {
            if (!file.exists()) return

            try
            {
                val length = file.length()
                val random = SecureRandom()

                // Overwrite with random data multiple times
                repeat(3)
                {
                    RandomAccessFile(file, "rws").use { raf ->
                        val buffer = ByteArray(4096)

                        var remaining = length

                        while (remaining > 0)
                        {
                            val toWrite = minOf(remaining, buffer.size.toLong()).toInt()
                            random.nextBytes(buffer)
                            raf.write(buffer, 0, toWrite)
                            remaining -= toWrite
                        }

                        raf.fd.sync()
                    }
                }

                // Delete the file
                file.delete()
            }
            catch (e: kotlin.Exception)
            {
                Timber.d("Error deleting a file: ${e.printStackTrace()}")
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun clearAllData(secondaryCode: Boolean)
        {
            secureDelete(friendsFile)
            secureDelete(messagesFile)

            for (friend in friendList)
            {
                overwriteFriend(friend)
            }
            friendList.clear()


            for ((index, message) in messageList.withIndex())
            {
                messageList[index] = overwriteMessage(message)
            }
            messageList.clear()

            var passcode = ""
            if (secondaryCode) {
                passcode = encryptedSharedPreferences.getString(sharedPrefSecondaryPasscodeKey, "").toString()
            }

            // Overwrite the keys to EncryptedSharedPreferences
            repeat(3)
            {
                val outputBytes = ByteArray(32)
                val secRandom = SecureRandom.getInstanceStrong()
                secRandom.nextBytes(outputBytes)
                val keyHex = outputBytes.toHexString()

                encryptedSharedPreferences
                    .edit()
                    .putString("NahoftPrivateKey", keyHex)
                    .putString(publicKeyPreferencesKey, keyHex)
                    .apply()
            }

            // Remove Everything from EncryptedSharedPreferences
            encryptedSharedPreferences
                .edit()
                .clear()
                .apply()

            if (secondaryCode)
            {
                saveKey(sharedPrefPasscodeKey, passcode)
                saveBooleanKey(sharedPrefAlreadySeeTutorialKey, true)
                status = LoginStatus.LoggedIn
                saveLoginStatus()
            }
            else
            {
                status = LoginStatus.NotRequired
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun overwriteFriend(friend: Friend)
        {
            val secRandom = SecureRandom.getInstanceStrong()

            if (friend.publicKeyEncoded != null)
            {
                secRandom.nextBytes(friend.publicKeyEncoded)
            }

            val nameBytes = ByteArray(friend.name.length + secRandom.nextInt(10))
            secRandom.nextBytes(nameBytes)
            friend.name =  nameBytes.toHexString()
            friend.status = FriendStatus.Default
        }

        fun overwriteMessage(message: Message): Message
        {
            val secRandom = SecureRandom.getInstanceStrong()
            secRandom.nextBytes(message.cipherText)
            val randomTimestamp = LocalDateTime.ofEpochSecond(
                Random.nextLong(946684800, System.currentTimeMillis() / 1000),
                0,
                java.time.ZoneOffset.UTC
            ).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))

            return Message(randomTimestamp, randomCalendar(), message.cipherText, false)
        }

        fun randomCalendar(): JavaCalendar
        {
            val minMillis = 946684800000L  // 2000-01-01
            val maxMillis = System.currentTimeMillis()

            val randomMillis = Random.nextLong(minMillis, maxMillis)

            return JavaCalendar.getInstance().apply {
                timeInMillis = randomMillis
            }
        }

        fun saveFriendsToFile(context: Context) {
            val serializer = Persister()
            val outputStream = ByteArrayOutputStream()
            val friendsObject = Friends(friendList)

            try { serializer.write(friendsObject, outputStream) }
            catch (error: Exception) {
                print("Failed to serialize our friends list: $error")
            }

            PersistenceEncryption().writeEncryptedFile(friendsFile, outputStream.toByteArray(), context)
        }

        fun saveMessagesToFile(context: Context) {
            val serializer = Persister()
            val outputStream = ByteArrayOutputStream()
            val messagesObject = Messages(messageList)
            try { serializer.write(messagesObject, outputStream) }
            catch (error: Exception) {
                print("Failed to serialize our messages list: $error")
                return
            }

            PersistenceEncryption().writeEncryptedFile(messagesFile, outputStream.toByteArray(), context)
        }

        fun resetFriend(context: Context, friend: Friend)
        {
            messageList.removeIf { it.sender == friend }
            friend.publicKeyEncoded = null
            updateFriend(context, friend, newStatus = FriendStatus.Default)

            saveMessagesToFile(context)
            saveFriendsToFile(context)
        }

        fun removeFriendAt(context: Context, friend: Friend)
        {
            val byeFriend = friendList.find { it.name == friend.name }
            messageList.removeIf { it.sender == byeFriend }
            byeFriend?.publicKeyEncoded = null
            friendList.remove(byeFriend)

            saveMessagesToFile(context)
            saveFriendsToFile(context)
        }
    }

}