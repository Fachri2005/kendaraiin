package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController

class EditProfileFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(requireContext()) }
    private var selectedImageUri: Uri? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var ivProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)
                    selectedImageUri = it
                    ivProfile.setImageURI(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Gagal mengambil izin gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val etName = view.findViewById<EditText>(R.id.etEditName)
        val etEmail = view.findViewById<EditText>(R.id.etEditEmail)
        val etPhone = view.findViewById<EditText>(R.id.etEditPhone)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        ivProfile = view.findViewById(R.id.ivEditProfile)

        setupObservers(etName, etEmail, etPhone)

        // Load current data via ViewModel
        userViewModel.getUserEmail()?.let { email ->
            userViewModel.fetchUser(email)
        }

        view.findViewById<View>(R.id.cvEditProfileImage).setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()
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

    private fun setupObservers(etName: EditText, etEmail: EditText, etPhone: EditText) {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                etName.setText(it.name)
                etEmail.setText(it.email)
                etPhone.setText(it.phone)
                if (it.imageUri.isNotEmpty()) {
                    try {
                        ivProfile.setImageURI(Uri.parse(it.imageUri))
                    } catch (e: Exception) {
                        ivProfile.setImageResource(android.R.drawable.ic_menu_myplaces)
                    }
                }
            }
        }

        userViewModel.isSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        userViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        userViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Update UI state based on loading if needed
        }
    }
}
