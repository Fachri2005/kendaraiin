package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    private lateinit var viewModel: EventViewModel
    private lateinit var repository: EventRepository
    private var eventAdapter: EventAdapter? = null
    private lateinit var rvHomeVehicles: RecyclerView
    
    private var tvTotalUnit: TextView? = null
    private var tvTotalRented: TextView? = null
    
    private var isAdmin: Boolean = false
    private var userEmail: String = ""

    private var selectedImageUri: Uri? = null
    private var currentDialogImageView: ImageView? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<Array<String>>

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
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())
        val factory = ViewModelFactory(repository)
        
        // PERBAIKAN: Gunakan requireActivity() agar data tidak hilang saat pindah fragment
        viewModel = ViewModelProvider(requireActivity(), factory)[EventViewModel::class.java]

        rvHomeVehicles = view.findViewById(R.id.rvHomeVehicles)
        tvTotalUnit = view.findViewById(R.id.tvTotalUnit)
        tvTotalRented = view.findViewById(R.id.tvTotalRented)
        
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvHeading = view.findViewById<TextView>(R.id.tvHeading)
        val layoutAdmin = view.findViewById<LinearLayout>(R.id.layoutAdminDashboard)
        val layoutPromo = view.findViewById<LinearLayout>(R.id.layoutPromoAndCategory)
        val btnQuickAdd = view.findViewById<Button>(R.id.btnQuickAdd)
        val tvListTitle = view.findViewById<TextView>(R.id.tvListTitle)
        val etSearchHome = view.findViewById<EditText>(R.id.etSearchHome)

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", "Pengguna")
        val userRole = sharedPref.getString("user_role", "Customer")
        userEmail = sharedPref.getString("user_email", "") ?: ""
        isAdmin = userRole == "Admin"

        tvWelcome.text = "Halo, $userName!"

        if (isAdmin) {
            layoutAdmin.visibility = View.VISIBLE
            layoutPromo.visibility = View.GONE
            tvHeading.text = "Kelola bisnis\nkendaraan Anda"
            tvListTitle.text = "Manajemen Unit Anda"
            btnQuickAdd.setOnClickListener { showAddEditDialog() }
            etSearchHome.hint = "Cari unit Anda..."
        } else {
            layoutAdmin.visibility = View.GONE
            layoutPromo.visibility = View.VISIBLE
            tvHeading.text = "Sewa kendaraan\nimpianmu hari ini"
            tvListTitle.text = "Rekomendasi Untukmu"
            etSearchHome.isFocusable = false
            etSearchHome.setOnClickListener { findNavController().navigate(R.id.navigation_list) }
        }

        // 1. SETUP RECYCLER VIEW (Langsung isi data jika sudah ada di ViewModel)
        setupRecyclerView(etSearchHome.text.toString())

        // 2. OBSERVE LIVE DATA
        viewModel.events.observe(viewLifecycleOwner) {
            updateVehicleList(etSearchHome.text.toString())
        }

        // 3. LOAD DATA DARI API (Hanya jika belum pernah dimuat)
        if (viewModel.events.value.isNullOrEmpty()) {
            viewModel.fetchEventsFromApi()
        }

        etSearchHome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateVehicleList(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView(searchQuery: String) {
        if (eventAdapter == null) {
            val allEvents = viewModel.events.value ?: emptyList()
            val initialFiltered = allEvents.filter { event ->
                val matchesRole = if (isAdmin) event.adminEmail?.trim().equals(userEmail.trim(), ignoreCase = true) else true
                val matchesQuery = event.name.contains(searchQuery, ignoreCase = true)
                matchesRole && matchesQuery
            }

            eventAdapter = EventAdapter(
                events = initialFiltered,
                isAdmin = isAdmin, 
                onItemClick = { event -> 
                    val bundle = Bundle().apply { putInt("vehicle_id", event.id) }
                    if (isAdmin) {
                        showAdminOptionsDialog(event)
                    } else {
                        findNavController().navigate(R.id.navigation_detail, bundle)
                    }
                },
                onDeleteClick = { event -> if (isAdmin) showDeleteConfirmation(event) } 
            )
        }
        rvHomeVehicles.adapter = eventAdapter
        
        // Update stats jika admin
        if (isAdmin) {
            val allEvents = viewModel.events.value ?: emptyList()
            val myUnits = allEvents.filter { it.adminEmail?.trim().equals(userEmail.trim(), ignoreCase = true) }
            tvTotalUnit?.text = myUnits.size.toString()
            tvTotalRented?.text = myUnits.count { it.isRegistered }.toString()
        }
    }

    private fun updateVehicleList(query: String) {
        val allEvents = viewModel.events.value ?: emptyList()
        val filtered = allEvents.filter { event ->
            val matchesRole = if (isAdmin) event.adminEmail?.trim().equals(userEmail.trim(), ignoreCase = true) else true
            val matchesQuery = event.name.contains(query, ignoreCase = true)
            matchesRole && matchesQuery
        }
        eventAdapter?.updateData(filtered, isAdmin)
    }

    private fun showAdminOptionsDialog(event: Event) {
        val options = arrayOf("Lihat Detail (Preview)", "Edit Data Kendaraan", "Hapus Kendaraan")
        AlertDialog.Builder(requireContext())
            .setTitle("Opsi Unit: ${event.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val bundle = Bundle().apply { putInt("vehicle_id", event.id) }
                        findNavController().navigate(R.id.navigation_detail, bundle)
                    }
                    1 -> showAddEditDialog(event)
                    2 -> showDeleteConfirmation(event)
                }
            }
            .show()
    }

    private fun showAddEditDialog(event: Event? = null) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (event != null) "Edit Kendaraan" else "Tambah Kendaraan Baru")

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
                selectedImageUri = Uri.parse(event.imageUri)
                ivSelectedImage.setImageURI(selectedImageUri)
            }
        }

        btnPickImage.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }

        builder.setView(layout)
        builder.setPositiveButton("Simpan") { _, _ ->
            val name = etName.text.toString().trim()
            val price = etPrice.text.toString().trim()
            val type = actvType.text.toString().trim()

            if (name.isNotEmpty() && price.isNotEmpty() && type.isNotEmpty()) {
                val vehicle = Event(
                    id = event?.id ?: 0,
                    name = name,
                    price = price,
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
                    viewModel.updateEvent(event.id, vehicle) { viewModel.fetchEventsFromApi() }
                } else {
                    viewModel.addEvent(vehicle) { viewModel.fetchEventsFromApi() }
                }
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun showDeleteConfirmation(event: Event) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Kendaraan")
            .setMessage("Apakah Anda yakin ingin menghapus ${event.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteEvent(event.id) { viewModel.fetchEventsFromApi() }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
