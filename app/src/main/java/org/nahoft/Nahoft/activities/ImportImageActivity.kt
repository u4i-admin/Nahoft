package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_import_image.*
import kotlinx.android.synthetic.main.activity_import_image.imageImportProgressBar
import kotlinx.android.synthetic.main.activity_import_image.import_image_button
import kotlinx.android.synthetic.main.activity_import_text.*
import kotlinx.coroutines.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.codex.LogoutTimerBroadcastReceiver
import org.nahoft.nahoft.*
import org.nahoft.org.nahoft.swatch.Decoder
import org.nahoft.util.RequestCodes
import org.nahoft.util.showAlert

class ImportImageActivity: AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var decodePayload: ByteArray? = null
    private var sender: Friend? = null
    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    private val receiver by lazy {
        LogoutTimerBroadcastReceiver {
            cleanUp()
        }
    }

    companion object {
        const val SENDER = "Sender"
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_image)

        makeSureAccessIsAllowed()

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        // Check to see if a friend was slected in a previous activity
        val maybeFriend = intent.getSerializableExtra(RequestCodes.friendExtraTaskDescription) as? Friend
        if (maybeFriend != null)
        {
            sender = maybeFriend
        }

        import_image_button.setOnClickListener {
            handleImageImport()
        }

        setupFriendDropdown()
        receiveSharedMessages()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (position != 0) // The first value is a placeholder
        {
            val maybeFriend: Friend = parent?.selectedItem as Friend
            maybeFriend.let { userTappedFriend: Friend ->
                sender = userTappedFriend
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        //Stub
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun setupFriendDropdown()
    {
        val verifiedFriends = Friends().verifiedSpinnerList()
        val friendAdapter: ArrayAdapter<Friend> = ArrayAdapter(this, R.layout.spinner, verifiedFriends)
        val friendSpinner: Spinner = findViewById(R.id.message_sender_spinner)
        friendSpinner.adapter = friendAdapter
        friendSpinner.onItemSelectedListener = this

        // If sender != null select the correct friend in the spinner
        if (sender != null)
        {
            friendSpinner.setSelection(friendAdapter.getPosition(sender))
        }
        else
        {
            friendSpinner.setSelection(0, false)
        }
    }

    private fun receiveSharedMessages()
    {
        // Receive shared messages
        if (intent?.action == Intent.ACTION_SEND)
        {
            if (intent.type?.startsWith("image/") == true)
            {
                val extraStream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                if (extraStream != null)
                {
                    (extraStream as? Uri)?.let {
                        decodeImage(it)
                    }
                }
            }
            else
            {
                showAlert(getString(R.string.alert_text_unable_to_process_request))
            }
        }
        else // See if we got intent extras from the EnterPasscode Activity
        {
            // See if we received an image message
            val extraStream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
            if (extraStream != null)
            {
                val extraUri = Uri.parse(extraStream.toString())
                decodeImage(extraUri)
            }
        }
    }

    @ExperimentalUnsignedTypes
    private fun decodeImage(imageUri: Uri)
    {
        makeWait()

        val decodeResult: Deferred<ByteArray?> =
            coroutineScope.async(Dispatchers.IO) {
                val swatch = Decoder()
                return@async swatch.decode(applicationContext, imageUri)
            }

        coroutineScope.launch(Dispatchers.Main) {
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
    }

    private fun handleImageImport()
    {
        if (sender == null)
        {
            showAlert(getString(R.string.alert_text_which_friend_sent_this_message))
        }
        else
        {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, RequestCodes.selectImageForSharingCode)
        }
    }

    @ExperimentalUnsignedTypes
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == RequestCodes.selectImageForSharingCode)
            {
                // get data?.data as URI
                val imageURI = data?.data
                imageURI?.let {
                    decodeImage(it)
                }
            }
        }
    }

    private fun handleImageDecodeResult()
    {
        if (sender != null)
        {
            sender?.let {

                if (decodePayload != null)
                {
                    decodePayload?.let { decodedBytes ->
                        // Create Message Instance
                        val newMessage = Message(decodedBytes, it)
                        newMessage.save(this)

                        // Go to message view
                        val messageArguments = MessageActivity.Arguments(message = newMessage)
                        messageArguments.startActivity(this)

                        // Clear out the message view I removed import_message_text_view.text?.clear() I think it's
                        // making the app crash because there is no longer this view for this activity.
                        // import_message_text_view.text?.clear()
                    }
                }
                else
                {
                    showAlert(getString(R.string.alert_text_unable_to_decode_message))
                }
            }
        }
        else
        {
            showAlert(getString(R.string.alert_text_which_friend_sent_this_message))
        }
    }

    private fun makeSureAccessIsAllowed()
    {
        Persist.getStatus()

        if (Persist.status == LoginStatus.NotRequired || Persist.status == LoginStatus.LoggedIn)
        {
            return
        }
        else
        {
            sendToLogin()
        }
    }

    private fun sendToLogin()
    {
        // If the status is not either NotRequired, or Logged in, request login
        this.showAlert(getString(R.string.alert_text_passcode_required_to_proceed))

        // Send user to the EnterPasscode Activity
        val loginIntent = Intent(applicationContext, EnterPasscodeActivity::class.java)

        // We received a shared message but the user is not logged in
        // Save the intent
        if (intent?.action == Intent.ACTION_SEND)
        {
            if (intent.type?.startsWith("image/") == true)
            {
                val extraStream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                if (extraStream != null){
                    val extraUri = Uri.parse(extraStream.toString())
                    loginIntent.putExtra(Intent.EXTRA_STREAM, extraUri)
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
    }

    private fun makeWait()
    {
        imageImportProgressBar.visibility = View.VISIBLE
        import_image_button.isEnabled = false
        import_image_button.isClickable = false
    }

    private fun noMoreWaiting()
    {
        imageImportProgressBar.visibility = View.INVISIBLE
        import_image_button.isEnabled = true
        import_image_button.isClickable = true
    }

    private fun cleanUp () {
        decodePayload = null
        sender = null
    }
}
