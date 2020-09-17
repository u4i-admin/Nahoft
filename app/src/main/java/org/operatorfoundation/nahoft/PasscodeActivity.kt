package org.operatorfoundation.nahoft

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_passcode.*


class PasscodeActivity : AppCompatActivity () {

    fun OnCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passcode)

        login_button.setOnClickListener {
            var status=if (user_name_edit_text.text.toString().equals("Jessica")
                &&passcode_edit_text.text.toString().equals("password")) "Logged In Successfully"
            else "Login Failed"
            Toast.makeText(this,status,Toast.LENGTH_SHORT).show()
        }
}
}
