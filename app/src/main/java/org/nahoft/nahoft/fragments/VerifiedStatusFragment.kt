package org.nahoft.nahoft.fragments

import android.os.Bundle
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import org.nahoft.nahoft.*
import org.nahoft.nahoft.adapters.MessagesRecyclerAdapter
import org.nahoft.nahoft.databinding.FragmentVerifiedStatusBinding
import org.nahoft.nahoft.models.Friend
import org.nahoft.nahoft.models.Message

// the fragment initialization parameters
private const val FRIEND = "friend"

class VerifiedStatusFragment : Fragment()
{
    var _binding: FragmentVerifiedStatusBinding? = null
    val binding get() = _binding!!
    private var friend: Friend? = null
    private lateinit var filteredMessages: ArrayList<Message>

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: MessagesRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
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
        _binding = FragmentVerifiedStatusBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.messagesRecyclerView.adapter?.notifyDataSetChanged()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param friend Parameter 1.
         * @return A new instance of fragment VerifiedStatusFragment.
         */
        @JvmStatic
        fun newInstance(friend: Friend) =
            VerifiedStatusFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(FRIEND, friend)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup the messages RecyclerView
        linearLayoutManager = LinearLayoutManager(context)
        filteredMessages = Persist.messageList.filter { message ->  message.sender == friend } as ArrayList<Message>

        if (filteredMessages.size > 0) binding.noDataLayout.isVisible = false

        // Hide the empty-state hints when the keyboard is open. The hints are corner-anchored
        // to the fragment's full-height ConstraintLayout, so when adjustResize shrinks the
        // frame they collide in the middle. Hiding them on IME visibility avoids the collision
        // and is appropriate UX — the user is actively typing, not reading first-run guidance.
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val hasMessages = filteredMessages.isNotEmpty()

            // Only show hints when there are no messages AND the keyboard is closed.
            binding.noDataLayout.isVisible = !hasMessages && !imeVisible

            insets
        }
        ViewCompat.requestApplyInsets(view)


        adapter = MessagesRecyclerAdapter(filteredMessages)
        binding.messagesRecyclerView.layoutManager = linearLayoutManager
        binding.messagesRecyclerView.adapter = adapter
        binding.messagesRecyclerView.scrollToPosition(adapter.itemCount - 1)
        adapter.onItemLongClick = { message ->
            showDeleteConfirmationDialog(message)
        }
    }

    private fun showDeleteConfirmationDialog(message: Message)
    {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity(), R.style.AppTheme_DeleteAlertDialog)
        val title = SpannableString(getString(R.string.are_you_sure_to_delete))
        // alert dialog title align center
        title.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            title.length,
            0
        )
        builder.setTitle(title)

        builder.setPositiveButton(resources.getString(R.string.button_label_delete))
        { _, _->
            //delete friend
            deleteMessage(message)
        }

        builder.setNeutralButton(resources.getString(R.string.button_label_cancel))
        { _, _ ->
            //cancel
        }
        builder.create()
        builder.show()
    }

    private fun deleteMessage(message: Message)
    {
        context?.let {
            Persist.deleteMessage(it, message)
            filteredMessages.removeIf { msg -> msg == message && msg.timestamp == message.timestamp }
            adapter.notifyDataSetChanged()
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