package org.nahoft.nahoft

import android.app.Application
import android.content.Intent
import android.os.CountDownTimer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import org.nahoft.nahoft.activities.EnterPasscodeActivity
import java.util.*

class Nahoft: Application(), LifecycleObserver {

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onEnterForeground() {
        //TODO: Check login state and act accordingly
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {

        val logoutTimer = object: CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // stub
            }

            override fun onFinish() {
                // TODO("Logout the user if they are logged in")
                val enterPasscodeIntent = Intent(this@Nahoft, EnterPasscodeActivity::class.java)
                startActivity(enterPasscodeIntent)
            }
        }

        logoutTimer.start()
    }

}