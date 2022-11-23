package org.nahoft.nahoft.fragments

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_menu.*
import org.nahoft.nahoft.Friend
import org.nahoft.nahoft.R
import org.nahoft.nahoft.activities.FriendInfoActivity

// the fragment initialization parameters
private const val FRIEND = "friend"

class MenuFragment : Fragment() {
    private var friend: Friend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            friend = it.getSerializable(FRIEND) as Friend?
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
        fun newInstance(friend: Friend) =
            MenuFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(FRIEND, friend)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enter_name_input.setText(friend?.name)
        enter_phone_input.setText(friend?.phone)

        verify_button.setOnClickListener {
            (activity as FriendInfoActivity?)?.showVerificationStep()
        }

        save_name_button.setOnClickListener {
            saveName()
        }

        save_phone_button.setOnClickListener {
            savePhoneNumber()
        }
    }

    private fun saveName() {
        // If a new name has been entered, save it and display it
        if (enter_name_input.text?.isNotBlank() == true)
        {
            val newName = enter_name_input.text.toString()

            if (newName != friend!!.name)
            {
                (activity as FriendInfoActivity?)?.changeFriendsName(newName)
                removePhoneKeypad()
            }
        }
    }

    private fun savePhoneNumber() {
        // If a new phone number has been entered, save it
        if (enter_phone_input.text?.isNotBlank() == true)
        {
            val newPhoneNumber = enter_phone_input.text.toString()

            if (newPhoneNumber != friend!!.phone)
            {
                (activity as FriendInfoActivity?)?.changeFriendsPhone(newPhoneNumber)
                removePhoneKeypad()
            }
        }
    }

    private fun removePhoneKeypad() {
        val inputManager = view?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val binder: IBinder = requireView().windowToken
        inputManager.hideSoftInputFromWindow(
            binder,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}