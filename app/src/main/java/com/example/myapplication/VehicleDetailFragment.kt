package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class VehicleDetailFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(requireContext()) }
    private val eventViewModel: EventViewModel by viewModels { ViewModelFactory(requireContext()) }
    
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
        toolbar?.setNavigationOnClickListener { findNavController().navigateUp() }

        if (vehicleId != -1) {
            loadVehicleData(view)
        }
        
        setupObservers()
    }

    private fun setupObservers() {
        eventViewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                // Fix: removed unnecessary safe call on errorResId (Int)
                Toast.makeText(context, getString(errorResId), Toast.LENGTH_SHORT).show()
            }
        }

        eventViewModel.rentalSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    findNavController().navigateUp()
                }
            }
        }
        
        eventViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            view?.findViewById<View>(R.id.btnApproveAdmin)?.isEnabled = !isLoading
            view?.findViewById<View>(R.id.btnRejectAdmin)?.isEnabled = !isLoading
        }
    }

    private fun loadVehicleData(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = vehicleRepository.getEventByIdFromApi(vehicleId)
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { displayVehicle(view, it) }
                } else {
                    vehicleRepository.getEventById(vehicleId)?.let { displayVehicle(view, it) }
                }
            } catch (e: Exception) {
                vehicleRepository.getEventById(vehicleId)?.let { displayVehicle(view, it) }
            }
        }
    }

    private fun displayVehicle(view: View, vehicle: Event) {
        val ivDetail = view.findViewById<ImageView>(R.id.ivVehicleDetail)
        val tvName = view.findViewById<TextView>(R.id.tvVehicleName)
        val tvPrice = view.findViewById<TextView>(R.id.tvVehiclePrice)
        val btnSewa = view.findViewById<Button>(R.id.btnSewaDetail)
        val tvStatusInfo = view.findViewById<TextView>(R.id.tvRentalStatusInfo)
        val layoutAdminActions = view.findViewById<LinearLayout>(R.id.layoutAdminActions)
        val btnApprove = view.findViewById<Button>(R.id.btnApproveAdmin)
        val btnReject = view.findViewById<Button>(R.id.btnRejectAdmin)
        val context = requireContext()

        tvName?.text = vehicle.name ?: getString(R.string.unknown_name)
        
        val rawPrice = vehicle.price?.replace(Regex("[^0-9]"), "") ?: "0"
        tvPrice?.text = getString(R.string.price_per_day_format, rawPrice)
        
        view.findViewById<TextView>(R.id.tvVehicleDesc)?.text = vehicle.description
        view.findViewById<TextView>(R.id.tvDetailType)?.text = vehicle.vehicleType
        view.findViewById<TextView>(R.id.tvDetailTrans)?.text = vehicle.transmission
        view.findViewById<TextView>(R.id.tvDetailSeats)?.text = getString(R.string.seats_format, vehicle.seats ?: "2")
        view.findViewById<TextView>(R.id.tvDetailLocation)?.text = vehicle.location

        if (!vehicle.imageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(vehicle.imageUri)
                context.contentResolver.openInputStream(uri)?.use {
                    ivDetail?.setImageURI(uri)
                }
            } catch (e: Exception) {
                ivDetail?.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        loadShopInfo(view, vehicle.adminEmail)

        val currentUserRole = userViewModel.getUserRole() ?: "Customer"
        val currentUserEmail = userViewModel.getUserEmail() ?: ""
        val isAdmin = currentUserRole.equals("Admin", ignoreCase = true)
        val isRenter = vehicle.renterEmail?.trim().equals(currentUserEmail.trim(), ignoreCase = true)

        val status = vehicle.effectiveStatus

        btnSewa?.visibility = View.GONE
        tvStatusInfo?.visibility = View.GONE
        layoutAdminActions?.visibility = View.GONE

        if (status.isEmpty() || status == "canceled") {
            btnSewa?.visibility = View.VISIBLE
            btnSewa?.text = getString(R.string.btn_sewa_now)
            btnSewa?.isEnabled = true
            btnSewa?.setOnClickListener {
                if (userViewModel.isLoggedIn()) {
                    val bundle = Bundle().apply {
                        putInt("vehicle_id", vehicle.id)
                        putString("vehicle_name", vehicle.name)
                        putString("vehicle_price", vehicle.price)
                    }
                    findNavController().navigate(R.id.navigation_rental_form, bundle)
                } else {
                    Toast.makeText(context, getString(R.string.error_must_login), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            tvStatusInfo?.visibility = View.VISIBLE
            
            val statusDisplay = when (status) {
                "pending" -> getString(R.string.status_pending)
                "approved" -> getString(R.string.status_approved)
                "canceled" -> getString(R.string.status_canceled)
                "completed" -> getString(R.string.status_completed)
                else -> status.uppercase()
            }
            tvStatusInfo?.text = getString(R.string.label_rental_status, statusDisplay)

            if (status == "pending") {
                if (isAdmin) {
                    layoutAdminActions?.visibility = View.VISIBLE
                    btnApprove?.setOnClickListener {
                        eventViewModel.updateRentalStatus(vehicle.id, "approved", currentUserEmail)
                    }
                    btnReject?.setOnClickListener {
                        eventViewModel.updateRentalStatus(vehicle.id, "canceled", currentUserEmail)
                    }
                } else if (isRenter) {
                    btnSewa?.visibility = View.VISIBLE
                    btnSewa?.text = getString(R.string.btn_cancel_rental)
                    btnSewa?.setOnClickListener {
                        eventViewModel.updateRentalStatus(vehicle.id, "canceled", null)
                    }
                } else {
                    btnSewa?.visibility = View.VISIBLE
                    btnSewa?.text = getString(R.string.status_processing)
                    btnSewa?.isEnabled = false
                }
            } else if (status == "approved") {
                btnSewa?.visibility = View.VISIBLE
                btnSewa?.text = getString(R.string.status_rented)
                btnSewa?.isEnabled = false
            }
        }
    }

    private fun loadShopInfo(view: View, adminEmail: String?) {
        val tvShopName = view.findViewById<TextView>(R.id.tvShopName)
        val btnVisitShop = view.findViewById<Button>(R.id.btnVisitShop)

        if (!adminEmail.isNullOrEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                val admin = userRepository.getUserByEmail(adminEmail)
                if (admin != null) {
                    tvShopName?.text = admin.name
                    btnVisitShop?.setOnClickListener {
                        val bundle = Bundle().apply {
                            putString("admin_email", admin.email)
                        }
                        findNavController().navigate(R.id.navigation_shop, bundle)
                    }
                } else {
                    tvShopName?.text = getString(R.string.general_renter)
                }
            }
        }
    }
}
