package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController

class SettingsFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_settings,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbarSettings)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupObservers()

        // --- Ganti Password ---
        view.findViewById<View>(R.id.btnChangePassword).setOnClickListener {
            findNavController().navigate(R.id.action_navigation_settings_to_navigation_change_password)
        }

        // --- Logika Switch Notifikasi ---
        val switchNotif = view.findViewById<Switch>(R.id.switchNotification)
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        
        switchNotif.isChecked = sharedPref.getBoolean("notifications_enabled", true)
        switchNotif.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notifications_enabled", isChecked).apply()
            val msg = if (isChecked) "Notifikasi diaktifkan" else "Notifikasi dimatikan"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        // --- Update Teks Bahasa Saat Ini ---
        val tvCurrentLang = view.findViewById<TextView>(R.id.tvCurrentLanguage)
        val currentLang = LocaleHelper.getLanguage(requireContext())
        tvCurrentLang?.text = if (currentLang == "id" || currentLang == "in") "Indonesia" else "English"

        // Bahasa / Language
        val btnLanguage = view.findViewById<View>(R.id.btnChangeLanguage)
        btnLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // --- Hapus Akun (Danger Zone) ---
        view.findViewById<View>(R.id.btnDeleteAccount).setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun setupObservers() {
        userViewModel.isSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Akun berhasil dihapus", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
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
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_account))
            .setMessage("Apakah Anda yakin ingin menghapus akun? Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                val email = userViewModel.getUserEmail()
                if (email != null) {
                    userViewModel.deleteAccount(email)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Indonesia", "English")
        val languageCodes = arrayOf("id", "en")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.language))
            .setItems(languages) { _, which ->
                setAppLocale(languageCodes[which])
            }
            .show()
    }

    private fun setAppLocale(languageCode: String) {
        LocaleHelper.setLocale(requireContext(), languageCode)
        
        val intent = requireActivity().intent
        requireActivity().finish()
        requireActivity().startActivity(intent)
    }
}
