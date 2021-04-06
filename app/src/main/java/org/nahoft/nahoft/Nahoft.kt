package org.nahoft.nahoft

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.nahoft.activities.LoginStatus

class Nahoft: Application(), LifecycleObserver {

    val logoutTimer = object: CountDownTimer(15000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            // stub
        }

        //TODO: Work with Brandon this is not being called.
        override fun onFinish() {
            // Logout the user if they are logged in
            if (status == LoginStatus.LoggedIn) {
                status = LoginStatus.LoggedOut
                Persist.saveLoginStatus()
            } else if(status == LoginStatus.NotRequired) {
                return
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {

        logoutTimer.start()
    }

}