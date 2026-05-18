package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class VehicleDetailFragment : Fragment() {

    private lateinit var vehicleRepository: EventRepository
    private lateinit var userRepository: UserRepository
    private var vehicleId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vehicle_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vehicleRepository = EventRepository(requireContext())
        userRepository = UserRepository(requireContext())

        vehicleId = arguments?.getInt("vehicle_id") ?: -1

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        if (vehicleId != -1) {
            loadVehicleData(view)
        } else {
            Toast.makeText(context, "Data kendaraan tidak ditemukan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun loadVehicleData(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Coba ambil dari API
                val response = vehicleRepository.getEventByIdFromApi(vehicleId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val vehicle = response.body()?.data
                    if (vehicle is Event) { // Pastikan tipenya benar
                        displayVehicle(view, vehicle)
                        return@launch
                    }
                }
                
                // 2. Jika API gagal/null, ambil dari Lokal
                val localVehicle = vehicleRepository.getEventById(vehicleId)
                if (localVehicle != null) {
                    displayVehicle(view, localVehicle)
                } else {
                    Toast.makeText(context, "Gagal memuat data kendaraan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VehicleDetail", "Error: ${e.message}")
                // Cadangan terakhir jika koneksi error
                val localVehicle = vehicleRepository.getEventById(vehicleId)
                if (localVehicle != null) {
                    displayVehicle(view, localVehicle)
                }
            }
        }
    }

    private fun displayVehicle(view: View, vehicle: Event) {
        val ivDetail = view.findViewById<ImageView>(R.id.ivVehicleDetail)
        val tvName = view.findViewById<TextView>(R.id.tvVehicleName)
        val tvPrice = view.findViewById<TextView>(R.id.tvVehiclePrice)
        val tvType = view.findViewById<TextView>(R.id.tvDetailType)
        val tvTrans = view.findViewById<TextView>(R.id.tvDetailTrans)
        val tvSeats = view.findViewById<TextView>(R.id.tvDetailSeats)
        val tvLocation = view.findViewById<TextView>(R.id.tvDetailLocation)
        val tvDesc = view.findViewById<TextView>(R.id.tvVehicleDesc)
        val btnSewa = view.findViewById<Button>(R.id.btnSewaDetail)

        tvName?.text = vehicle.name
        tvPrice?.text = "${vehicle.price} / Hari"
        tvType?.text = vehicle.vehicleType ?: "Mobil"
        tvTrans?.text = vehicle.transmission ?: "Matic"
        tvSeats?.text = vehicle.seats ?: "5 Kursi"
        tvLocation?.text = vehicle.location ?: "Jakarta"
        tvDesc?.text = vehicle.description

        if (!vehicle.imageUri.isNullOrEmpty()) {
            try {
                ivDetail?.setImageURI(Uri.parse(vehicle.imageUri))
            } catch (e: Exception) {
                ivDetail?.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        loadShopInfo(view, vehicle.adminEmail)

        if (vehicle.isRegistered) {
            btnSewa?.text = "Sudah Terdaftar/Disewa"
            btnSewa?.isEnabled = false
            btnSewa?.alpha = 0.5f
        } else {
            btnSewa?.setOnClickListener {
                val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val userEmail = sharedPref.getString("user_email", "")
                
                if (!userEmail.isNullOrEmpty()) {
                    vehicleRepository.setRegistered(vehicle.id, true, userEmail)
                    Toast.makeText(context, "Berhasil menyewa ${vehicle.name}!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadShopInfo(view: View, adminEmail: String?) {
        val tvShopName = view.findViewById<TextView>(R.id.tvShopName)
        val btnVisit = view.findViewById<Button>(R.id.btnVisitShop)

        if (!adminEmail.isNullOrEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                val admin = userRepository.getUserByEmail(adminEmail)
                if (admin != null) {
                    tvShopName?.text = admin.name
                    btnVisit?.setOnClickListener {
                        val bundle = Bundle()
                        bundle.putString("admin_email", adminEmail)
                        findNavController().navigate(R.id.navigation_shop, bundle)
                    }
                } else {
                    tvShopName?.text = "Penyewa Umum"
                    btnVisit?.visibility = View.GONE
                }
            }
        } else {
            tvShopName?.text = "Kendaraiin Official"
            btnVisit?.visibility = View.GONE
        }
    }
}
