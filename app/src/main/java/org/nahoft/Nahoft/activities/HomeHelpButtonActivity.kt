package org.nahoft.nahoft.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import org.nahoft.nahoft.R
import java.lang.IllegalStateException

class HomeHelpButtonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_help_button)
    }

    /*class HomeHelpButtonDialogFragment : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setMessage(R.string.button_home_help)
                    .setNeutralButton(R.string.ok_button,
                        DialogInterface.OnClickListener { dialog, id ->
                            // Ok Btn
                        })
                // Create the AlertDialog object and return it
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }*/
}