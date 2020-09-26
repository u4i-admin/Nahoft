package org.org.nahoft.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_select_key_sender.*
import org.org.nahoft.Persist
import org.org.nahoft.R
import org.org.nahoft.SelectKeySenderRecyclerAdapter
import org.org.nahoft.SelectMessageSenderRecyclerAdapter
import org.org.util.RequestCodes

class SelectKeySenderActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: SelectKeySenderRecyclerAdapter

    companion object {
        fun newIntent(context: Context) = Intent(context, SelectKeySenderActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_key_sender)

        linearLayoutManager = LinearLayoutManager(this)
        adapter = SelectKeySenderRecyclerAdapter(Persist.friendList) {

            // On click listener for the recycler view
            val result = Intent()
            result.putExtra(RequestCodes.friendExtraTaskDescription, it)
            setResult(RESULT_OK, result)
            finish()
        }

        select_k_sender_recycler_view.layoutManager = linearLayoutManager
        select_k_sender_recycler_view.adapter = adapter
    }
}