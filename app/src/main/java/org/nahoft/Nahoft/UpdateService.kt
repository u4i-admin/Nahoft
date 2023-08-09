package org.nahoft.nahoft

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log


class UpdateService : Service() {
    var receiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        // register receiver that handles screen on and screen off logic
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        receiver = ScreenLockReceiver()
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        Log.i("onDestroy Receiver", "Called")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}