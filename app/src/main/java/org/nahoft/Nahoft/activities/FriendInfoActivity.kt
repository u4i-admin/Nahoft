package org.nahoft.nahoft.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.telephony.SmsManager
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_friend_info.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.coroutines.*
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Codex
import org.nahoft.codex.Encryption
import org.nahoft.codex.KeyOrMessage
import org.nahoft.nahoft.*
import org.nahoft.nahoft.fragments.*
import org.nahoft.org.nahoft.swatch.Decoder
import org.nahoft.org.nahoft.swatch.Encoder
import org.nahoft.util.RequestCodes
import org.nahoft.util.ShareUtil
import org.nahoft.util.showAlert

class FriendInfoActivity: AppCompatActivity()
{
    private var decodePayload: ByteArray? = null
    private lateinit var thisFriend: Friend
    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    private val TAG = "FriendInfoActivity"
    private val menuFragmentTag = "MenuFragment"
    private var isShareImageButtonShow: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_info)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        if (!Persist.accessIsAllowed()) { sendToLogin() }

        // Get our pending friend
        val maybeFriend = intent.getSerializableExtra(RequestCodes.friendExtraTaskDescription) as? Friend

        if (maybeFriend == null)
        { // this should never happen, get out of this activity.
            Log.e(TAG, "Attempted to open FriendInfoActivity, but Friend was null.")
            return
        }
        else
        {
            thisFriend = maybeFriend
        }

        setClickListeners()
        setupViewByStatus()
        receivedSharedMessage()
    }

    override fun onResume() {
        super.onResume()

        setupViewByStatus()
    }

    @ExperimentalUnsignedTypes
    private fun receivedSharedMessage() {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let{
            //Attempt to decode the message
            decodeStringMessage(it)
        }

        intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
            try
            {
                // See if we received an image message
                val extraStream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                if (extraStream != null)
                {
                    (extraStream as? Uri)?.let {
                        decodeImage(it)
                    }
                }
            }
            catch (e:Exception)
            {
                showAlert(getString(R.string.alert_text_unable_to_process_request))
            }
        }
    }

    override fun onBackPressed() {
        returnButtonPressed()
    }

    private fun setClickListeners() {
        btn_resend_invite.setOnClickListener {
            inviteClicked()
        }

        button_back.setOnClickListener {
            returnButtonPressed()
        }

        send_as_text.setOnClickListener {
            trySendingOrSavingMessage(isImage = false, saveImage = false)
        }

        send_as_image.setOnClickListener {
            showHideShareImageButtons()
        }

        save_as_image.setOnClickListener {
            trySendingOrSavingMessage(isImage = true, saveImage = true)
        }

        share_as_image.setOnClickListener {
            trySendingOrSavingMessage(isImage = true, saveImage = false)
        }

        btn_import_text.setOnClickListener {
            importInvitationClicked()
        }

        btn_import_image.setOnClickListener {
            handleImageImport()
        }

        btn_help.setOnClickListener {
            val helpActivity = Intent(this, HelpActivity::class.java)
            startActivity(helpActivity)
        }

        profile_picture.setOnClickListener {
            showMenuFragment()
        }

        tv_friend_name.setOnClickListener {
            showMenuFragment()
        }
    }

    private fun showHideShareImageButtons() {
        share_as_image.animate().apply {
            duration = 500
            translationY(if (isShareImageButtonShow) 0F else -175F)
            translationX(if (isShareImageButtonShow) 0F else 150F)
        }
        save_as_image.animate().apply {
            duration = 500
            translationY(if (isShareImageButtonShow) 0F else -175F)
        }
        isShareImageButtonShow = !isShareImageButtonShow
    }

    private fun returnButtonPressed() {
        val lastFragment = supportFragmentManager.fragments.last()
        if (lastFragment.tag == menuFragmentTag) {
            setupViewByStatus()
        } else {
            finish()
        }
    }

    private fun showMenuFragment() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(
            R.id.frame_placeholder,
            MenuFragment.newInstance(thisFriend),
            menuFragmentTag
        )
        ft.commit()
        btn_import_image.isVisible = false
        btn_import_text.isVisible = false
        btn_resend_invite.isVisible = false
        send_message_container.isVisible = false
        if (isShareImageButtonShow) showHideShareImageButtons()
    }

    fun showVerificationStep() {
        if (thisFriend.status == FriendStatus.Approved || thisFriend.status == FriendStatus.Verified) {
            val ft = supportFragmentManager.beginTransaction()
            val codex = Codex()
            val friendCode = codex.encodeKey(PublicKey(thisFriend.publicKeyEncoded).toBytes())
            val userCode = codex.encodeKey(Encryption().ensureKeysExist().publicKey.toBytes())
            ft.replace(
                R.id.frame_placeholder,
                VerifyStatusFragment.newInstance(userCode, friendCode, thisFriend.name),
                menuFragmentTag
            )
            ft.commit()
            btn_import_image.isVisible = false
            btn_import_text.isVisible = false
            btn_resend_invite.isVisible = true
            send_message_container.isVisible = false
        }
    }

    private fun setupViewByStatus() {
        tv_friend_name.text = thisFriend.name
        val ft = supportFragmentManager.beginTransaction()
        when (thisFriend.status) {
            FriendStatus.Default -> {
                status_icon_image_view.setImageResource(FriendStatus.Default.getIcon())
                ft.replace(R.id.frame_placeholder, DefaultStatusFragment())
                ft.commit()
                btn_import_image.isVisible = false
                btn_import_text.isVisible = false
                btn_resend_invite.isVisible = false
                send_message_container.isVisible = false
            }
            FriendStatus.Requested -> {
                status_icon_image_view.setImageResource(FriendStatus.Requested.getIcon())
                ft.replace(R.id.frame_placeholder, DefaultStatusFragment())
                ft.commit()
                btn_import_image.isVisible = false
                btn_import_text.isVisible = false
                btn_resend_invite.isVisible = false
                send_message_container.isVisible = false
            }
            FriendStatus.Invited -> {
                status_icon_image_view.setImageResource(FriendStatus.Invited.getIcon())
                ft.replace(R.id.frame_placeholder, InvitedStatusFragment())
                ft.commit()
                btn_import_image.isVisible = false
                btn_import_text.isVisible = false
                btn_resend_invite.isVisible = true
                send_message_container.isVisible = false
            }
            FriendStatus.Verified -> {
                status_icon_image_view.setImageResource(FriendStatus.Verified.getIcon())
                ft.replace(R.id.frame_placeholder, VerifiedStatusFragment.newInstance(thisFriend))
                ft.commit()
                btn_import_image.isVisible = true
                btn_import_text.isVisible = true
                btn_resend_invite.isVisible = false
                send_message_container.isVisible = true
                verified_status_icon_image_view.isVisible = true
            }
            FriendStatus.Approved -> {
                status_icon_image_view.setImageResource(FriendStatus.Approved.getIcon())
                ft.replace(R.id.frame_placeholder, VerifiedStatusFragment.newInstance(thisFriend))
                ft.commit()
                btn_import_image.isVisible = true
                btn_import_text.isVisible = true
                btn_resend_invite.isVisible = false
                send_message_container.isVisible = true
//                val codex = Codex()
//                val friendCode = codex.encodeKey(PublicKey(thisFriend.publicKeyEncoded).toBytes())
//                val userCode = codex.encodeKey(Encryption().ensureKeysExist().publicKey.toBytes())
//                ft.replace(R.id.frame_placeholder, VerifyStatusFragment.newInstance(userCode, friendCode, thisFriend.name))
//                ft.commit()
//                btn_import_image.isVisible = false
//                btn_import_text.isVisible = false
//                btn_resend_invite.isVisible = true
//                send_message_container.isVisible = false
            }
        }
    }

    fun inviteClicked() {
        // Get user's public key to send to contact
        val userPublicKey = Encryption().ensureKeysExist().publicKey
        val keyBytes = userPublicKey.toBytes()

        // Share the key
        ShareUtil.shareKey(this, keyBytes)

        if (thisFriend.status == FriendStatus.Requested)
        {
            // We have already received an invitation from this friend.
            // Set friend status to approved.
            thisFriend.status = FriendStatus.Approved
            Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Approved)
            setupViewByStatus()
        }
        else
        {
            // We have not received an invitation from this friend.
            // Set friend status to Invited
            thisFriend.status = FriendStatus.Invited
            Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Invited)
            setupViewByStatus()
        }
    }

    fun importInvitationClicked() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_AddFriendAlertDialog))
        val title = SpannableString(getString(R.string.import_text))

        // alert dialog title align center
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)

        // Set the input - EditText
        val inputEditText = EditText(this)
        inputEditText.setBackgroundResource(R.drawable.btn_bkgd_light_grey_outline_8)
        inputEditText.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
        inputEditText.setPadding(20)
        inputEditText.height = 500
        inputEditText.gravity = Gravity.TOP
        inputEditText.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        builder.setView(inputEditText)
        builder.setPositiveButton(resources.getString(R.string.import_text))
        { _, _->
            if (inputEditText.text.isNotEmpty())
            {
                if (inputEditText.text.length > 5000)
                {
                    showAlert(getString(R.string.alert_text_message_too_long))
                } else {
                    decodeStringMessage(inputEditText.text.toString())
                }
            }
        }
        builder.setNeutralButton(resources.getString(R.string.cancel_button)) { dialog, _->
            dialog.cancel()
        }.create().show()
    }

    private fun trySendingOrSavingMessage(isImage: Boolean, saveImage: Boolean) {
        // Make sure there is a message to send
        val message = message_edit_text.text.toString()

        if (message.isBlank()) {
            showAlert(getString(R.string.alert_text_write_a_message_to_send))
            return
        }

        if (message.length > 5000) {
            showAlert(getString(R.string.alert_text_message_too_long))
            return
        }

        if (isImage) {
            // If the message is sent as an image
            ActivityCompat.requestPermissions(
                this@FriendInfoActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
            pickImageFromGallery(saveImage)
        } else {
            // If the message is sent as text
            if (thisFriend.publicKeyEncoded != null) {
                val encryptedMessage = Encryption().encrypt(thisFriend.publicKeyEncoded!!, message)
                if (Persist.loadBooleanKey(Persist.sharedPrefUseSmsAsDefaultKey) && (thisFriend.phone?.isNotEmpty() == true)) {
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        this, Manifest.permission.SEND_SMS
                    )
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), RequestCodes.requestPermissionCode)
                        showAlert(getString(R.string.sms_permission_needed))
                        return
                    } else {
                        val codex = Codex()
                        try {
                            val encodedMessage = codex.encodeEncryptedMessage(encryptedMessage)
                            try {
                                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
                                    this.getSystemService(SmsManager::class.java)
                                } else {
                                    SmsManager.getDefault()
                                }
                                val parts = smsManager.divideMessage(encodedMessage)
                                smsManager.sendMultipartTextMessage(
                                    thisFriend.phone,
                                    null,
                                    parts,
                                    null,
                                    null
                                )
                                saveMessage(encryptedMessage, thisFriend, true)
                            } catch (e: Exception) {
                                this.showAlert(getString(R.string.unable_to_send_sms))
                                return
                            }

                        } catch (exception: SecurityException) {
                            this.showAlert(getString(R.string.alert_text_unable_to_process_request))
                            return
                        }
                    }
                } else {
                    ShareUtil.shareText(this, message, thisFriend.publicKeyEncoded!!)
                    saveMessage(encryptedMessage, thisFriend, true)
                }

                message_edit_text.text?.clear()
            } else {
                this.showAlert(getString(R.string.alert_text_verified_friends_only))
                return
            }
        }
    }

    private fun pickImageFromGallery(saveImage: Boolean) {
        // Calling GetContent contract
        val pickImageIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (saveImage) {
            startActivityForResult(pickImageIntent, RequestCodes.selectImageForSavingCode)
        } else {
            startActivityForResult(pickImageIntent, RequestCodes.selectImageForSharingCode)
        }
    }

    private fun handleImageImport()
    {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, RequestCodes.selectImageForImport)
    }

    @ExperimentalUnsignedTypes
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == RequestCodes.selectImageForImport)
            {
                // get data?.data as URI
                val imageURI = data?.data
                imageURI?.let {
                    decodeImage(it)
                }
            }
            else if (requestCode == RequestCodes.selectImageForSharingCode || requestCode == RequestCodes.selectImageForSavingCode) {
                // We can only share an image if a recipient with a public key has been selected
                thisFriend.publicKeyEncoded?.let {
                    // Get the message text
                    val message = message_edit_text.text.toString()
                    // get data?.data as URI
                    val imageURI = data?.data
                    imageURI?.let {
                        imageImportProgressBar.visibility = View.VISIBLE
                        shareOrSaveAsImage(
                            imageURI,
                            message,
                            thisFriend.publicKeyEncoded!!,
                            requestCode == RequestCodes.selectImageForSavingCode
                        )
                        message_edit_text.text?.clear()
                    }
                }
            }
        }
    }

    @ExperimentalUnsignedTypes
    private fun shareOrSaveAsImage(imageUri: Uri, message: String, encodedFriendPublicKey: ByteArray, saveImage: Boolean) {
        try {
            // Encrypt the message
            val encryptedMessage = Encryption().encrypt(encodedFriendPublicKey, message)
            makeWait()
            // Encode the image
            val newUri: Deferred<Uri?> =
                coroutineScope.async(Dispatchers.IO) {
                    val swatch = Encoder()
                    return@async swatch.encode(
                        applicationContext,
                        encryptedMessage,
                        imageUri,
                        saveImage
                    )
                }

            coroutineScope.launch(Dispatchers.Main) {
                val maybeUri = newUri.await()
                noMoreWaiting()
                imageImportProgressBar.visibility = View.INVISIBLE

                if (maybeUri != null)
                {
                    if (saveImage) {
                        showAlert(getString(R.string.alert_text_image_saved))
                    }
                    else {
                        ShareUtil.shareImage(applicationContext, maybeUri)
                    }
                    saveMessage(encryptedMessage, thisFriend, true)
                }
                else
                {
                    applicationContext.showAlert(applicationContext.getString(R.string.alert_text_unable_to_process_request))
                }
            }

        } catch (exception: SecurityException) {
            applicationContext.showAlert(applicationContext.getString(R.string.alert_text_unable_to_process_request))
            print("Unable to send message as photo, we were unable to encrypt the mess56age.")
            return
        }
    }

    @ExperimentalUnsignedTypes
    private fun decodeImage(imageUri: Uri) {
        makeWait()

        val decodeResult: Deferred<ByteArray?> =
            coroutineScope.async(Dispatchers.IO) {
                val swatch = Decoder()
                return@async swatch.decode(applicationContext, imageUri)
            }

        coroutineScope.launch(Dispatchers.Main) {
            try
            {
                val maybeDecodeResult = decodeResult.await()
                noMoreWaiting()

                if (maybeDecodeResult != null)
                {
                    decodePayload = maybeDecodeResult
                    handleImageDecodeResult()
                }
                else
                {
                    showAlert(getString(R.string.alert_text_unable_to_decode_message))
                }
            }
            catch (e: Exception)
            {
                noMoreWaiting()
                showAlert(getString(R.string.alert_text_unable_to_decode_message))
            }
        }
    }

    private fun handleImageDecodeResult() {
        if (decodePayload == null)
        {
            showAlert(getString(R.string.alert_text_unable_to_decode_message))
            return
        }
        saveMessage(decodePayload!!, thisFriend, false)
    }

    private fun saveMessage(cipherBytes: ByteArray, messageSender: Friend, fromMe: Boolean) {
        val newMessage = Message(cipherBytes, messageSender, fromMe)
        newMessage.save(this)

        // Add to messages
        setupViewByStatus()
    }

    private fun makeWait() {
        imageImportProgressBar.visibility = View.VISIBLE
        btn_import_image.isEnabled = false
        btn_import_image.isClickable = false
    }

    private fun noMoreWaiting() {
        imageImportProgressBar.visibility = View.INVISIBLE
        btn_import_image.isEnabled = true
        btn_import_image.isClickable = true
    }

    private fun decodeStringMessage(messageString: String) {
        // Update UI to reflect text being shared
        val decodeResult = Codex().decode(messageString)

        if (decodeResult != null)
        {
            when (decodeResult.type)
            {
                KeyOrMessage.EncryptedMessage ->
                {
                    // Create Message Instance
                    val newMessage = Message(decodeResult.payload, thisFriend, false)
                    newMessage.save(this)

                    // Add to messages
                    setupViewByStatus()
                }
                KeyOrMessage.Key ->
                {
                    updateKeyAndStatus(decodeResult.payload)
                }
            }
        }
        else
        {
            this.showAlert(getString(R.string.alert_text_unable_to_decode_message))
        }
    }

    private fun updateKeyAndStatus(keyData: ByteArray) {
        when (thisFriend.status)
        {
            FriendStatus.Default ->
            {
                Persist.updateFriend(
                    context = this,
                    friendToUpdate = thisFriend,
                    newStatus = FriendStatus.Requested,
                    encodedPublicKey = keyData
                )
                thisFriend.status = FriendStatus.Requested
                setupViewByStatus()
            }
            FriendStatus.Invited ->
            {
                Persist.updateFriend(
                    context = this,
                    friendToUpdate = thisFriend,
                    newStatus = FriendStatus.Approved,
                    encodedPublicKey = keyData
                )
                thisFriend.status = FriendStatus.Approved
                thisFriend.publicKeyEncoded = keyData
                setupViewByStatus()
            }
            else ->
                this.showAlert(getString(R.string.alert_text_unable_to_update_friend_status))
        }
    }

    fun approveVerifyFriend() {
        Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Verified,
            encodedPublicKey = thisFriend.publicKeyEncoded)
        thisFriend.status = FriendStatus.Verified
        setupViewByStatus()
    }

    fun declineVerifyFriend() {
        thisFriend.publicKeyEncoded = null
        Persist.updateFriend(this, thisFriend, newStatus = FriendStatus.Default)
        thisFriend.status = FriendStatus.Default
        setupViewByStatus()
    }

    private fun sendToLogin() {
        // If the status is not either NotRequired, or Logged in, request login
        this.showAlert(getString(R.string.alert_text_passcode_required_to_proceed))
        // Send user to the Login Activity
        val loginIntent = Intent(applicationContext, LogInActivity::class.java)
        startActivity(loginIntent)
        finish()
    }

    fun changeFriendsName(newName: String) {
        Persist.updateFriend(this, thisFriend, newName)
        thisFriend.name = newName
        tv_friend_name.text = thisFriend.name
        showAlert("New name saved")
    }

    fun changeFriendsPhone(newPhoneNumber: String) {
        Persist.updateFriendsPhone(this, thisFriend, newPhoneNumber)
        thisFriend.phone = newPhoneNumber
        showAlert("New phone number saved")
    }

    fun Activity.hideSoftKeyboard(editText: EditText) {
        (getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(editText.windowToken, 0)
        }
    }
}
