package org.nahoft.nahoft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimeChangeReceiver: BroadcastReceiver()
{
    override fun onReceive(context: Context?, intent: Intent?)
    {
        // Only respond to actual time change intents
        if (intent?.action != Intent.ACTION_TIME_CHANGED &&
            intent?.action != Intent.ACTION_DATE_CHANGED) {
            return
        }

        val failedLoginAttempts = Persist.encryptedSharedPreferences
            .getInt(Persist.sharedPrefFailedLoginAttemptsKey, 0)

        // No active lockout
        if (failedLoginAttempts < 6) return

        // Check for forward clock manipulation
        // This returns true only if wall clock says expired but real time disagrees
        if (Persist.isClockManipulationDetected(failedLoginAttempts))
        {
            // User moved clock forward - reset lockout from current time
            Persist.saveLoginFailure(failedLoginAttempts)
        }
        // Otherwise: do nothing
    }
}