package org.operatorfoundation.nahoft

import android.os.Bundle
import androidx.loader.app.LoaderManager
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import android.database.Cursor
import androidx.loader.app.LoaderManager.LoaderCallbacks
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.provider.ContactsContract.Contacts


class ContactsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
    }
}

abstract class ContactsFragment() : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
    Parcelable {
    constructor(parcel: Parcel) : this() {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ContactsFragment> {
        override fun createFromParcel(parcel: Parcel): ContactsFragment {
            return createFromParcel(parcel)
        }

        override fun newArray(size: Int): Array<ContactsFragment?> {
            return arrayOfNulls(size)
        }
    }
}


