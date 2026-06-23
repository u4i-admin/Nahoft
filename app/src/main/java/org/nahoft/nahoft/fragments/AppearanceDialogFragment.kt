package org.nahoft.nahoft.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import org.nahoft.nahoft.adapters.AppearanceAdapter
import org.nahoft.nahoft.databinding.FragmentAppearanceDialogBinding
import org.nahoft.util.AppIdentity

class AppearanceDialogFragment : DialogFragment() {

    private var _binding: FragmentAppearanceDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var currentIdentity: AppIdentity
    private var selectedIdentity: AppIdentity? = null

    var onIdentitySelected: ((AppIdentity) -> Unit)? = null

    companion object {
        private const val ARG_CURRENT_IDENTITY = "current_identity"

        fun newInstance(currentIdentity: AppIdentity): AppearanceDialogFragment {
            return AppearanceDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_IDENTITY, currentIdentity.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIdentity = AppIdentity.valueOf(
            arguments?.getString(ARG_CURRENT_IDENTITY) ?: AppIdentity.NAHOFT.name
        )
        selectedIdentity = currentIdentity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppearanceDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AppearanceAdapter(
            identities = AppIdentity.entries.toTypedArray(),
            selectedIdentity = currentIdentity,
            onSelectionChanged = { selectedIdentity = it }
        )

        binding.rvIdentities.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            this.adapter = adapter
        }

        binding.btnOk.setOnClickListener {
            selectedIdentity?.let { onIdentitySelected?.invoke(it) }
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}