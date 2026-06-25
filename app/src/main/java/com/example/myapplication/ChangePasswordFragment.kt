package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentChangePasswordBinding

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Proteksi: Jika email tidak ada, jangan biarkan user di sini
        if (userViewModel.getUserEmail() == null) {
            Toast.makeText(requireContext(), getString(R.string.error_must_login), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        binding.toolbarChangePassword.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupObservers()

        binding.btnUpdatePassword.setOnClickListener {
            val currentPass = binding.etCurrentPassword.text.toString().trim()
            val newPass = binding.etNewPassword.text.toString().trim()
            val confirmPass = binding.etConfirmNewPassword.text.toString().trim()

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(requireContext(), getString(R.string.error_password_short), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(requireContext(), getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = userViewModel.getUserEmail()
            if (email != null) {
                userViewModel.changePassword(email, currentPass, newPass)
            }
        }
    }

    private fun setupObservers() {
        userViewModel.isSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Toast.makeText(requireContext(), getString(R.string.password_update_success), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        userViewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                errorResId?.let {
                    Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                }
            }
        }

        userViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnUpdatePassword.isEnabled = !isLoading
            binding.btnUpdatePassword.text = if (isLoading) "..." else getString(R.string.btn_update_password)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
