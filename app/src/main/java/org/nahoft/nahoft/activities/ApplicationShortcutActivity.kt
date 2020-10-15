package org.nahoft.nahoft

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_application_shortcut.*
import org.nahoft.nahoft.R.layout.activity_application_shortcut

class ApplicationShortcutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_shortcut)

     /*getPackageManager().setComponentEnabledSetting(
         ComponentName("org.nahoft.nahoft", "org.nahoft.nahoft.Nahoft"),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
*/
        /*setApplicationShortcutDefault.setOnClickListener{
            val ShortcutDefaultIntent = Intent(this,packageManager,startActivity(packageManager) )*/
        }
    }
