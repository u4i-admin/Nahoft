package org.operatorfoundation.nahoft

import android.os.Bundle
import androidx.loader.app.LoaderManager
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import android.database.Cursor
import androidx.loader.app.LoaderManager.LoaderCallbacks
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.provider.ContactsContract.Contacts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import kotlinx.android.synthetic.main.activity_contacts.*

val from_columns : Array<String> = arrayOf(Contacts.DISPLAY_NAME_PRIMARY)

private val TO_IDS: IntArray = intArrayOf(android.R.id.text1)

class FriendsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
    }
}

class ContactsFragment() : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
    Parcelable {
    // Define global mutable variables
    // Define a ListView object
    lateinit var contactsList: ListView
    // Define variables for the contact the user selects
    // The contact's _ID value
    var contactId: Long = 0
    // The contact's LOOKUP_KEY
    var contactKey: String? = null
    // A content URI for the selected contact
    var contactUri: Uri? = null
    // An adapter that binds the result Cursor to the ListView
    private var cursorAdapter: SimpleCursorAdapter? = null
    val projection:Array<String> = arrayOf(Contacts._ID,
        Contacts.LOOKUP_KEY,Contacts.DISPLAY_NAME,Contacts.PHOTO_THUMBNAIL_URI)

    val selectionArgs = arrayOf<String>("${Contacts.DISPLAY_NAME_PRIMARY} LIKE ?")

    constructor(parcel: Parcel) : this() {
    }

    override fun onItemClick(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        // Get the Cursor
        val cursor: Cursor? = (parent.adapter as? CursorAdapter)?.cursor?.apply {
            // Move to the selected contact
            moveToPosition(position)
            // Get the _ID value
            contactId = getLong(0)
            // Get the selected LOOKUP KEY
            contactKey = getString(1)
            // Create the contact's content Uri
            contactUri = Contacts.getLookupUri(contactId, contactKey)
            /*
             * You can use contactUri as the content URI for retrieving
             * the details for a contact.
             */
        }
    }

    // A UI Fragment must inflate its View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.contacts_list_item, container, false)
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Gets the ListView from the View list of the parent activity
        activity?.also {
                   // Gets a CursorAdapter
            cursorAdapter = SimpleCursorAdapter(
                it,
                R.layout.contacts_list_item,
                null,
                from_columns,
                TO_IDS,
                0
            )
            // Sets the adapter for the ListView
            contact_list.adapter = cursorAdapter
        }
        contact_list.onItemClickListener = this



        loaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Cursor> {
        /*
         * Makes search string into pattern and
         * stores it in the selection array
         */
        // Starts the query
        return activity?.let {
            return CursorLoader(
                it,
                Contacts.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
        } ?: throw IllegalStateException()
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        // Put the result Cursor in the adapter for the ListView
        cursorAdapter?.swapCursor(cursor)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        // Delete the reference to the existing Cursor
        cursorAdapter?.swapCursor(null)
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


