package com.example.myapplication

import android.content.Context
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private lateinit var userRepository: UserRepository
    private var userEmail: String? = null
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

        userRepository = UserRepository(requireContext())
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("user_email", null)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val etName = view.findViewById<EditText>(R.id.etEditName)
        val etEmail = view.findViewById<EditText>(R.id.etEditEmail)
        val etPhone = view.findViewById<EditText>(R.id.etEditPhone)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        
        ivProfile = view.findViewById(R.id.ivEditProfile)
        view.findViewById<View>(R.id.cvEditProfileImage).setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Load current data using Coroutines
        userEmail?.let { email ->
            viewLifecycleOwner.lifecycleScope.launch {
                val user = userRepository.getUserByEmail(email)
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
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()

            if (newName.isNotEmpty() && userEmail != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val currentUser = userRepository.getUserByEmail(userEmail!!)
                    val imageUriString = selectedImageUri?.toString() ?: currentUser?.imageUri ?: ""
                    
                    val result = userRepository.updateUser(userEmail!!, newName, newPhone, imageUriString)
                    if (result > 0) {
                        sharedPref.edit().apply {
                            putString("user_name", newName)
                            putString("user_image", imageUriString)
                            apply()
                        }
                        Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(context, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
