package org.nahoft.nahoft

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class ApplicationShortcutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_shortcut)

        if (Build.VERSION.SDK_INT >= 25) {
            Shortcut.setUp(applicationContext)
        }
    }
}