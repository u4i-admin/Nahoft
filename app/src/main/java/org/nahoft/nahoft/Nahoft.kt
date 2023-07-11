package org.nahoft.nahoft

import android.app.Application
import android.content.Intent
import android.os.CountDownTimer
import androidx.lifecycle.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.nahoft.LoginStatus

class Nahoft: Application(), LifecycleObserver {

    // Set Logout Timer to 3 minutes.
    private val logoutTimer = object: CountDownTimer(180000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            // stub
        }

        override fun onFinish() {
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
        }
    }

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
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