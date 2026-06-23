package org.nahoft.nahoft.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import org.nahoft.nahoft.ScreenLockReceiver
import timber.log.Timber

class UpdateService : Service()
{
    var receiver: BroadcastReceiver? = null

    override fun onCreate()
    {
        super.onCreate()
        // register receiver that handles screen on and screen off logic
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        receiver = ScreenLockReceiver()
        registerReceiver(receiver, filter)
    }

    override fun onDestroy()
    {
        unregisterReceiver(receiver)
        Timber.i("onDestroy Receiver Called")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}