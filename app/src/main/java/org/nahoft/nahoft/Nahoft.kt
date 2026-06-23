package org.nahoft.nahoft

import android.app.Application
import android.content.Intent
import android.os.CountDownTimer
import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.nahoft.models.LoginStatus
import org.nahoft.nahoft.services.UpdateService
import org.nahoft.nahoft.viewmodels.FriendViewModel
import org.nahoft.nahoft.viewmodels.MessageViewModel
import org.operatorfoundation.transmission.SerialConnectionFactory
import org.operatorfoundation.transmission.SerialDeviceMonitor
import timber.log.Timber
import java.io.File

class Nahoft: Application(), LifecycleObserver
{
    lateinit var serialDeviceMonitor: SerialDeviceMonitor
    val eden = MutableStateFlow<Eden?>(null)

    // Set Logout Timer to 5 minutes.
    private val logoutTimer = object: CountDownTimer(300000, 1000) {

        override fun onTick(millisUntilFinished: Long)
        {
            // stub
        }

        override fun onFinish()
        {
            // Logout the user if they are logged in
            if (status == LoginStatus.LoggedIn) {
                status = LoginStatus.LoggedOut
                Persist.saveLoginStatus()
            } else if(status == LoginStatus.NotRequired) {
                return
            }
            sendBroadcast(Intent().apply {
                action = LOGOUT_TIMER_VAL
            })
            stopService(Intent(applicationContext, UpdateService::class.java))
        }
    }

    override fun onCreate()
    {
        super.onCreate()

        if (BuildConfig.DEBUG)
        {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Persist early so data is available to services
        initializePersist()

        serialDeviceMonitor = SerialDeviceMonitor(applicationContext)

        // Create/destroy Eden instance as serial connection comes and goes
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            serialDeviceMonitor.connectionState.collect { state ->
                when (state)
                {
                    is SerialConnectionFactory.ConnectionState.Connected -> {
                        eden.value?.close()
                        eden.value = Eden(state.connection)
                        Timber.d("Eden instance created")
                    }
                    is SerialConnectionFactory.ConnectionState.Disconnected,
                    is SerialConnectionFactory.ConnectionState.DeviceAvailable,
                    is SerialConnectionFactory.ConnectionState.Error -> {
                        eden.value?.close()
                        eden.value = null
                        Timber.d("Eden instance released")
                    }
                    else -> { /* Connecting / RequestingPermission — no action */ }
                }
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun initializePersist()
    {
        Persist.app = this
        Persist.loadEncryptedSharedPreferences(applicationContext)

        // Initialize file references
        Persist.friendsFile = File(filesDir.absolutePath + File.separator + Persist.friendsFilename)
        Persist.messagesFile = File(filesDir.absolutePath + File.separator + Persist.messagesFilename)

        // Load friends if file exists
        if (Persist.friendsFile.exists())
        {
            val friendsToAdd = FriendViewModel.getFriends(Persist.friendsFile, applicationContext)
            Persist.friendList.clear()
            Persist.friendList.addAll(friendsToAdd)
        }

        // Load messages if file exists
        if (Persist.messagesFile.exists())
        {
            val messagesToAdd = MessageViewModel().getMessages(Persist.messagesFile, applicationContext)
            messagesToAdd?.let {
                Persist.messageList.clear()
                Persist.messageList.addAll(it)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onEnterForeground() {

        logoutTimer.cancel()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {

        logoutTimer.start()
    }
}