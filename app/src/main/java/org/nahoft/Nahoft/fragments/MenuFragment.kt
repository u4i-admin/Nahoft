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
import kotlinx.android.synthetic.main.fragment_menu.*
import kotlinx.android.synthetic.main.fragment_menu.friend_public_key
import kotlinx.android.synthetic.main.fragment_menu.friend_public_key_title
import kotlinx.android.synthetic.main.fragment_menu.user_public_key
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity

// the fragment initialization parameters
private const val FRIEND = "friend"
private const val USER_PUBLIC_KEY = "User's Public key"
private const val FRIEND_PUBLIC_KEY = "Friend's public key"
class MenuFragment : Fragment() {
    private var friend: Friend? = null
    private var userPublicKey: String? = null
    private var friendPublicKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false)
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

        enter_name_input.setText(friend?.name)
//        enter_phone_input.setText(friend?.phone)
        user_public_key.text = userPublicKey
        friend_public_key.text = friendPublicKey
        friend_public_key_title.text = getString(R.string.label_verify_friend_number, friend?.name)

        if (friendPublicKey == "") {
            approve_button.isVisible = false
            decline_button.isVisible = false
        } else {
            approve_button.isVisible = true
            decline_button.isVisible = true
        }

        enter_name_input.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveName()
            }
        }

//        enter_phone_input.setOnFocusChangeListener { _, hasFocus ->
//            if (!hasFocus) {
//                savePhoneNumber()
//            }
//        }

        save_button.setOnClickListener {
            saveName()
//            savePhoneNumber()
        }

        approve_button.setOnClickListener { (activity as FriendInfoActivity?)?.approveVerifyFriend() }
        decline_button.setOnClickListener { (activity as FriendInfoActivity?)?.declineVerifyFriend() }
    }

    private fun saveName() {
        // If a new name has been entered, save it and display it
        if (enter_name_input.text?.isNotBlank() == true && enter_name_input.isDirty)
        {
            val newName = enter_name_input.text.toString()

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
}