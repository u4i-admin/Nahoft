package org.nahoft.nahoft

import android.app.Application
import android.content.Intent
import android.os.CountDownTimer
import androidx.lifecycle.*
import org.nahoft.codex.LOGOUT_TIMER_VAL
import org.nahoft.nahoft.Persist.Companion.status
import timber.log.Timber

class Nahoft: Application(), LifecycleObserver
{
    // Set Logout Timer to 5 minutes.
    private val logoutTimer = object: CountDownTimer(300000, 1000) {
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