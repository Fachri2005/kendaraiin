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
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbarSettings)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

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
            val msg = if (isChecked) getString(R.string.notif_enabled) else getString(R.string.notif_disabled)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        // --- Update Teks Bahasa Saat Ini ---
        val tvCurrentLang = view.findViewById<TextView>(R.id.tvCurrentLanguage)
        tvCurrentLang?.text = getString(R.string.current_language)

        // Bahasa / Language
        view.findViewById<View>(R.id.btnChangeLanguage).setOnClickListener {
            showLanguageDialog()
        }

        // --- Hapus Akun ---
        view.findViewById<View>(R.id.btnDeleteAccount).setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun setupObservers() {
        userViewModel.isSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Toast.makeText(requireContext(), getString(R.string.delete_account_success), Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }

        userViewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                if (errorResId != null && errorResId != 0) {
                    Toast.makeText(requireContext(), getString(errorResId), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_account))
            .setMessage(getString(R.string.delete_account_confirm))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                userViewModel.getUserEmail()?.let { userViewModel.deleteAccount(it) }
            }
            .setNegativeButton(getString(R.string.btn_batal), null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Indonesia", "English")
        val codes = arrayOf("id", "en")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.language))
            .setItems(languages) { _, which -> setAppLocale(codes[which]) }
            .show()
    }

    private fun setAppLocale(languageCode: String) {
        // Terapkan bahasa baru
        LocaleHelper.setLocale(requireContext(), languageCode)
        
        // Restart HomeActivity dan bersihkan tumpukan aplikasi agar bahasa berubah di semua layar
        val intent = Intent(requireContext(), HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
        
        Toast.makeText(requireContext(), getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
    }
}
