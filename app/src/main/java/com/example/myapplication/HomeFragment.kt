package com.example.myapplication

import android.app.AlertDialog
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
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.databinding.DialogFilterBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    // Filter state
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
                    currentDialogImageView?.setImageURI(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = ViewModelFactory(requireContext())
        viewModel = ViewModelProvider(requireActivity(), factory)[EventViewModel::class.java]

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", getString(R.string.guest_name))
        val userRole = sharedPref.getString("user_role", "Customer")
        userEmail = sharedPref.getString("user_email", "") ?: ""
        isAdmin = userRole == "Admin"

        setupUI(userName)
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
            binding.tvListTitle.text = getString(R.string.home_admin_list_title)
            binding.btnQuickAdd.setOnClickListener { showAddEditDialog() }
        } else {
            binding.layoutAdminDashboard.visibility = View.GONE
            binding.cvFilterHome.visibility = View.VISIBLE
            binding.tvHeading.text = getString(R.string.home_heading)
            binding.tvListTitle.text = getString(R.string.home_recommendation_user)
            binding.cvFilterHome.setOnClickListener { showFilterDialog() }
        }
        
        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.navigation_list)
        }
    }

    private fun showFilterDialog() {
        val dialogBinding = DialogFilterBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.chipMobil.isChecked = selectedTypes.contains("Mobil")
        dialogBinding.chipMotor.isChecked = selectedTypes.contains("Motor")
        dialogBinding.chipMatic.isChecked = selectedTransmissions.contains("Matic")
        dialogBinding.chipManual.isChecked = selectedTransmissions.contains("Manual")
        
        dialogBinding.priceSlider.setValues(minPrice, maxPrice)
        updatePriceLabel(dialogBinding.tvPriceRangeValue, minPrice, maxPrice)

        dialogBinding.priceSlider.addOnChangeListener { slider, _, _ ->
            updatePriceLabel(dialogBinding.tvPriceRangeValue, slider.values[0], slider.values[1])
        }

        when (priceSortOrder) {
            "low_to_high" -> dialogBinding.rbPriceLow.isChecked = true
            "high_to_low" -> dialogBinding.rbPriceHigh.isChecked = true
        }

        dialogBinding.btnApply.setOnClickListener {
            selectedTypes.clear()
            if (dialogBinding.chipMobil.isChecked) selectedTypes.add("Mobil")
            if (dialogBinding.chipMotor.isChecked) selectedTypes.add("Motor")

            selectedTransmissions.clear()
            if (dialogBinding.chipMatic.isChecked) selectedTransmissions.add("Matic")
            if (dialogBinding.chipManual.isChecked) selectedTransmissions.add("Manual")

            minPrice = dialogBinding.priceSlider.values[0]
            maxPrice = dialogBinding.priceSlider.values[1]

            priceSortOrder = when (dialogBinding.rgPriceSort.checkedRadioButtonId) {
                R.id.rbPriceLow -> "low_to_high"
                R.id.rbPriceHigh -> "high_to_low"
                else -> null
            }

            updateVehicleList()
            dialog.dismiss()
        }

        dialogBinding.btnReset.setOnClickListener {
            selectedTypes.clear()
            selectedTransmissions.clear()
            priceSortOrder = null
            minPrice = 0f
            maxPrice = 10000000f
            updateVehicleList()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updatePriceLabel(textView: TextView, min: Float, max: Float) {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        textView.text = "${format.format(min.toLong())} - ${format.format(max.toLong())}"
    }

    private fun updateVehicleList() {
        val allEvents = viewModel.events.value ?: emptyList()
        
        var filtered = if (isAdmin) {
            allEvents.filter { it.adminEmail?.trim().equals(userEmail.trim(), ignoreCase = true) }
        } else {
            allEvents.filter { !it.isRegistered }
        }

        if (!isAdmin) {
            if (selectedTypes.isNotEmpty()) {
                filtered = filtered.filter { event ->
                    selectedTypes.any { type -> event.vehicleType?.equals(type, ignoreCase = true) == true }
                }
            }
            if (selectedTransmissions.isNotEmpty()) {
                filtered = filtered.filter { event ->
                    selectedTransmissions.any { trans -> event.transmission?.equals(trans, ignoreCase = true) == true }
                }
            }
            filtered = filtered.filter { event ->
                val price = parsePrice(event.price)
                price >= minPrice && price <= maxPrice
            }
            if (priceSortOrder != null) {
                filtered = when (priceSortOrder) {
                    "low_to_high" -> filtered.sortedBy { parsePrice(it.price) }
                    "high_to_low" -> filtered.sortedByDescending { parsePrice(it.price) }
                    else -> filtered
                }
            }
        }
        
        eventAdapter?.updateData(filtered, isAdmin)

        if (isAdmin) {
            val myUnits = allEvents.filter { it.adminEmail?.trim().equals(userEmail.trim(), ignoreCase = true) }
            binding.tvTotalUnit.text = myUnits.size.toString()
            binding.tvTotalRented.text = myUnits.count { it.effectiveStatus == "approved" || it.effectiveStatus == "pending" }.toString()
        }
    }

    private fun parsePrice(priceStr: String?): Long {
        if (priceStr == null) return 0L
        return try {
            priceStr.replace(Regex("[^0-9]"), "").toLong()
        } catch (e: Exception) {
            0L
        }
    }

    private fun refreshData() {
        viewModel.fetchEventsFromApi(if (isAdmin) userEmail else null)
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(
            events = emptyList(),
            isAdmin = isAdmin, 
            onItemClick = { event -> 
                if (isAdmin) {
                    showAdminOptionsDialog(event)
                } else {
                    val bundle = Bundle().apply { putInt("vehicle_id", event.id) }
                    findNavController().navigate(R.id.navigation_detail, bundle)
                }
            },
            onDeleteClick = { event -> if (isAdmin) showDeleteConfirmation(event) } 
        )
        binding.rvHomeVehicles.adapter = eventAdapter
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) {
            updateVehicleList()
        }
        
        // Poin 3: Menambahkan observasi error
        viewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                errorResId?.let {
                    Toast.makeText(context, getString(it), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAdminOptionsDialog(event: Event) {
        // Poin 1: Perbaikan Menu dan Aksi Admin
        val options = arrayOf(
            getString(R.string.home_see_all), 
            getString(R.string.title_edit_vehicle), 
            "Hapus Kendaraan", 
            getString(R.string.btn_batal)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(event.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.navigation_detail, Bundle().apply { putInt("vehicle_id", event.id) })
                    1 -> showAddEditDialog(event)
                    2 -> showDeleteConfirmation(event)
                    // Index 3 (Batal) otomatis menutup dialog
                }
            }
            .show()
    }

    private fun showAddEditDialog(event: Event? = null) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (event != null) getString(R.string.title_edit_vehicle) else getString(R.string.title_add_vehicle))

        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_add_event, null)
        val etName = layout.findViewById<EditText>(R.id.etName)
        val etPrice = layout.findViewById<EditText>(R.id.etPrice)
        val etDesc = layout.findViewById<EditText>(R.id.etDesc)
        val etSeats = layout.findViewById<EditText>(R.id.etSeats)
        val etLocation = layout.findViewById<EditText>(R.id.etLocation)
        val ivSelectedImage = layout.findViewById<ImageView>(R.id.ivSelectedImage)
        val btnPickImage = layout.findViewById<Button>(R.id.btnPickImage)
        val actvType = layout.findViewById<AutoCompleteTextView>(R.id.actvVehicleType)
        val actvTransmission = layout.findViewById<AutoCompleteTextView>(R.id.actvTransmission)

        actvType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, arrayOf("Motor", "Mobil")))
        actvTransmission.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, arrayOf("Matic", "Manual")))

        currentDialogImageView = ivSelectedImage
        selectedImageUri = null 

        if (event != null) {
            etName.setText(event.name)
            etPrice.setText(event.price)
            etDesc.setText(event.description)
            etSeats.setText(event.seats)
            etLocation.setText(event.location)
            actvType.setText(event.vehicleType, false)
            actvTransmission.setText(event.transmission, false)
            if (!event.imageUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(event.imageUri)
                    ivSelectedImage.setImageURI(uri)
                } catch (e: Exception) {
                    ivSelectedImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }

        btnPickImage.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }

        builder.setView(layout)
        builder.setPositiveButton(getString(R.string.btn_apply)) { _, _ ->
            val name = etName.text.toString().trim()
            val inputPrice = etPrice.text.toString().trim()
            val type = actvType.text.toString().trim()

            // Perbaikan Sinkronisasi Harga: Bersihkan input harga dari karakter non-angka
            val cleanPrice = inputPrice.replace(Regex("[^0-9]"), "")

            if (name.isNotEmpty() && cleanPrice.isNotEmpty() && type.isNotEmpty()) {
                val vehicle = Event(
                    id = event?.id ?: 0,
                    name = name,
                    price = cleanPrice,
                    description = etDesc.text.toString().trim(),
                    isRegistered = event?.isRegistered ?: false,
                    adminEmail = userEmail,
                    imageUri = selectedImageUri?.toString() ?: event?.imageUri ?: "",
                    vehicleType = type,
                    transmission = actvTransmission.text.toString().trim(),
                    seats = etSeats.text.toString().trim(),
                    location = etLocation.text.toString().trim()
                )

                if (event != null) {
                    viewModel.updateEvent(event.id, vehicle) { refreshData() }
                } else {
                    viewModel.addEvent(vehicle) { refreshData() }
                }
            }
        }
        builder.setNegativeButton(getString(R.string.btn_batal), null)
        builder.show()
    }

    private fun showDeleteConfirmation(event: Event) {
        // Poin 2: Perbaikan Judul Dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Kendaraan")
            .setMessage("Apakah Anda yakin ingin menghapus ${event.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteEvent(event.id, userEmail) { refreshData() }
            }
            .setNegativeButton(getString(R.string.btn_batal), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
