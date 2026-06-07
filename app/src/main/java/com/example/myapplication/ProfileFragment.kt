package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = view.findViewById<TextView>(R.id.tvProfileEmail)
        val tvEditProfile = view.findViewById<TextView>(R.id.tvEditProfile)
        val tvLogout = view.findViewById<TextView>(R.id.tvLogout)
        val ivProfile = view.findViewById<ImageView>(R.id.ivProfile)

        // Set initial data from session immediately
        tvName.text = userViewModel.getUserName() ?: getString(R.string.guest_name)
        tvEmail.text = userViewModel.getUserEmail() ?: ""

        setupObservers(tvName, tvEmail, ivProfile)

        // Fetch latest user data from server
        userViewModel.getUserEmail()?.let { email ->
            userViewModel.fetchUser(email)
        }

        // Navigate to Edit Profile
        tvEditProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_edit_profile)
        }

        // Logout logic
        tvLogout.setOnClickListener {
            userViewModel.logout()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupObservers(tvName: TextView, tvEmail: TextView, ivProfile: ImageView) {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                tvName.text = user.name
                tvEmail.text = user.email
                
                if (user.imageUri.isNotEmpty()) {
                    try {
                        // Jika imageUri adalah path lokal dari OpenDocument
                        ivProfile.setImageURI(Uri.parse(user.imageUri))
                        ivProfile.colorFilter = null // Hapus tint jika ada gambar
                    } catch (e: Exception) {
                        ivProfile.setImageResource(android.R.drawable.ic_menu_myplaces)
                    }
                }
            }
        }
    }
}
