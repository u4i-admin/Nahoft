package org.nahoft.nahoft.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity
import org.nahoft.nahoft.databinding.FragmentDefaultStatusBinding

private const val FRIEND = "friend"

class DefaultStatusFragment : Fragment()
{
    private var _binding: FragmentDefaultStatusBinding? = null
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
        _binding = FragmentDefaultStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param friend Parameter 1.
         * @return A new instance of fragment DefaultStatusFragment.
         */
        @JvmStatic
        fun newInstance(friend: Friend) =
            DefaultStatusFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(FRIEND, friend)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.friendsName.text = friend?.name
        binding.textView.text = String.format(getString(R.string.default_fragment_text), friend?.name)
        binding.inviteButton.setOnClickListener {
            binding.keyImageview.isVisible = true
            binding.keyImageview.animate().apply {
                duration = 500
                translationX(300F)
            }.withEndAction {
                (activity as FriendInfoActivity?)?.inviteClicked()
            }
        }
    }

    // Clean up binding reference when Fragment's view is destroyed
    // This is important to prevent memory leaks in Fragments
    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }
}