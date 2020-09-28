package org.nahoft.nahoft

import android.app.Application
import android.content.Intent
import android.os.CountDownTimer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import org.nahoft.nahoft.Persist.Companion.status
import org.nahoft.nahoft.activities.EnterPasscodeActivity
import org.nahoft.nahoft.activities.LoginStatus
import java.util.*

class Nahoft: Application(), LifecycleObserver {

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    fun onEnterForeground() {
//        //TODO: Check login state and act accordingly
//        // TODO Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag.
//        // Return user to the EnterPasscodeActivity
//        val enterPasscodeIntent = Intent(this@Nahoft, EnterPasscodeActivity::class.java)
//        startActivity(enterPasscodeIntent)
//    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {

        val logoutTimer = object: CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // stub
            }

            //TODO: Work with Brandon this is not being called.
            override fun onFinish() {
                // Logout the user if they are logged in

                if (status == LoginStatus.LoggedIn) {
                    status = LoginStatus.LoggedOut
                    Persist().saveStatus()
                } else if(status == LoginStatus.NotRequired) {
                    return
                }
            }
        }

        logoutTimer.start()
    }

}