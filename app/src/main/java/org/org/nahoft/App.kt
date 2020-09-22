package org.org.nahoft

import android.app.Application
import android.content.Context

class App: Application() {
    companion object {
        var application: Application? = null

        fun getContext(): Context? {
            return application?.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        application = this;
    }
}
