package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityRegisterBinding
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Cek jika sudah login
        if (userViewModel.isLoggedIn()) {
            navigateToHome()
            return
        }

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupDropdowns()
        setupDatePicker()
        setupObservers()

        binding.btnMasukTab.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        binding.btnLanjut.setOnClickListener {
            handleRegistration()
        }

        binding.btnGuest.setOnClickListener {
            userViewModel.loginAsGuest()
        }
    }

    private fun setupDropdowns() {
        val roles = arrayOf("Customer", "Admin")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        binding.actvRole.setAdapter(roleAdapter)

        val genders = arrayOf("Laki-laki", "Perempuan")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.actvGender.setAdapter(genderAdapter)
    }

    private fun setupDatePicker() {
        binding.etBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)
                    binding.etBirth.setText(date)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun setupObservers() {
        userViewModel.isSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
        }

        userViewModel.user.observe(this) { user ->
            if (user != null) navigateToHome()
        }

        userViewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        userViewModel.isLoading.observe(this) { isLoading ->
            binding.btnLanjut.isEnabled = !isLoading
            // Anda bisa menambahkan ProgressBar di sini jika ada di layout
        }
    }

    private fun handleRegistration() {
        val role = binding.actvRole.text.toString().trim()
        val nama = binding.etNama.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val telp = binding.etTelp.text.toString().trim()
        val telpDarurat = binding.etTelpDarurat.text.toString().trim()
        val gender = binding.actvGender.text.toString().trim()
        val birth = binding.etBirth.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (validateInput(role, nama, email, telp, telpDarurat, gender, birth, password, confirmPassword)) {
            val user = User(
                name = nama,
                email = email,
                password = password,
                role = role,
                phone = telp,
                emergencyPhone = telpDarurat,
                gender = gender,
                birthDate = birth
            )
            userViewModel.register(user)
        }
    }

    private fun validateInput(vararg fields: String): Boolean {
        if (fields.any { it.isEmpty() }) {
            Toast.makeText(this, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(fields[2]).matches()) {
            Toast.makeText(this, "Format email tidak valid!", Toast.LENGTH_SHORT).show()
            return false
        }
        if (fields[7].length < 6) {
            Toast.makeText(this, "Password minimal 6 karakter!", Toast.LENGTH_SHORT).show()
            return false
        }
        if (fields[7] != fields[8]) {
            Toast.makeText(this, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
