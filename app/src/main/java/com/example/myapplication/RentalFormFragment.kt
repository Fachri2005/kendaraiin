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
import android.widget.ImageButton
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
    private var vehiclePrice: Long = 0
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
        // Improved price parsing: handle potentially large numbers and non-digit characters
        val rawPriceString = arguments?.getString("vehicle_price") ?: "0"
        vehiclePrice = rawPriceString.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0
        vehicleName = arguments?.getString("vehicle_name") ?: ""

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val etName = view.findViewById<EditText>(R.id.etRentalName)
        val etStartDate = view.findViewById<EditText>(R.id.etStartDate)
        val etDuration = view.findViewById<EditText>(R.id.etDuration)
        val etPickup = view.findViewById<EditText>(R.id.etPickupLocation)
        val tvPriceDay = view.findViewById<TextView>(R.id.tvPricePerDay)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotalPrice)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmRental)

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        tvPriceDay.text = getString(R.string.price_format, String.format(Locale.getDefault(), "%,d", vehiclePrice))

        // Pre-fill user name from session
        val currentUserName = userViewModel.getUserName()
        if (currentUserName != null) {
            etName.setText(currentUserName)
        } else {
            etName.setText(getString(R.string.guest_name))
        }

        // Fetch latest user data
        userViewModel.getUserEmail()?.let { email ->
            userViewModel.fetchUser(email)
        }
        
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                etName.setText(user.name)
            }
        }

        // Observe rental process using EventWrapper to prevent duplicate triggers
        eventViewModel.rentalSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Toast.makeText(context, getString(R.string.rental_success, vehicleName), Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack(R.id.navigation_home, false)
                }
            }
        }

        eventViewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                Toast.makeText(context, getString(errorResId), Toast.LENGTH_SHORT).show()
            }
        }

        setupDatePicker(etStartDate)

        etDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val duration = s.toString().toLongOrNull() ?: 0
                val total = duration * vehiclePrice
                tvTotal.text = getString(R.string.price_format, String.format(Locale.getDefault(), "%,d", total))
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            val startDate = etStartDate.text.toString().trim()
            val durationStr = etDuration.text.toString().trim()
            val pickup = etPickup.text.toString().trim()

            if (name.isEmpty() || startDate.isEmpty() || durationStr.isEmpty() || pickup.isEmpty()) {
                Toast.makeText(context, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = durationStr.toIntOrNull() ?: 0
            val userEmail = userViewModel.getUserEmail() ?: ""
            val userRole = userViewModel.getUserRole()
            
            // Point 3: Guest Security. Prevent rental if user is guest or not logged in properly.
            if (userEmail.isEmpty() || userEmail == "guest@kendaraiin.com") {
                Toast.makeText(context, getString(R.string.error_must_login), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (vehicleId != -1) {
                eventViewModel.rentVehicle(vehicleId, userEmail, startDate, duration, pickup)
            }
        }
    }

    private fun setupDatePicker(editText: EditText) {
        // Point 6: Prevent manual typing
        editText.isFocusable = false
        editText.isClickable = true
        
        val showDialog = {
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
            
            datePickerDialog.datePicker.minDate = calendar.timeInMillis
            datePickerDialog.show()
        }
        
        editText.setOnClickListener { showDialog() }
    }
}
