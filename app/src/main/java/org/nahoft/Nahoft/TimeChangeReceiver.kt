package org.nahoft.nahoft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimeChangeReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_DATE_CHANGED || intent?.action == Intent.ACTION_TIME_CHANGED) {
            val failedLoginAttempts = Persist.encryptedSharedPreferences.getInt(Persist.sharedPrefFailedLoginAttemptsKey, 0)
            if (failedLoginAttempts > 0) {
                Persist.saveLoginFailure(failedLoginAttempts)
            }
        }
    }
}