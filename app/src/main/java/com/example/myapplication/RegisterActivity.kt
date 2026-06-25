package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Patterns
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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupDropdowns()
        setupObservers()
        setupDatePicker()

        binding.btnMasukTab.setOnClickListener {
            finish()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }

        binding.btnLanjut.setOnClickListener {
            val name = binding.etNama.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etTelp.text.toString().trim()
            val emergencyPhone = binding.etTelpDarurat.text.toString().trim()
            val gender = binding.actvGender.text.toString()
            val birthDate = binding.etBirth.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val role = binding.actvRole.text.toString().ifEmpty { getString(R.string.role_customer) }

            // Validasi Input (Poin 1: Perbaikan Validasi)
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || 
                emergencyPhone.isEmpty() || birthDate.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, getString(R.string.error_password_short), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newUser = User(
                name = name,
                email = email,
                password = password,
                role = role,
                phone = phone,
                emergencyPhone = emergencyPhone,
                gender = gender,
                birthDate = birthDate
            )
            userViewModel.register(newUser)
        }
    }

    private fun setupDropdowns() {
        val genders = arrayOf(getString(R.string.gender_male), getString(R.string.gender_female))
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, genders)
        binding.actvGender.setAdapter(genderAdapter)

        val roles = arrayOf(getString(R.string.role_customer), getString(R.string.role_admin))
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, roles)
        binding.actvRole.setAdapter(roleAdapter)
    }

    private fun setupDatePicker() {
        binding.etBirth.showSoftInputOnFocus = false
        
        val showDialog = {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    binding.etBirth.setText(date)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etBirth.setOnClickListener { showDialog() }
        binding.etBirth.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDialog()
        }
    }

    private fun setupObservers() {
        // Poin 2: Implementasi EventWrapper agar tidak terpicu berulang saat rotasi
        userViewModel.isSuccess.observe(this) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        userViewModel.error.observe(this) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                // Jika errorResId tidak null, tampilkan toast
                Toast.makeText(this, getString(errorResId), Toast.LENGTH_SHORT).show()
            }
        }

        userViewModel.isLoading.observe(this) { isLoading ->
            binding.btnLanjut.isEnabled = !isLoading
        }
    }
}
