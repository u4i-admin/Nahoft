package org.nahoft.nahoft.activities

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_application_shortcut.*
import org.nahoft.nahoft.R
import org.nahoft.nahoft.R.layout.activity_application_shortcut

class ApplicationShortcutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_shortcut)

//    fun showLauncherSelector(activity: AppCompatActivity, requestCode : Int) {
//        val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager
//        if(roleManager.isRoleAvailable(RoleManager.ROLE_HOME)){
//            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
//            activity.startActivityForResult(intent, requestCode)
//        }
//    }

       /* getPackageManager().setComponentEnabledSetting(
            ComponentName("org.nahoft.nahoft", "org.nahoft.nahoft.Nahoft"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )*/
        //setApplicationShortcutDefault.setOnClickListener {
            //val ShortcutDefaultIntent = Intent(this, packageManager, startActivity(packageManager))
        //}
    }
}

