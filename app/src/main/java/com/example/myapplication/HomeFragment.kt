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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    private lateinit var repository: EventRepository
    private lateinit var adapter: EventAdapter
    private lateinit var rvHomeVehicles: RecyclerView
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
        rvHomeVehicles = view.findViewById(R.id.rvHomeVehicles)
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvHeading = view.findViewById<TextView>(R.id.tvHeading)
        
        // Dashboard views
        val layoutAdmin = view.findViewById<LinearLayout>(R.id.layoutAdminDashboard)
        val layoutPromo = view.findViewById<LinearLayout>(R.id.layoutPromoAndCategory)
        val tvTotalUnit = view.findViewById<TextView>(R.id.tvTotalUnit)
        val tvTotalRented = view.findViewById<TextView>(R.id.tvTotalRented)
        val btnQuickAdd = view.findViewById<Button>(R.id.btnQuickAdd)
        val tvListTitle = view.findViewById<TextView>(R.id.tvListTitle)
        val etSearchHome = view.findViewById<EditText>(R.id.etSearchHome)

        // Get user session
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", "Pengguna")
        val userRole = sharedPref.getString("user_role", "Customer")
        userEmail = sharedPref.getString("user_email", "") ?: ""
        isAdmin = userRole == "Admin"

        tvWelcome.text = "Halo, $userName!"

        // Toggle UI based on Role
        if (isAdmin) {
            layoutAdmin.visibility = View.VISIBLE
            layoutPromo.visibility = View.GONE
            tvHeading.text = "Kelola bisnis\nkendaraan Anda"
            tvListTitle.text = "Manajemen Unit Anda"
            
            updateAdminStats(tvTotalUnit, tvTotalRented)
            
            btnQuickAdd.setOnClickListener {
                showAddEditDialog()
            }

            etSearchHome.hint = "Cari unit Anda..."
        } else {
            layoutAdmin.visibility = View.GONE
            layoutPromo.visibility = View.VISIBLE
            tvHeading.text = "Sewa kendaraan\nimpianmu hari ini"
            tvListTitle.text = "Rekomendasi Untukmu"
            
            // For Customer, search bar can navigate to Search page
            etSearchHome.setOnClickListener {
                findNavController().navigate(R.id.navigation_list)
            }
        }

        // Search filtering logic for Home
        etSearchHome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    filterVehicles(query)
                } else {
                    loadVehicles()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupRecyclerView()
        loadVehicles()
    }

    private fun filterVehicles(query: String) {
        val allResults = repository.searchVehicles(query)
        val filtered = if (isAdmin) {
            allResults.filter { it.adminEmail == userEmail }
        } else {
            allResults
        }
        adapter.updateData(filtered, isAdmin)
    }

    private fun updateAdminStats(tvTotalUnit: TextView, tvTotalRented: TextView) {
        val adminVehicles = repository.getEventsByAdmin(userEmail)
        tvTotalUnit.text = adminVehicles.size.toString()
        tvTotalRented.text = adminVehicles.count { it.isRegistered }.toString()
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(
            events = emptyList(),
            isAdmin = isAdmin, 
            onItemClick = { event -> 
                if (isAdmin && event.adminEmail == userEmail) {
                    showAdminOptionsDialog(event)
                } else {
                    val bundle = Bundle()
                    bundle.putInt("vehicle_id", event.id)
                    findNavController().navigate(R.id.navigation_detail, bundle)
                }
            },
            onDeleteClick = { event ->
                if (isAdmin && event.adminEmail == userEmail) {
                    showDeleteConfirmation(event)
                }
            } 
        )
        rvHomeVehicles.adapter = adapter
    }

    private fun loadVehicles() {
        val events = if (isAdmin) {
            repository.getEventsByAdmin(userEmail)
        } else {
            repository.getAllEvents()
        }
        adapter.updateData(events, isAdmin)
    }

    private fun showAdminOptionsDialog(event: Event) {
        val options = arrayOf("Lihat Detail (Preview)", "Edit Data Kendaraan", "Hapus Kendaraan")
        AlertDialog.Builder(requireContext())
            .setTitle("Opsi Admin: ${event.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val bundle = Bundle()
                        bundle.putInt("vehicle_id", event.id)
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

        val types = arrayOf("Motor", "Mobil")
        actvType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types))
        val transmissions = arrayOf("Matic", "Manual")
        actvTransmission.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, transmissions))

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

        btnPickImage.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        builder.setView(layout)
        builder.setPositiveButton("Simpan") { _, _ ->
            val name = etName.text.toString().trim()
            val price = etPrice.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val seats = etSeats.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val type = actvType.text.toString().trim()
            val transmission = actvTransmission.text.toString().trim()
            val imageUriString = selectedImageUri?.toString() ?: event?.imageUri ?: ""

            if (name.isNotEmpty() && price.isNotEmpty() && type.isNotEmpty()) {
                if (event != null) {
                    repository.updateEvent(event.id, name, price, desc, imageUriString, type, transmission, seats, location)
                    Toast.makeText(context, "Kendaraan diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    repository.addEvent(name, price, desc, userEmail, imageUriString, type, transmission, seats, location)
                    Toast.makeText(context, "Kendaraan ditambahkan!", Toast.LENGTH_SHORT).show()
                }
                loadVehicles()
                // Update stats after adding/editing
                val tvTotalUnit = view?.findViewById<TextView>(R.id.tvTotalUnit)
                val tvTotalRented = view?.findViewById<TextView>(R.id.tvTotalRented)
                if (tvTotalUnit != null && tvTotalRented != null) {
                    updateAdminStats(tvTotalUnit, tvTotalRented)
                }
            } else {
                Toast.makeText(context, "Nama, Harga, dan Tipe wajib diisi!", Toast.LENGTH_SHORT).show()
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
                repository.deleteEvent(event.id)
                loadVehicles()
                val tvTotalUnit = view?.findViewById<TextView>(R.id.tvTotalUnit)
                val tvTotalRented = view?.findViewById<TextView>(R.id.tvTotalRented)
                if (tvTotalUnit != null && tvTotalRented != null) {
                    updateAdminStats(tvTotalUnit, tvTotalRented)
                }
                Toast.makeText(context, "Kendaraan dihapus!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}