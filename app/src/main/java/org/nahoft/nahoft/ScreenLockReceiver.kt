package org.nahoft.nahoft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.nahoft.models.LoginStatus
import org.nahoft.nahoft.services.UpdateService

class ScreenLockReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action.equals(Intent.ACTION_SCREEN_OFF)) {
            if (Persist.status == LoginStatus.LoggedIn) {
                Persist.status = LoginStatus.LoggedOut
                Persist.saveLoginStatus()
            } else if(Persist.status == LoginStatus.NotRequired) {
                return
            }
            context?.sendBroadcast(Intent().apply {
                action = LOGOUT_TIMER_VAL
            })
            context?.stopService(Intent(context, UpdateService::class.java))
        }
    }
}