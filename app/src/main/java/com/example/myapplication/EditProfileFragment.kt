package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(requireContext()) }
    private var selectedImageUri: Uri? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)
                    selectedImageUri = it
                    binding.ivEditProfile.setImageURI(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, getString(R.string.error_image_permission), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        // Load current data via ViewModel
        userViewModel.getUserEmail()?.let { email ->
            userViewModel.fetchUser(email)
        }

        binding.cvEditProfileImage.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            val newName = binding.etEditName.text.toString().trim()
            val newPhone = binding.etEditPhone.text.toString().trim()
            val email = userViewModel.getUserEmail()

            if (newName.isNotEmpty() && email != null) {
                val currentImageUri = userViewModel.user.value?.imageUri ?: ""
                val imageUriString = selectedImageUri?.toString() ?: currentImageUri
                
                userViewModel.updateUser(email, newName, newPhone, imageUriString)
            } else {
                Toast.makeText(context, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etEditName.setText(it.name)
                binding.etEditEmail.setText(it.email)
                binding.etEditPhone.setText(it.phone)
                if (it.imageUri.isNotEmpty()) {
                    try {
                        val uri = Uri.parse(it.imageUri)
                        requireContext().contentResolver.openInputStream(uri)?.use {
                            binding.ivEditProfile.setImageURI(uri)
                        }
                    } catch (e: Exception) {
                        binding.ivEditProfile.setImageResource(android.R.drawable.ic_menu_myplaces)
                    }
                }
            }
        }

        // Poin 2: Menggunakan EventWrapper untuk event satu kali
        userViewModel.isSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Toast.makeText(context, getString(R.string.profile_update_success), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        userViewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                errorResId?.let {
                    Toast.makeText(context, getString(it), Toast.LENGTH_SHORT).show()
                }
            }
        }

        userViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) "..." else getString(R.string.btn_save_changes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
