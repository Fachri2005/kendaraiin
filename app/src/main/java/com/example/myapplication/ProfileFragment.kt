package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set initial data
        binding.tvProfileName.text = userViewModel.getUserName() ?: getString(R.string.profile_name_default)
        binding.tvProfileEmail.text = userViewModel.getUserEmail() ?: ""

        setupObservers()

        // Fetch data
        userViewModel.getUserEmail()?.let { userViewModel.fetchUser(it) }

        binding.menuEditProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_edit_profile)
        }

        binding.menuSettings.setOnClickListener {
            try {
                findNavController().navigate(R.id.navigation_settings)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_data_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        binding.menuHelp.setOnClickListener {
            try {
                findNavController().navigate(R.id.navigation_help_center)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_data_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        binding.menuLogout.setOnClickListener {
            userViewModel.logout()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupObservers() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (_binding == null) return@observe
            if (user != null) {
                binding.tvProfileName.text = user.name
                binding.tvProfileEmail.text = user.email
                
                if (!user.imageUri.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(Uri.parse(user.imageUri))
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .error(android.R.drawable.ic_menu_myplaces)
                        .into(binding.ivProfile)
                    binding.ivProfile.colorFilter = null
                } else {
                    binding.ivProfile.setImageResource(android.R.drawable.ic_menu_myplaces)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
