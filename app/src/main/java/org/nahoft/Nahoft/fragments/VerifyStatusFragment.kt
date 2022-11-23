package org.nahoft.nahoft.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_verify_status.*
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity

private const val USER_PUBLIC_KEY = "User's Public key"
private const val FRIEND_PUBLIC_KEY = "Friend's public key"
private const val FRIEND_NAME = "Friend's name"

class VerifyStatusFragment : Fragment() {
    private var userPublicKey: String? = null
    private var friendPublicKey: String? = null
    private var friendsName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userPublicKey = it.getString(USER_PUBLIC_KEY)
            friendPublicKey = it.getString(FRIEND_PUBLIC_KEY)
            friendsName = it.getString(FRIEND_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_verify_status, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param userPublicKey Parameter 1.
         * @param friendPublicKey Parameter 2.
         * @param friendName Parameter 3
         * @return A new instance of fragment VerifyStatusFragment.
         */
        @JvmStatic
        fun newInstance(userPublicKey: String, friendPublicKey: String, friendName: String) =
            VerifyStatusFragment().apply {
                arguments = Bundle().apply {
                    putString(USER_PUBLIC_KEY, userPublicKey)
                    putString(FRIEND_PUBLIC_KEY, friendPublicKey)
                    putString(FRIEND_NAME, friendName)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        user_public_key.text = userPublicKey
        friend_public_key.text = friendPublicKey
        friend_public_key_title.text = getString(R.string.label_verify_friend_number, friendsName)

        approve_button.setOnClickListener { (activity as FriendInfoActivity?)?.approveVerifyFriend() }
        decline_button.setOnClickListener { (activity as FriendInfoActivity?)?.declineVerifyFriend() }
    }
}