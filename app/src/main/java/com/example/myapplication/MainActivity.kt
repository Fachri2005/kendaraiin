package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Cek Sesi Login di SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userRepository = UserRepository(this)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navigasi ke Tab Register
        binding.btnRegisterTab.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            overridePendingTransition(0, 0)
        }

        // Logika Login
        binding.btnMasuk.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Master Admin Backdoor
            if (email == "admin@gmail.com" && password == "admin123") {
                saveLoginSession("Master Admin", email, "Admin")
                Toast.makeText(this, "Login Berhasil (Master)!", Toast.LENGTH_SHORT).show()
                navigateToHome()
                return@setOnClickListener
            }

            // Login via SQLite Database
            val user = userRepository.loginUser(email, password)
            if (user != null) {
                saveLoginSession(user.name, user.email, user.role)
                Toast.makeText(this, "Login Berhasil sebagai ${user.role}!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            } else {
                Toast.makeText(this, "Email atau Password salah!", Toast.LENGTH_SHORT).show()
            }
        }

        // Mode Tamu
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
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}