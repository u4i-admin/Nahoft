package org.nahoft.nahoft.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_help.*
import org.nahoft.nahoft.R

class HelpActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        help_menu_button_about.setOnClickListener {
            startActivity(intent)
        }

        help_menu_button_read.setOnClickListener {
            val intent = Intent(this, ReadUserGuideActivity::class.java)
            startActivity(intent)
        }

        help_menu_button_create.setOnClickListener {
            val intent = Intent(this, CreateUserGuideActivity::class.java)
            startActivity(intent)
        }

        help_menu_button_friends.setOnClickListener {
            val intent = Intent(this, FriendsUserGuideActivity::class.java)
            startActivity(intent)
        }

        help_menu_button_settings.setOnClickListener {
            val intent = Intent(this, SettingsUserGuideActivity::class.java)
            startActivity(intent)
        }
    }
}