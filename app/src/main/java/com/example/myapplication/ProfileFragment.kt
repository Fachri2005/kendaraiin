package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {

    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userRepository = UserRepository(requireContext())
        val tvName = view.findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = view.findViewById<TextView>(R.id.tvProfileEmail)
        val tvEditProfile = view.findViewById<TextView>(R.id.tvEditProfile)
        val tvLogout = view.findViewById<TextView>(R.id.tvLogout)
        val ivProfile = view.findViewById<ImageView>(R.id.ivProfile)

        // Get current user session
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", "") ?: ""

        // Load latest data from Database for consistency
        val user = userRepository.getUserByEmail(userEmail)
        if (user != null) {
            tvName.text = user.name
            tvEmail.text = user.email
            
            if (user.imageUri.isNotEmpty()) {
                try {
                    ivProfile.setImageURI(Uri.parse(user.imageUri))
                } catch (e: Exception) {
                    ivProfile.setImageResource(android.R.drawable.ic_menu_myplaces)
                }
            }
        } else {
            // Fallback to shared prefs
            tvName.text = sharedPref.getString("user_name", "Pengguna")
            tvEmail.text = userEmail
        }

        // Navigate to Edit Profile
        tvEditProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_edit_profile)
        }

        // Logout logic
        tvLogout.setOnClickListener {
            sharedPref.edit().apply {
                putBoolean("is_logged_in", false)
                apply()
            }
            
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}