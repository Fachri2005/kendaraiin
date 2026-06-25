package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.databinding.DialogFilterBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EventViewModel
    private var eventAdapter: EventAdapter? = null
    
    private var isAdmin: Boolean = false
    private var userEmail: String = ""

    private var selectedImageUri: Uri? = null
    private var currentDialogImageView: ImageView? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<Array<String>>
    private var updateJob: Job? = null

    private val idLocale = Locale("id", "ID")
    private val currencyFormat = NumberFormat.getCurrencyInstance(idLocale).apply { maximumFractionDigits = 0 }

    private var selectedTypes = mutableSetOf<String>()
    private var selectedTransmissions = mutableSetOf<String>()
    private var priceSortOrder: String? = null
    private var minPrice: Float = 0f
    private var maxPrice: Float = 10000000f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)
                    selectedImageUri = it
                    currentDialogImageView?.let { iv -> 
                        Glide.with(iv).load(it).override(500).into(iv) 
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory = ViewModelFactory(requireContext())
        viewModel = ViewModelProvider(requireActivity(), factory)[EventViewModel::class.java]

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("user_email", "") ?: ""
        isAdmin = sharedPref.getString("user_role", "Customer") == "Admin"

        setupUI(sharedPref.getString("user_name", "User"))
        setupRecyclerView()
        observeViewModel()
        refreshData()
    }

    private fun setupUI(userName: String?) {
        binding.tvWelcome.text = getString(R.string.home_welcome, userName)
        if (isAdmin) {
            binding.layoutAdminDashboard.visibility = View.VISIBLE
            binding.cvFilterHome.visibility = View.GONE
            binding.tvHeading.text = getString(R.string.home_heading_admin)
            binding.btnQuickAdd.setOnClickListener { showAddEditDialog() }
        } else {
            binding.layoutAdminDashboard.visibility = View.GONE
            binding.cvFilterHome.visibility = View.VISIBLE
            binding.cvFilterHome.setOnClickListener { showFilterDialog() }
        }
        binding.tvSeeAll.setOnClickListener { findNavController().navigate(R.id.navigation_list) }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(
            isAdmin = isAdmin, 
            onItemClick = { event -> 
                if (isAdmin) showAdminOptionsDialog(event) 
                else findNavController().navigate(R.id.navigation_detail, Bundle().apply { putInt("vehicle_id", event.id) })
            },
            onDeleteClick = { event -> if (isAdmin) showDeleteConfirmation(event) }
        )
        binding.rvHomeVehicles.adapter = eventAdapter
    }

    private fun updateVehicleList() {
        if (_binding == null) return
        updateJob?.cancel() // HENTIKAN proses lama agar tidak LAG
        
        val allEvents = viewModel.events.value ?: emptyList()
        updateJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            var filtered = allEvents.filter { 
                if (isAdmin) it.adminEmail?.trim().equals(userEmail.trim(), ignoreCase = true) 
                else !it.isRegistered 
            }

            if (!isAdmin) {
                if (selectedTypes.isNotEmpty()) filtered = filtered.filter { e -> selectedTypes.any { t -> e.vehicleType?.equals(t, ignoreCase = true) == true } }
                if (selectedTransmissions.isNotEmpty()) filtered = filtered.filter { e -> selectedTransmissions.any { tr -> e.transmission?.equals(tr, ignoreCase = true) == true } }
                filtered = filtered.filter { it.numericPrice >= minPrice && it.numericPrice <= maxPrice }
                priceSortOrder?.let { order -> filtered = if (order == "low_to_high") filtered.sortedBy { it.numericPrice } else filtered.sortedByDescending { it.numericPrice } }
            }

            val rentedCount = if (isAdmin) filtered.count { it.effectiveStatus == "approved" || it.effectiveStatus == "pending" } else 0

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    eventAdapter?.updateData(filtered, isAdmin)
                    if (isAdmin) {
                        binding.tvTotalUnit.text = filtered.size.toString()
                        binding.tvTotalRented.text = rentedCount.toString()
                    }
                }
            }
        }
    }

    private fun showAddEditDialog(event: Event? = null) {
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)
        val etName = layout.findViewById<EditText>(R.id.etName)
        val etPrice = layout.findViewById<EditText>(R.id.etPrice)
        val etDesc = layout.findViewById<EditText>(R.id.etDesc)
        val etSeats = layout.findViewById<EditText>(R.id.etSeats)
        val etLocation = layout.findViewById<EditText>(R.id.etLocation)
        val ivSelectedImage = layout.findViewById<ImageView>(R.id.ivSelectedImage)
        val actvType = layout.findViewById<AutoCompleteTextView>(R.id.actvVehicleType)
        val actvTransmission = layout.findViewById<AutoCompleteTextView>(R.id.actvTransmission)

        actvType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, arrayOf("Motor", "Mobil")))
        actvTransmission.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, arrayOf("Matic", "Manual")))

        currentDialogImageView = ivSelectedImage
        selectedImageUri = null 

        if (event != null) {
            etName.setText(event.name); etPrice.setText(event.price); etDesc.setText(event.description)
            etSeats.setText(event.seats); etLocation.setText(event.location)
            actvType.setText(event.vehicleType, false); actvTransmission.setText(event.transmission, false)
            if (!event.imageUri.isNullOrEmpty()) {
                Glide.with(ivSelectedImage).load(event.imageUri).override(500).into(ivSelectedImage)
            }
        }

        layout.findViewById<Button>(R.id.btnPickImage).setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }

        MaterialAlertDialogBuilder(requireActivity()) // Gunakan activity agar lebih stabil
            .setTitle(if (event != null) getString(R.string.title_edit_vehicle) else getString(R.string.title_add_vehicle))
            .setView(layout)
            .setPositiveButton(getString(R.string.btn_apply)) { _, _ ->
                val name = etName.text.toString().trim()
                val price = etPrice.text.toString().trim().replace(Regex("[^0-9]"), "")
                if (name.isNotEmpty() && price.isNotEmpty()) {
                    val vehicle = Event(
                        id = event?.id ?: 0, name = name, price = price, 
                        description = etDesc.text.toString().trim(), seats = etSeats.text.toString().trim(),
                        location = etLocation.text.toString().trim(), vehicleType = actvType.text.toString(),
                        transmission = actvTransmission.text.toString(),
                        adminEmail = userEmail, imageUri = selectedImageUri?.toString() ?: event?.imageUri ?: ""
                    )
                    if (event != null) viewModel.updateEvent(event.id, vehicle) {} 
                    else viewModel.addEvent(vehicle) {}
                }
            }
            .setNegativeButton(getString(R.string.btn_batal), null)
            .setOnDismissListener { currentDialogImageView = null }
            .show()
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { updateVehicleList() }
        viewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                // FIX BUG BLANK SCREEN: Pastikan errorResId tidak null sebelum panggil getString
                if (errorResId != null && isAdded) {
                    Toast.makeText(requireContext(), getString(errorResId), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showFilterDialog() {
        val dialogBinding = DialogFilterBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogBinding.root).create()
        dialogBinding.chipMobil.isChecked = selectedTypes.contains("Mobil")
        dialogBinding.chipMotor.isChecked = selectedTypes.contains("Motor")
        dialogBinding.priceSlider.setValues(minPrice, maxPrice)
        updatePriceLabel(dialogBinding.tvPriceRangeValue, minPrice, maxPrice)
        dialogBinding.priceSlider.addOnChangeListener { slider, _, _ -> updatePriceLabel(dialogBinding.tvPriceRangeValue, slider.values[0], slider.values[1]) }
        dialogBinding.btnApply.setOnClickListener {
            selectedTypes.clear(); if (dialogBinding.chipMobil.isChecked) selectedTypes.add("Mobil"); if (dialogBinding.chipMotor.isChecked) selectedTypes.add("Motor")
            minPrice = dialogBinding.priceSlider.values[0]; maxPrice = dialogBinding.priceSlider.values[1]
            priceSortOrder = if (dialogBinding.rbPriceLow.isChecked) "low_to_high" else if (dialogBinding.rbPriceHigh.isChecked) "high_to_low" else null
            updateVehicleList(); dialog.dismiss()
        }
        dialogBinding.btnReset.setOnClickListener { 
            selectedTypes.clear(); selectedTransmissions.clear(); priceSortOrder = null; minPrice = 0f; maxPrice = 10000000f
            updateVehicleList(); dialog.dismiss() 
        }
        dialog.show()
    }

    private fun updatePriceLabel(textView: TextView, min: Float, max: Float) {
        textView.text = "${currencyFormat.format(min.toLong())} - ${currencyFormat.format(max.toLong())}"
    }

    private fun refreshData() = viewModel.fetchEventsFromApi(if (isAdmin) userEmail else null)
    
    private fun showAdminOptionsDialog(event: Event) {
        val options = arrayOf(getString(R.string.home_see_all), getString(R.string.title_edit_vehicle), "Hapus", getString(R.string.btn_batal))
        MaterialAlertDialogBuilder(requireContext()).setItems(options) { _, w ->
            when (w) {
                0 -> findNavController().navigate(R.id.navigation_detail, Bundle().apply { putInt("vehicle_id", event.id) })
                1 -> showAddEditDialog(event)
                2 -> showDeleteConfirmation(event)
            }
        }.show()
    }
    private fun showDeleteConfirmation(event: Event) {
        MaterialAlertDialogBuilder(requireContext()).setMessage("Hapus ${event.name}?").setPositiveButton("Hapus") { _, _ -> viewModel.deleteEvent(event.id, userEmail) { refreshData() } }.show()
    }
    override fun onDestroyView() { 
        updateJob?.cancel()
        super.onDestroyView()
        _binding = null 
    }
}
