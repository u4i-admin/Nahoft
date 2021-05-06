package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.UriCompat
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_message.*
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.coroutines.*
import org.nahoft.codex.Encryption
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.R
import org.nahoft.org.nahoft.swatch.Encoder
import org.nahoft.util.showAlert
import org.nahoft.util.RequestCodes
import org.nahoft.util.ShareUtil
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI

class NewMessageActivity : AppCompatActivity() {

    private var selectedFriend: Friend? = null
    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        // Select Friend Button
        friend_button.setOnClickListener {
            selectFriend()
        }

        // Send message as text button
        send_as_text_button.setOnClickListener {
            trySendingOrSavingMessage(false, false)
        }

        // Send message as image button
        send_as_image_button.setOnClickListener {
            trySendingOrSavingMessage(true, false)
        }

        // TODO: Save message as image button
        // trySendingOrSavingMessage(true, true)
        save_image_button.setOnClickListener {
            trySendingOrSavingMessage(true, true)
        }
    }

    override fun onStop() {

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })
        cleanUp()
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        unregisterReceiver(receiver)
    }

    private fun selectFriend() {
        val intent = FriendSelectionActivity.newIntent(this@NewMessageActivity)
        Intent(this, FriendSelectionActivity::class.java)
        startActivityForResult(intent, RequestCodes.selectFriendCode)
    }

    private fun pickImageFromGallery(saveImage: Boolean) {
        val pickImageIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (saveImage) {
            startActivityForResult(pickImageIntent, RequestCodes.selectImageForSavingCode)
        } else {
            startActivityForResult(pickImageIntent, RequestCodes.selectImageForSharingCode)
        }
    }

    private fun trySendingOrSavingMessage(isImage: Boolean, saveImage: Boolean) {

        // Make sure there is a message to send
        val message = editMessageText.text.toString()
        if (message.isBlank()) {
            showAlert(getString(R.string.alert_text_write_a_message_to_send))
            return
        }

        if (message.length > compose_message_text_layout.counterMaxLength) {
            showAlert(getString(R.string.alert_text_message_too_long))
            return
        }

        // Make sure there is a friend to create the message for.
        if (selectedFriend == null) {
            this.showAlert(getString(R.string.alert_text_select_friend_to_send_message))
            return
        }

        if (isImage) {
            // If the message is sent as an image
            pickImageFromGallery(saveImage)
        } else {
            // If the message is sent as text
            if (selectedFriend!!.publicKeyEncoded != null) {

                // Share this message as a text
                ShareUtil.shareText(this, message, selectedFriend!!.publicKeyEncoded!!)

                // Clean up the text box and the friend selection.
                editMessageText.text?.clear()
                selectedFriend = null
                friend_button.text = getString(R.string.hintOnChooseFriendButton)
            } else {
                this.showAlert(getString(R.string.alert_text_verified_friends_only))
                return
            }
        }
    }

    @ExperimentalUnsignedTypes
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RequestCodes.selectImageForSharingCode) {
                if (selectedFriend != null) {

                    // We can only share an image if a recipient with a public key has been selected
                    selectedFriend?.publicKeyEncoded?.let {

                        // Get the message text
                        val message = editMessageText.text.toString()

                        // get data?.data as URI
                        val imageURI = data?.data

                        imageURI?.let {
                            imageShareProgressBar.visibility = View.VISIBLE
                            shareOrSaveAsImage(
                                imageURI,
                                message,
                                selectedFriend!!.publicKeyEncoded!!,
                                false
                            )
                            editMessageText.text?.clear()
                            selectedFriend = null
                            friend_button.text = getString(R.string.hintOnChooseFriendButton)
                        }
                    }
                } else {
                    this.showAlert(getString(R.string.alert_text_verified_friends_only))
                    return
                }

            } else if (requestCode == RequestCodes.selectFriendCode) {

                // Get Friend information from data
                val friend =
                    data?.getSerializableExtra(RequestCodes.friendExtraTaskDescription) as? Friend

                // If friend is not null use it to set the button title and set selectedFriend
                friend?.let {
                    this.selectedFriend = friend
                    friend_button.text = friend.name
                }
            } else if (requestCode == RequestCodes.selectImageForSavingCode) {
                if (selectedFriend != null) {

                    // We can only share an image if a recipient with a public key has been selected
                    selectedFriend?.publicKeyEncoded?.let {

                        // Get the message text
                        val message = editMessageText.text.toString()

                        // get data?.data as URI
                        val imageURI = data?.data

                        imageURI?.let {
                            imageShareProgressBar.visibility = View.VISIBLE
                            shareOrSaveAsImage(
                                imageURI,
                                message,
                                selectedFriend!!.publicKeyEncoded!!,
                                true
                            )
                            editMessageText.text?.clear()
                            selectedFriend = null
                            friend_button.text = getString(R.string.hintOnChooseFriendButton)
                        }
                    }
                } else {
                    this.showAlert(getString(R.string.alert_text_verified_friends_only))
                    return
                }
            }
        }
    }

    @ExperimentalUnsignedTypes
    private fun shareOrSaveAsImage(
        imageUri: Uri,
        message: String,
        encodedFriendPublicKey: ByteArray,
        saveImage: Boolean
    ) {
        try {
            // Encrypt the message
            val encryptedMessage = Encryption().encrypt(encodedFriendPublicKey, message)

            val newUri: Deferred<Uri?> =
                coroutineScope.async(Dispatchers.IO) {

                    // Encode the image
                    val swatch = Encoder()
                    return@async swatch.encode(applicationContext, encryptedMessage, imageUri)
                }

            coroutineScope.launch(Dispatchers.Main) {
                val maybeUri = newUri.await()

                imageShareProgressBar.visibility = View.INVISIBLE

                // Save bitmap to image roll to get URI for sharing intent
                if (maybeUri != null) {
                    if (saveImage) {
                        //TODO: Save Image
                        //saveImageToGallery()
                        //saveMediaToStorage((Context, ByteArray, coverUri: Uri): Uri? )

                    } else {
                        ShareUtil.shareImage(applicationContext, maybeUri!!)
                    }
                } else {
                    applicationContext.showAlert(applicationContext.getString(R.string.alert_text_unable_to_process_request))
                    print("Unable to send message as photo, we were unable to encode the selected image.")
                }
            }

        } catch (exception: SecurityException) {
            applicationContext.showAlert(applicationContext.getString(R.string.alert_text_unable_to_process_request))
            print("Unable to send message as photo, we were unable to encrypt the mess56age.")
            return
        }
    }

    private fun makeWait() {
        imageShareProgressBar.visibility = View.VISIBLE
        send_as_text_button.isEnabled = false
        send_as_image_button.isClickable = false
        send_as_image_button.isEnabled = false
        send_as_image_button.isClickable = false
        friend_button.isEnabled = false
        friend_button.isClickable = false
    }

    private fun noMoreWaiting() {
        imageShareProgressBar.visibility = View.INVISIBLE
        send_as_text_button.isEnabled = true
        send_as_text_button.isClickable = true
        send_as_image_button.isEnabled = true
        send_as_image_button.isClickable = true
        friend_button.isEnabled = true
        friend_button.isClickable = true
    }

    fun cleanUp() {
        //selectedFriend = null
        //selectedFriend =Friend()
        editMessageText.text?.clear()
        //showAlert("New Message Logout Timer Broadcast Received", length = Toast.LENGTH_LONG)
    }

    private fun saveMediaToStorage(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            showAlert("Saved to Photos")
        }
    }

    /// @param folderName can be your app's name
    private fun saveImageToGallery(bitmap: Bitmap, context: Context, folderName: String) {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = contentValues()
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName)
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            // RELATIVE_PATH and IS_PENDING are introduced in API 29.

            val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val directory = File(Environment.getExternalStorageDirectory().toString() + separator + folderName)
            // getExternalStorageDirectory is deprecated in API 29

            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".png"
            val file = File(directory, fileName)
            saveImageToStream(bitmap, FileOutputStream(file))
            if (file.absolutePath != null) {
                val values = contentValues()
                values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                // .DATA is deprecated in API 29
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
        }
    }

    private fun contentValues() : ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
