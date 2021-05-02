package org.nahoft.codex

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

const val LOGOUT_TIMER_VAL = "Logout Timer Has Been Called"

class LogoutTimerBroadcastReceiver(
    private inline val onBroadcastReceived: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == LOGOUT_TIMER_VAL){
            onBroadcastReceived()
        }
    }
}