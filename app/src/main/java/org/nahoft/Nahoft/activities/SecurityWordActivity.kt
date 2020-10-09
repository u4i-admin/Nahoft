package org.nahoft.nahoft.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_security_word.*
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.showAlert

class SecurityWordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_word)

        // Get the current security word from encrypted shared preferences in display it
        val maybeSecurityWord = Persist.encryptedSharedPreferences.getString(Persist.sharedPrefSecurityWordKey, null)

        maybeSecurityWord?.let {
            security_word_input.setText(it)
        }

        save_security_word_button.setOnClickListener {
            saveSecurityWordClick()
        }
    }

    fun saveSecurityWordClick() {
        val securityWord = security_word_input.text.toString()

        // If the input is empty, do nothing
        if (securityWord == "") {
            return
        }

        // Otherwise, save the security word
        Persist.saveKey(Persist.sharedPrefSecurityWordKey, securityWord)
    }
}