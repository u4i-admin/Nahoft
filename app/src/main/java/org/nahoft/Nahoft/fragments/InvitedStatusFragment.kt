package org.nahoft.nahoft.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_default_status.*
import kotlinx.android.synthetic.main.fragment_invited_status.*
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity

class InvitedStatusFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_invited_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        import_invitation_button.setOnClickListener { (activity as FriendInfoActivity?)?.importInvitationClicked() }
    }
}