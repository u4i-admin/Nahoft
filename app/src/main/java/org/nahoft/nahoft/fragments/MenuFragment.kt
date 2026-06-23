package org.nahoft.nahoft.fragments

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.nahoft.nahoft.models.Friend
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity
import org.nahoft.nahoft.databinding.FragmentMenuBinding

// the fragment initialization parameters
private const val FRIEND = "friend"
private const val USER_PUBLIC_KEY = "User's Public key"
private const val FRIEND_PUBLIC_KEY = "Friend's public key"
class MenuFragment : Fragment()
{
    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    private var friend: Friend? = null
    private var userPublicKey: String? = null
    private var friendPublicKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        arguments?.let {
            friend = it.getSerializable(FRIEND) as Friend?
            userPublicKey = it.getString(USER_PUBLIC_KEY)
            friendPublicKey = it.getString(FRIEND_PUBLIC_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        // Inflate the layout for this fragment
        _binding = FragmentMenuBinding.inflate(layoutInflater)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param friend Parameter 1.
         * @return A new instance of fragment MenuFragment.
         */
        @JvmStatic
        fun newInstance(friend: Friend, userPublicKey: String, friendPublicKey: String) =
            MenuFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(FRIEND, friend)
                    putString(USER_PUBLIC_KEY, userPublicKey)
                    putString(FRIEND_PUBLIC_KEY, friendPublicKey)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.enterNameInput.setText(friend?.name)
//        enter_phone_input.setText(friend?.phone)
        binding.userPublicKey.text = userPublicKey
        binding.friendPublicKey.text = friendPublicKey
        binding.friendPublicKeyTitle.text = getString(R.string.label_verify_friend_number, friend?.name)

        if (friendPublicKey == "") {
            binding.approveButton.isVisible = false
            binding.declineButton.isVisible = false
        } else {
            binding.approveButton.isVisible = true
            binding.declineButton.isVisible = true
        }

        binding.enterNameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveName()
            }
        }

//        enter_phone_input.setOnFocusChangeListener { _, hasFocus ->
//            if (!hasFocus) {
//                savePhoneNumber()
//            }
//        }

        binding.saveButton.setOnClickListener {
            saveName()
//            savePhoneNumber()
        }

        binding.approveButton.setOnClickListener { (activity as FriendInfoActivity?)?.approveVerifyFriend() }
        binding.declineButton.setOnClickListener { (activity as FriendInfoActivity?)?.declineVerifyFriend() }
    }

    private fun saveName() {
        // If a new name has been entered, save it and display it
        if (binding.enterNameInput.text?.isNotBlank() == true && binding.enterNameInput.isDirty)
        {
            val newName = binding.enterNameInput.text.toString()

            if (newName != friend!!.name)
            {
                (activity as FriendInfoActivity?)?.changeFriendsName(newName)
                removePhoneKeypad()
            }
        }
    }

//    private fun savePhoneNumber() {
//        // If a new phone number has been entered, save it
//        if (enter_phone_input.text?.isNotBlank() == true && enter_phone_input.isDirty)
//        {
//            val newPhoneNumber = enter_phone_input.text.toString()
//
//            if (newPhoneNumber != friend!!.phone)
//            {
//                (activity as FriendInfoActivity?)?.changeFriendsPhone(newPhoneNumber)
//                removePhoneKeypad()
//            }
//        }
//    }

    private fun removePhoneKeypad() {
        val inputManager = view?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val binder: IBinder = requireView().windowToken
        inputManager.hideSoftInputFromWindow(
            binder,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    // Clean up binding reference when Fragment's view is destroyed
    // This is important to prevent memory leaks in Fragments
    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }
}