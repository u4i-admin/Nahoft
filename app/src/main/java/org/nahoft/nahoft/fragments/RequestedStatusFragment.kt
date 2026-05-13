package org.nahoft.nahoft.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.nahoft.nahoft.models.Friend
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity
import org.nahoft.nahoft.databinding.FragmentRequestedStatusBinding

// the fragment initialization parameters
private const val FRIEND = "friend"

class RequestedStatusFragment : Fragment()
{
    private var _binding: FragmentRequestedStatusBinding? = null
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
        _binding = FragmentRequestedStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param friend Parameter 1.
         * @return A new instance of fragment RequestedStatusFragment.
         */
        @JvmStatic
        fun newInstance(friend: Friend) =
            RequestedStatusFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(FRIEND, friend)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.friendsName.text = friend?.name
        binding.textView.text = getString(R.string.requested_fragment_text, friend?.name)
        binding.inviteButton.setOnClickListener { (activity as FriendInfoActivity?)?.inviteClicked() }
    }

    // Clean up binding reference when Fragment's view is destroyed
    // This is important to prevent memory leaks in Fragments
    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }
}