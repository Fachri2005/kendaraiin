package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import java.util.Calendar
import java.util.Locale

class RentalFormFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(requireContext()) }
    private val eventViewModel: EventViewModel by viewModels { ViewModelFactory(requireContext()) }
    
    private var vehicleId: Int = -1
    private var vehiclePrice: Int = 0
    private var vehicleName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rental_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vehicleId = arguments?.getInt("vehicle_id") ?: -1
        vehiclePrice = arguments?.getString("vehicle_price")?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
        vehicleName = arguments?.getString("vehicle_name") ?: ""

        val etName = view.findViewById<EditText>(R.id.etRentalName)
        val etStartDate = view.findViewById<EditText>(R.id.etStartDate)
        val etDuration = view.findViewById<EditText>(R.id.etDuration)
        val etPickup = view.findViewById<EditText>(R.id.etPickupLocation)
        val tvPriceDay = view.findViewById<TextView>(R.id.tvPricePerDay)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotalPrice)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmRental)

        tvPriceDay.text = "Rp ${String.format("%, d", vehiclePrice)}"

        // Pre-fill user name
        userViewModel.getUserEmail()?.let { email ->
            userViewModel.fetchUser(email)
        }
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            etName.setText(user?.name ?: "Pengguna")
        }

        setupDatePicker(etStartDate)

        etDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val duration = s.toString().toIntOrNull() ?: 0
                val total = duration * vehiclePrice
                tvTotal.text = "Rp ${String.format("%, d", total)}"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirm.setOnClickListener {
            val startDate = etStartDate.text.toString()
            val duration = etDuration.text.toString()
            val pickup = etPickup.text.toString()

            if (startDate.isEmpty() || duration.isEmpty() || pickup.isEmpty()) {
                Toast.makeText(context, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Di sini kita panggil fungsi rental di repository
            val userEmail = userViewModel.getUserEmail() ?: ""
            if (userEmail.isNotEmpty() && vehicleId != -1) {
                // Untuk sementara kita gunakan setRegistered yang ada
                // Di masa depan bisa ditambahkan tabel khusus penyewaan
                val repo = EventRepository(requireContext())
                repo.setRegistered(vehicleId, true, userEmail)
                
                Toast.makeText(context, "Berhasil menyewa $vehicleName!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack(R.id.navigation_home, false)
            }
        }
    }

    private fun setupDatePicker(editText: EditText) {
        editText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)
                    editText.setText(date)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }
}
