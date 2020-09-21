package org.org.nahoft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import android.content.res.Configuration
import android.content.res.Resources
import java.util.*
import kotlin.collections.ArrayList

@Suppress("DEPRECATION")

class SetLanguage : AppCompatActivity() {

    lateinit var spinner: Spinner
    lateinit var locale: Locale

    private var currentLanguage = "en"
    private var currentLang: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_language)
        title = "SetLanguage"
        currentLang = intent.getStringExtra(currentLang).toString()
        spinner = findViewById(R.id.setLanguage)
        val list = ArrayList<String>()
        list.add("Default Language")
        list.add("English")
        list.add("Persian")

        println(
            getResources().getConfiguration().locale
                .getDisplayName())

        val adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                when (position) {
                    0 -> {
                    }
                    1 -> setLocale("en")
                    2 -> setLocale("fa-rIR")
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
    }

    fun setLocale(localeName: String) {
        if (localeName != currentLanguage) {

            println(getResources().getConfiguration().locale
                .getDisplayName())

            locale = Locale(localeName)
            Locale.setDefault(locale)
            val res: Resources = getResources()
            val dm: DisplayMetrics = res.getDisplayMetrics()
            val conf: Configuration = res.getConfiguration()
            conf.setLocale(locale)
            res.updateConfiguration(conf, dm);
            getApplicationContext().createConfigurationContext(conf);

            println(getResources().getConfiguration().locale
                .getDisplayName())

            val refresh = Intent(
                this,
                SetLanguage::class.java
            )
            refresh.putExtra(currentLang, localeName)
            startActivity(refresh)
        } else {
            Toast.makeText(
                this@SetLanguage, "LANGUAGE ALREADY SELECTED", Toast.LENGTH_SHORT).show();
        }
    }

//override fun onBackPressed(){
    //val intent = Intent(Intent.ACTION_MAIN)
    //intent.addCategory(Intent.CATEGORY_HOME)
    //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    //startActivity(intent)
    //finish()
        //}
}



