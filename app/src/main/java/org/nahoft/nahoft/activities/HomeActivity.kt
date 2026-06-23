package org.nahoft.nahoft.activities

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.text.*
import android.text.style.AlignmentSpan
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.nahoft.nahoft.utils.registerReceiverCompat
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.nahoft.Persist.Companion.friendsFilename
import org.nahoft.nahoft.Persist.Companion.messagesFilename
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.nahoft.databinding.ActivityHomeBinding
import org.nahoft.nahoft.adapters.FriendsRecyclerAdapter
import org.nahoft.nahoft.models.Friend
import org.nahoft.nahoft.models.FriendStatus
import org.nahoft.nahoft.models.LoginStatus
import org.nahoft.nahoft.models.slideNameAbout
import org.nahoft.nahoft.models.slideNameAboutAndFriends
import org.nahoft.nahoft.services.WSPRReceiveSessionService
import org.nahoft.nahoft.services.UpdateService
import org.nahoft.nahoft.viewmodels.FriendViewModel
import org.nahoft.nahoft.viewmodels.MessageViewModel
import org.nahoft.util.RequestCodes
import org.nahoft.util.applySecureFlag
import org.nahoft.util.showAlert
import java.io.File

class HomeActivity : AppCompatActivity()
{
    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            adapter.cleanup()
        }
    }

    private lateinit var binding: ActivityHomeBinding

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: FriendsRecyclerAdapter
    private lateinit var filteredFriendList: ArrayList<Friend>
    private var hasShareData: Boolean = false
    private var receivingFriendName: String? = null

    // Service binding for receiving indicator
    private var receiveService: WSPRReceiveSessionService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection
    {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?)
        {
            val localBinder = binder as WSPRReceiveSessionService.LocalBinder
            receiveService = localBinder.getService()
            serviceBound = true
            observeReceivingFriend()
        }

        override fun onServiceDisconnected(name: ComponentName?)
        {
            receiveService = null
            serviceBound = false
            adapter.setReceivingFriend(null)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("MissingSuperCall")
    override fun onBackPressed()
    {
        finishAffinity()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "NotifyDataSetChanged")
    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.headerSection.updatePadding(top = systemBars.top)
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }

        window.applySecureFlag()

        registerReceiverCompat(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        }, exported = false)

        if (Persist.accessIsAllowed())
        {
            // Logout Button
            if (status == LoginStatus.NotRequired)
            {
                binding.logoutButton.visibility = View.GONE
            }
            else
            {
                binding.logoutButton.visibility = View.VISIBLE
                startService(Intent(this, UpdateService::class.java))
            }

            setupOnClicks()
            setupFriends()
            loadSavedMessages()
            showFriendsList()
            receiveSharedMessages()
            setupHelpText()

            if (!Persist.loadBooleanKey(Persist.sharedPrefAlreadySeeTutorialKey))
            {
                val slideActivity = Intent(applicationContext, SlideActivity::class.java)
                startActivity(slideActivity)
                Persist.saveBooleanKey(Persist.sharedPrefAlreadySeeTutorialKey, true)
            }

            binding.searchFriends.doOnTextChanged { text, _, _, _ ->
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

    override fun onStart()
    {
        super.onStart()

        // Bind to receive service if running (for receiving indicator)
        val intent = Intent(this, WSPRReceiveSessionService::class.java)
        bindService(intent, serviceConnection, 0)
    }

    override fun onResume()
    {
        super.onResume()

        if (!Persist.accessIsAllowed())
        {
            finish()
            sendToLogin()
        }

        if (status == LoginStatus.NotRequired)
        {
            binding.logoutButton.visibility = View.GONE
        }
        else
        {
            binding.logoutButton.visibility = View.VISIBLE
        }
    }

    override fun onStop()
    {
        super.onStop()

        if (serviceBound)
        {
            unbindService(serviceConnection)
            serviceBound = false
            receiveService = null
        }
    }

    override fun onDestroy()
    {
        // Ensure service is unbound
        if (serviceBound)
        {
            try
            {
                unbindService(serviceConnection)
            }
            catch (_: Exception)
            {
                // Already unbound
            }
            serviceBound = false
        }

        try
        {
            unregisterReceiver(receiver)
        }
        catch (_: Exception)
        {
            //Nothing to unregister
        }

        super.onDestroy()
    }

    override fun onRestart()
    {
        super.onRestart()
        loadSavedFriends()
        updateFriendListAdapter()
    }

    private fun observeReceivingFriend()
    {
        lifecycleScope.launch {
            receiveService?.currentFriendName?.collect { name ->
                receivingFriendName = name
                adapter.setReceivingFriend(name)
            }
        }
    }

    private fun updateFriendListAdapter()
    {
        filteredFriendList.clear()
        filteredFriendList.addAll(Persist.friendList.filter { f -> f.name.contains(binding.searchFriends.text, true) } as ArrayList<Friend>)

        @Suppress("NotifyDataSetChanged")
        adapter.notifyDataSetChanged()
        setupHelpText()
    }

    private fun showFriendsList()
    {
        linearLayoutManager = LinearLayoutManager(this)
        filteredFriendList = ArrayList(Persist.friendList)
        adapter = FriendsRecyclerAdapter(filteredFriendList)
        binding.friendsRecyclerView.layoutManager = linearLayoutManager
        binding.friendsRecyclerView.adapter = adapter

        adapter.onItemClick = { friend ->
            showInfoActivity(friend)
        }

        adapter.onItemLongClick = { friend ->
            showDeleteConfirmationDialog(friend)
        }
    }

    private fun setupHelpText()
    {
        binding.helpTextview.isVisible = Persist.friendList.isEmpty()// && !isAddButtonShow
        binding.helpImageview.isVisible = Persist.friendList.isEmpty()// && !isAddButtonShow
    }

    private fun showInfoActivity(friend: Friend?)
    {
        if (friend != null)
        {
            val friendInfoIntent = Intent(applicationContext, FriendInfoActivity::class.java)
            friendInfoIntent.putExtra(RequestCodes.friendExtraTaskDescription, friend)

            if (hasShareData)
            {
                // We received a shared message but the user is not logged in
                // Save the intent
                if (intent?.action == Intent.ACTION_SEND)
                {
                    if (intent.type == "text/plain")
                    {
                        val messageString = intent.getStringExtra(Intent.EXTRA_TEXT)
                        friendInfoIntent.putExtra(Intent.EXTRA_TEXT, messageString)
                    }
                    else if (intent.type?.startsWith("image/") == true)
                    {
                        val extraStream = intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java)
                        if (extraStream != null)
                        {
                            try
                            {
                                val extraUri = extraStream as? Uri

                                if (extraUri != null)
                                {
                                    friendInfoIntent.putExtra(Intent.EXTRA_STREAM, extraUri)
                                }
                            }
                            catch (_: Exception)
                            {
                                showAlert(getString(R.string.alert_text_unable_to_process_request))
                            }
                        }
                        else println("Extra Stream is Null")
                    }
                    else showAlert(getString(R.string.alert_text_unable_to_process_request))
                }
                else
                {
                    val extraString = intent.getStringExtra(Intent.EXTRA_TEXT)
                    val extraStream = intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java)

                    when {
                        extraString != null // Check to see if we received a string message share
                        -> {
                            try {
                                // Received string message
                                friendInfoIntent.putExtra(Intent.EXTRA_TEXT, extraString)
                            } catch (_: Exception) {
                                // Something went wrong, don't share this extra
                            }

                        }
                        extraStream != null // See if we received an image message share
                        -> {
                            try {
                                friendInfoIntent.putExtra(Intent.EXTRA_STREAM, extraStream)
                            } catch (_: NullPointerException) {
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

    private fun sendToLogin()
    {
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
                val extraStream = intent.getStringExtra(Intent.EXTRA_STREAM)
                if (extraStream != null)
                {
                    try
                    {
                        val extraUri = extraStream.toUri()
                        loginIntent.putExtra(Intent.EXTRA_STREAM, extraUri)
                    }
                    catch (_: Exception)
                    {
                        // The string was not a url don't try to share it
                    }
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

    private fun loadSavedFriends()
    {
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

    private fun setupFriends()
    {
        Persist.friendsFile = File(filesDir.absolutePath + File.separator + friendsFilename )
        loadSavedFriends()
    }

    private fun loadSavedMessages()
    {
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

    private fun logoutButtonClicked()
    {
        status = LoginStatus.LoggedOut
        Persist.saveLoginStatus()

        val returnToLoginIntent = Intent(this, LogInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        startActivity(returnToLoginIntent)

        finish()
    }

    private fun setupOnClicks()
    {
        binding.logoutButton.setOnClickListener {
            logoutButtonClicked()
        }

        binding.addFriendButton.setOnClickListener {
            showAddFriendDialog()
        }

        binding.userGuideButton.setOnClickListener {
            val slideActivity = Intent(this, SlideActivity::class.java)
            slideActivity.putExtra(Intent.EXTRA_TEXT, slideNameAboutAndFriends)
            startActivity(slideActivity)
        }

        binding.nahoftMessageBottle.setOnClickListener {
            val slideActivity = Intent(this, SlideActivity::class.java)
            slideActivity.putExtra(Intent.EXTRA_TEXT, slideNameAbout)
            startActivity(slideActivity)
        }

        binding.settingsButton.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }
    }

    private fun showAddFriendDialog()
    {
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
        alertDialogContent.orientation = LinearLayout.VERTICAL

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
        builder.setView(alertDialogContent)

        // Set the Add and Cancel Buttons
        builder.setPositiveButton(resources.getString(R.string.add_button))
        {
                _, _->
            if (inputEditText.text.isNotEmpty())
            {
                val friendName = inputEditText.text.toString()
                val newFriend = saveFriend(friendName)//, phoneNumber)
                if (newFriend != null) {
                    val friendInfoActivityIntent = Intent(this, FriendInfoActivity::class.java)
                    friendInfoActivityIntent.putExtra(RequestCodes.friendExtraTaskDescription, newFriend)
                    startActivity(friendInfoActivityIntent)
                }
            }
        }

        builder.setNeutralButton(resources.getString(R.string.stop_button)) {
                dialog, _->
            dialog.cancel()
        }
            .create()
            .show()
    }

    private fun saveFriend(friendName: String) : Friend? { //, phoneNumber: String
        val newFriend = Friend(friendName, FriendStatus.Default, null) //phoneNumber,

        // Only add the friend if one with the same name doesn't already exist.
        if (Persist.friendList.any {friend -> friend.name == friendName })
        {
            showAlert(getString(R.string.alert_text_friend_already_exists))
            return null
        }
        else
        {
            val mng = applicationContext.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (!mng.isDeviceSecure) {
                showAlert(getString(R.string.you_have_to_set_a_lock_screen))
                return null
            }
            Persist.friendList.add(newFriend)
            Persist.saveFriendsToFile(this)
            filteredFriendList.add(newFriend)

            @Suppress("NotifyDataSetChanged")
            adapter.notifyDataSetChanged()
            setupHelpText()
            return newFriend
        }
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun showDeleteConfirmationDialog(friend: Friend)
    {
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

    private fun deleteFriend(friend: Friend)
    {
        Persist.friendList.remove(friend)
        Persist.saveFriendsToFile(this)
        updateFriendListAdapter()
    }

    @ExperimentalUnsignedTypes
    private fun receiveSharedMessages()
    {
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

        if (hasShareData) shareViewSetup()
        else normalViewSetup()
    }

    private fun shareViewSetup()
    {
        binding.settingsButton.isVisible = false
        binding.userGuideButton.isVisible = false
        binding.tvMessages.text = getString(R.string.select_sender)
        binding.tvMessages.isAllCaps = false
    }

    private fun normalViewSetup()
    {
        binding.settingsButton.isVisible = true
        binding.userGuideButton.isVisible = true
        binding.tvMessages.text = getString(R.string.friends_list)
        binding.tvMessages.isAllCaps = true
    }
}
