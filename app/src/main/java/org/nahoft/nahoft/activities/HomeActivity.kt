package org.nahoft.nahoft.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.*
import android.text.style.AlignmentSpan
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.setPadding
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_home.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.nahoft.Persist.Companion.friendsFilename
import org.nahoft.nahoft.Persist.Companion.messagesFilename
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.util.RequestCodes
import org.nahoft.util.showAlert
import java.io.File

class HomeActivity : AppCompatActivity()
{
    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            adapter.cleanup()
        }
    }

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: FriendsRecyclerAdapter
    private lateinit var filteredFriendList: ArrayList<Friend>
    private var hasShareData: Boolean = false
    private var isAddButtonShow: Boolean = false

    override fun onBackPressed() {
        finishAffinity()
    }

    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        // Prepare persisted data
        Persist.app = Nahoft()
        Persist.loadEncryptedSharedPreferences(this.applicationContext)

        if (Persist.accessIsAllowed())
        {
            // Logout Button
            if (status == LoginStatus.NotRequired) {
                logout_button.visibility = View.GONE
            } else {
                logout_button.visibility = View.VISIBLE
            }

            setupOnClicks()
            setupFriends()
            loadSavedMessages()
            showFriendsList()
            receiveSharedMessages()

            if (!Persist.loadBooleanKey(Persist.sharedPrefAlreadySeeTutorialKey)) {
                val slideActivity = Intent(applicationContext, SlideActivity::class.java)
                startActivity(slideActivity)
                Persist.saveBooleanKey(Persist.sharedPrefAlreadySeeTutorialKey, true)
            }

            if (Persist.loadBooleanKey(Persist.sharedPrefUseSmsAsDefaultKey)) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS), RequestCodes.requestPermissionCode)
                }
            }

            search_friends.doOnTextChanged { text, _, _, _ ->
                filteredFriendList.clear()
                filteredFriendList.addAll(Persist.friendList.filter { f -> f.name.contains(text.toString(), true) } as ArrayList<Friend>)
                adapter.notifyDataSetChanged()
            }
        }
        else
        {
            sendToLogin()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!Persist.accessIsAllowed())
        {
            finish()
            sendToLogin()
        }

        if (status == LoginStatus.NotRequired)
        {
            logout_button.visibility = View.GONE
        }
        else
        {
            logout_button.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        try
        {
            unregisterReceiver(receiver)
        }
        catch (e: Exception)
        {
            //Nothing to unregister
        }

        super.onDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        filteredFriendList.clear()
        filteredFriendList.addAll(Persist.friendList.filter { f -> f.name.contains(search_friends.text, true) } as ArrayList<Friend>)
        adapter.notifyDataSetChanged()
    }

    private fun showFriendsList() {
        linearLayoutManager = LinearLayoutManager(this)
        filteredFriendList = Persist.friendList.clone() as ArrayList<Friend>
        adapter = FriendsRecyclerAdapter(filteredFriendList)
        friendsRecyclerView.layoutManager = linearLayoutManager
        friendsRecyclerView.adapter = adapter

        adapter.onItemClick = { friend ->
            showInfoActivity(friend)
        }

        adapter.onItemLongClick = { friend ->
            showDeleteConfirmationDialog(friend)
        }
    }

    private fun showInfoActivity(friend: Friend?) {
        if (friend != null) {
            val friendInfoIntent = Intent(applicationContext, FriendInfoActivity::class.java)
            friendInfoIntent.putExtra(RequestCodes.friendExtraTaskDescription, friend)

            if (hasShareData) {
                // We received a shared message but the user is not logged in
                // Save the intent
                if (intent?.action == Intent.ACTION_SEND) {
                    if (intent.type == "text/plain") {
                        val messageString = intent.getStringExtra(Intent.EXTRA_TEXT)
                        friendInfoIntent.putExtra(Intent.EXTRA_TEXT, messageString)
                    } else if (intent.type?.startsWith("image/") == true) {
                        val extraStream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                        if (extraStream != null) {
                            try {
                                val extraUri = extraStream as? Uri

                                if (extraUri != null) {
                                    friendInfoIntent.putExtra(Intent.EXTRA_STREAM, extraUri)
                                }
                            } catch (e: Exception) {
                                showAlert(getString(R.string.alert_text_unable_to_process_request))
                            }
                        } else {
                            println("Extra Stream is Null")
                        }
                    } else {
                        showAlert(getString(R.string.alert_text_unable_to_process_request))
                    }
                } else {
                    val extraString = intent.getStringExtra(Intent.EXTRA_TEXT)
                    val extraStream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)

                    when {
                        extraString != null // Check to see if we received a string message share
                        -> {
                            try {
                                // Received string message
                                friendInfoIntent.putExtra(Intent.EXTRA_TEXT, extraString)
                            } catch (e: Exception) {
                                // Something went wrong, don't share this extra
                            }

                        }
                        extraStream != null // See if we received an image message share
                        -> {
                            try {
                                friendInfoIntent.putExtra(Intent.EXTRA_STREAM, extraStream)
                            } catch (e: NullPointerException) {
                                // Something went wrong, don't share this extra
                            }
                        }
                    }
                }
                hasShareData = false
                normalViewSetup()
            }

            startActivity(friendInfoIntent)
        }
    }

    private fun sendToLogin() {
        // If the status is not either NotRequired, or Logged in, request login
        this.showAlert(getString(R.string.alert_text_passcode_required_to_proceed))

        // Send user to the EnterPasscode Activity
        val loginIntent = Intent(applicationContext, LogInActivity::class.java)

        // We received a shared message but the user is not logged in
        // Save the intent
        if (intent?.action == Intent.ACTION_SEND)
        {
            if (intent.type == "text/plain")
            {
                val messageString = intent.getStringExtra(Intent.EXTRA_TEXT)
                loginIntent.putExtra(Intent.EXTRA_TEXT, messageString)
            }
            else if (intent.type?.startsWith("image/") == true)
            {
                val extraStream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                if (extraStream != null)
                {
                    try
                    {
                        val extraUri = extraStream as? Uri

                        if (extraUri != null)
                        {
                            loginIntent.putExtra(Intent.EXTRA_STREAM, extraUri)
                        }
                    }
                    catch (e: Exception)
                    {
                        showAlert(getString(R.string.alert_text_unable_to_process_request))
                    }
                }
                else
                {
                    println("Extra Stream is Null")
                }
            }
            else
            {
                showAlert(getString(R.string.alert_text_unable_to_process_request))
            }
        }

        startActivity(loginIntent)
        finish()
    }

    private fun loadSavedFriends() {
        // Load our existing friends list from our encrypted file
        if (Persist.friendsFile.exists())
        {
            val friendsToAdd = FriendViewModel.getFriends(Persist.friendsFile, applicationContext)

            for (newFriend in friendsToAdd)
            {
                // Only add this friend if the list does not contain a friend with that ID already
                if (!Persist.friendList.any { it.name == newFriend.name })
                {
                    Persist.friendList.add(newFriend)
                }
            }
        }
    }

    private fun setupFriends() {
        Persist.friendsFile = File(filesDir.absolutePath + File.separator + friendsFilename )
        loadSavedFriends()
    }

    private fun loadSavedMessages() {
        Persist.messagesFile = File(filesDir.absolutePath + File.separator + messagesFilename )

        // Load messages from file
        if (Persist.messagesFile.exists())
        {
            val messagesToAdd = MessageViewModel().getMessages(Persist.messagesFile, applicationContext)
            messagesToAdd?.let {
                Persist.messageList.clear()
                Persist.messageList.addAll(it)
            }
        }
    }

    private fun logoutButtonClicked() {
        status = LoginStatus.LoggedOut
        Persist.saveLoginStatus()

        val returnToLoginIntent = Intent(this, LogInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        startActivity(returnToLoginIntent)

        finish()
    }

    private fun setupOnClicks() {
        logout_button.setOnClickListener {
            logoutButtonClicked()
        }

        add_friend_button.setOnClickListener {
            showHideAddButtons()
        }

        add_friend_manually_button.setOnClickListener {
            showAddFriendDialog()
            add_friend_button.isFocusable = true
            add_friend_button.isFocusableInTouchMode = true
            add_friend_button.requestFocus()
            showHideAddButtons()
        }

        add_friend_contact_button.setOnClickListener {
            val contactActivity = Intent(this, ContactListActivity::class.java)
            startActivity(contactActivity)
            showHideAddButtons()
        }

        user_guide_button.setOnClickListener {
            val slideActivity = Intent(this, SlideActivity::class.java)
            slideActivity.putExtra(Intent.EXTRA_TEXT, slideNameAboutAndFriends)
            startActivity(slideActivity)
        }

        settings_button.setOnClickListener {
            val settingsIntent = Intent(this, SettingPasscodeActivity::class.java)
            startActivity(settingsIntent)
        }
    }

    private fun showHideAddButtons() {
        add_friend_manually_button.animate().apply {
            duration = 500
            translationY(if (isAddButtonShow) 0F else -150F)
        }
        add_friend_contact_button.animate().apply {
            duration = 500
            translationY(if (isAddButtonShow) 0F else -275F)
        }
        isAddButtonShow = !isAddButtonShow
    }

    private fun showAddFriendDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_AddFriendAlertDialog))
        val title = SpannableString(getString(R.string.add_new_friend))

        // alert dialog title align center
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)

        val alertDialogContent = LinearLayout(this)
        alertDialogContent.setOrientation(LinearLayout.VERTICAL)

        // Set the input - EditText
        val inputEditText = EditText(this)
        inputEditText.setBackgroundResource(R.drawable.btn_bkgd_light_grey_outline_8)
        inputEditText.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        inputEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        inputEditText.isSingleLine = true
        inputEditText.hint = getString(R.string.nickname)
        inputEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_nahoft_icons_business_card, 0, 0, 0)
        inputEditText.compoundDrawablePadding = 10
        inputEditText.setPadding(25)
        inputEditText.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        alertDialogContent.addView(inputEditText)

        // Set the phone - EditText
        val phoneEditText = EditText(this)
        phoneEditText.setBackgroundResource(R.drawable.btn_bkgd_light_grey_outline_8)
        phoneEditText.inputType = InputType.TYPE_CLASS_PHONE
        phoneEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        phoneEditText.isSingleLine = true
        phoneEditText.hint = getString(R.string.phone_number_optional)
        phoneEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_nahoft_icons_phone, 0, 0, 0)
        phoneEditText.compoundDrawablePadding = 10
        phoneEditText.setPadding(25)
        phoneEditText.setTextColor(ContextCompat.getColor(this, R.color.royalBlueDark))
        alertDialogContent.addView(phoneEditText)

        builder.setView(alertDialogContent)

        val param = phoneEditText.layoutParams as ViewGroup.MarginLayoutParams
        param.setMargins(0,20,0,0)
        phoneEditText.layoutParams = param

        // Set the Add and Cancel Buttons
        builder.setPositiveButton(resources.getString(R.string.add_button))
        {
                dialog, _->
            if (!inputEditText.text.isEmpty())
            {
                val friendName = inputEditText.text.toString()
                val phoneNumber = phoneEditText.text.toString()
                val newFriend = saveFriend(friendName, phoneNumber)
                if (newFriend != null) {
                    val friendInfoActivityIntent = Intent(this, FriendInfoActivity::class.java)
                    friendInfoActivityIntent.putExtra(RequestCodes.friendExtraTaskDescription, newFriend)
                    startActivity(friendInfoActivityIntent)
                }
            }
        }

        builder.setNeutralButton(resources.getString(R.string.cancel_button)) {
                dialog, _->
            dialog.cancel()
        }
            .create()
            .show()
    }

    private fun saveFriend(friendName: String, phoneNumber: String) : Friend? {
        val newFriend = Friend(friendName, phoneNumber, FriendStatus.Default, null)

        // Only add the friend if one with the same name doesn't already exist.
        if (Persist.friendList.any {friend -> friend.name == friendName })
        {
            showAlert(getString(R.string.alert_text_friend_already_exists))
            return null
        }
        else if (Persist.friendList.any { friend -> friend.phone == phoneNumber}) {
            showAlert(getString(R.string.alert_text_friend_phone_already_exists))
            return null
        }
        else
        {
            Persist.friendList.add(newFriend)
            Persist.saveFriendsToFile(this)
            filteredFriendList.add(newFriend)
            adapter.notifyDataSetChanged()
            return newFriend
        }
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun showDeleteConfirmationDialog(friend: Friend) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_DeleteAlertDialog))

        val title = SpannableString(getString(R.string.alert_text_confirm_friend_delete, friend.name))
        // alert dialog title align center
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)
        builder.setPositiveButton(resources.getString(R.string.button_label_delete))
        { _, _->
            //delete friend
            deleteFriend(friend)
        }

        builder.setNeutralButton(resources.getString(R.string.button_label_cancel))
        { _, _ ->
            //cancel
        }

        builder.create()
        builder.show()
    }

    private fun deleteFriend(friend: Friend) {
        Persist.friendList.remove(friend)
        Persist.saveFriendsToFile(this)
        filteredFriendList.clear()
        filteredFriendList.addAll(Persist.friendList)
        adapter.notifyDataSetChanged()
    }

    @ExperimentalUnsignedTypes
    private fun receiveSharedMessages() {
        // Receive shared messages
        if (intent?.action == Intent.ACTION_SEND)
        {
            showAlert(getString(R.string.alert_text_which_friend_sent_this_message))
            hasShareData = true
        }
        else // See if we got intent extras from the Login Activity
        {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let{
                showAlert(getString(R.string.alert_text_which_friend_sent_this_message))
                hasShareData = true
            }
            intent.getStringExtra(Intent.EXTRA_STREAM)?.let {
                showAlert(getString(R.string.alert_text_which_friend_sent_this_message))
                hasShareData = true
            }
        }
        if (hasShareData) {
            shareViewSetup()
        } else {
            normalViewSetup()
        }
    }

    private fun shareViewSetup() {
        settings_button.isVisible = false
        user_guide_button.isVisible = false
        tv_messages.text = getString(R.string.select_sender)
        tv_messages.isAllCaps = false
    }

    private fun normalViewSetup() {
        settings_button.isVisible = true
        user_guide_button.isVisible = true
        tv_messages.text = getString(R.string.messages)
        tv_messages.isAllCaps = true
    }
}
