package org.nahoft.nahoft.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity
import org.nahoft.nahoft.databinding.FragmentInvitedStatusBinding

// the fragment initialization parameters
private const val FRIEND = "friend"

class InvitedStatusFragment : Fragment()
{
    private var _binding: FragmentInvitedStatusBinding? = null
    private val binding get() = _binding!!
    private var friend: Friend? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        arguments?.let {
            friend = it.getSerializable(FRIEND) as Friend?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        // Inflate the layout for this fragment
        _binding = FragmentInvitedStatusBinding.inflate(layoutInflater)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param friend Parameter 1.
         * @return A new instance of fragment InvitedStatusFragment.
         */
        @JvmStatic
        fun newInstance(friend: Friend) =
            InvitedStatusFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(FRIEND, friend)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.friendsName.text = friend?.name
        binding.textView.text = String.format(getString(R.string.invited_fragment_text), friend?.name)
        binding.importInvitationButton.setOnClickListener { (activity as FriendInfoActivity?)?.importInvitationClicked() }
    }

    // Clean up binding reference when Fragment's view is destroyed
    // This is important to prevent memory leaks in Fragments
    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }
}