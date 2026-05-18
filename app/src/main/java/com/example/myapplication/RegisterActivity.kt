package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        
        userRepository = UserRepository(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup Role Dropdown
        val roles = arrayOf("Customer", "Admin")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        binding.actvRole.setAdapter(roleAdapter)

        // Setup Gender Dropdown
        val genders = arrayOf("Laki-laki", "Perempuan")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.actvGender.setAdapter(genderAdapter)

        // Setup Date Picker
        binding.etBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    binding.etBirth.setText(date)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        // Tab Navigation
        binding.btnMasukTab.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // Validation & Registration Logic
        binding.btnLanjut.setOnClickListener {
            val role = binding.actvRole.text.toString().trim()
            val nama = binding.etNama.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val telp = binding.etTelp.text.toString().trim()
            val telpDarurat = binding.etTelpDarurat.text.toString().trim()
            val gender = binding.actvGender.text.toString().trim()
            val birth = binding.etBirth.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (role.isEmpty() || nama.isEmpty() || email.isEmpty() || telp.isEmpty() || 
                telpDarurat.isEmpty() || gender.isEmpty() || birth.isEmpty() || 
                password.isEmpty() || confirmPassword.isEmpty()) {
                
                Toast.makeText(this, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
            } else {
                // Buat objek User untuk dikirim ke API XAMPP
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

                lifecycleScope.launch {
                    val isSuccess = userRepository.registerUser(user)

                    if (isSuccess) {
                        saveLoginSession(nama, email, role)
                        Toast.makeText(this@RegisterActivity, "Registrasi Berhasil ke Server!", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Registrasi Gagal! Cek koneksi XAMPP atau Email sudah terdaftar.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Guest Mode
        binding.btnGuest.setOnClickListener {
            saveLoginSession("Tamu", "guest@kendaraiin.com", "Customer")
            navigateToHome()
        }
    }

    private fun saveLoginSession(name: String, email: String, role: String) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("is_logged_in", true)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_role", role)
            apply()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
