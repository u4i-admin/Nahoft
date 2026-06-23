package org.nahoft.nahoft.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity
import org.nahoft.nahoft.databinding.FragmentVerifyStatusBinding

private const val USER_PUBLIC_KEY = "User's Public key"
private const val FRIEND_PUBLIC_KEY = "Friend's public key"
private const val FRIEND_NAME = "Friend's name"

class VerifyStatusFragment : Fragment()
{
    private var _binding: FragmentVerifyStatusBinding? = null
    private val binding get() = _binding!!
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
    ): View?
    {
        // Inflate the layout for this fragment
        _binding = FragmentVerifyStatusBinding.inflate(layoutInflater)
        return binding.root
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

        binding.userPublicKey.text = userPublicKey
        binding.friendPublicKey.text = friendPublicKey
        binding.friendPublicKeyTitle.text = getString(R.string.label_verify_friend_number, friendsName)

        binding.approveButton.setOnClickListener { (activity as FriendInfoActivity?)?.approveVerifyFriend() }
        binding.declineButton.setOnClickListener { (activity as FriendInfoActivity?)?.declineVerifyFriend() }
    }

    // Clean up binding reference when Fragment's view is destroyed
    // This is important to prevent memory leaks in Fragments
    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }
}