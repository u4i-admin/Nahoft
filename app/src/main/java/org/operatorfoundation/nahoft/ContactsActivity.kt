package org.operatorfoundation.nahoft

import android.os.Bundle
import androidx.loader.app.LoaderManager
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import android.database.Cursor
import androidx.loader.app.LoaderManager.LoaderCallbacks
import android.content.Intent
import androidx.fragment.app.Fragment
import android.provider.ContactsContract.Contacts


class ContactsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
    }
}

//class ContactsFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener


