package org.nahoft.nahoft

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.*
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.nahoft.activities.LoginStatus

class Nahoft: Application(), LifecycleObserver {

    // Set Logout Timer to 5 minutes.
    val logoutTimer = object: CountDownTimer(300000, 1000) {
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

    //TODO: Is this being called?
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {

        logoutTimer.start()
    }

}