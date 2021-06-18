package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_image)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(LOGOUT_TIMER_VAL)
        })

        sender = intent.getSerializableExtra(SENDER) as Friend?

        import_image_button.setOnClickListener {
            handleImageImport()
        }

        setupFriendDropdown()
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
        val friendsSpinner: Spinner = findViewById(R.id.message_sender_spinner)
        friendsSpinner.adapter = friendAdapter
        friendsSpinner.onItemSelectedListener = this
        friendsSpinner.setSelection(0, false)
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

                    makeWait()

                    val decodeResult: Deferred<ByteArray?> =
                        coroutineScope.async(Dispatchers.IO) {
                            val swatch = Decoder()
                            return@async swatch.decode(applicationContext, imageURI)
                    }

                    coroutineScope.launch(Dispatchers.Main) {
                        val maybeBytes = decodeResult.await()
                        noMoreWaiting()
                        if (maybeBytes != null)
                        {
                            handleImageDecodeResult(maybeBytes)
                        }
                        else
                        {
                            showAlert(getString(R.string.alert_text_unable_to_decode_message))
                        }

                    }
                }
            }
        }
    }

    private fun handleImageDecodeResult(messageBytes: ByteArray)
    {
        if (sender != null)
        {
            sender?.let {
                // Create Message Instance
                val newMessage = Message(messageBytes, it)
                newMessage.save(this)

                // Go to message view
                val messageArguments = MessageActivity.Arguments(message = newMessage)
                messageArguments.startActivity(this)

                // Clear out the message view
                import_message_text_view.text?.clear()
            }
        }
        else
        {
            showAlert(getString(R.string.alert_text_which_friend_sent_this_message))
        }
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
