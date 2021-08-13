package org.nahoft.nahoft.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_create.*
import kotlinx.android.synthetic.main.activity_help.*
import kotlinx.android.synthetic.main.activity_help.go_to_home_button
import org.nahoft.nahoft.R

class HelpActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)

        help_menu_button_about.setOnClickListener {
            val intentAboutUserGuide = Intent(this, AboutUserGuideActivity::class.java)
            startActivity(intentAboutUserGuide)
        }

        help_menu_button_read.setOnClickListener {
            val intentReadUserGuide = Intent(this, ReadUserGuideActivity::class.java)
            startActivity(intentReadUserGuide)
        }

        help_menu_button_create.setOnClickListener {
            val intentCreateUserGuide = Intent(this, CreateUserGuideActivity::class.java)
            startActivity(intentCreateUserGuide)
        }

        help_menu_button_friends.setOnClickListener {
            val intentFriendsUserGuide = Intent(this, FriendsUserGuideActivity::class.java)
            startActivity(intentFriendsUserGuide)
        }

        help_menu_button_settings.setOnClickListener {
            val intentSettingsUserGuide = Intent(this, SettingsUserGuideActivity::class.java)
            startActivity(intentSettingsUserGuide)
        }

        // Return to Home
        go_to_home_button.setOnClickListener {
            val homeIntent = Intent(this, HomeActivity::class.java)
            startActivity(homeIntent)
        }
    }
}