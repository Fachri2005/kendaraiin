package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShopFragment : Fragment() {

    private lateinit var vehicleRepository: EventRepository
    private lateinit var userRepository: UserRepository
    private lateinit var adapter: EventAdapter
    private var adminEmail: String? = null
    private var tvProductCount: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shop, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vehicleRepository = EventRepository(requireContext())
        userRepository = UserRepository(requireContext())
        adminEmail = arguments?.getString("admin_email")
        tvProductCount = view.findViewById(R.id.tvShopProductCount)

        view.findViewById<ImageView>(R.id.btnBackShop).setOnClickListener { findNavController().navigateUp() }

        if (!adminEmail.isNullOrEmpty()) {
            setupShopHeader()
            setupRecyclerView(view)
            loadAdminVehicles()
        } else {
            Toast.makeText(context, "Data toko tidak ditemukan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupShopHeader() {
        val tvName = view?.findViewById<TextView>(R.id.tvShopProfileName)
        val tvEmail = view?.findViewById<TextView>(R.id.tvShopProfileEmail)

        viewLifecycleOwner.lifecycleScope.launch {
            val admin = userRepository.getUserByEmail(adminEmail ?: return@launch)
            if (admin != null && isAdded) {
                tvName?.text = admin.name
                tvEmail?.text = admin.email
            } else if (isAdded) {
                tvName?.text = "Penyewa Umum"
                tvEmail?.text = adminEmail
            }
        }
    }

    private fun setupRecyclerView(view: View) {
        val rvShop = view.findViewById<RecyclerView>(R.id.rvShopVehicles)
        // FIX: Hapus parameter 'events' karena kita menggunakan ListAdapter sekarang
        adapter = EventAdapter(
            isAdmin = false,
            onItemClick = { event ->
                findNavController().navigate(R.id.navigation_detail, Bundle().apply { putInt("vehicle_id", event.id) })
            },
            onDeleteClick = {}
        )
        rvShop.adapter = adapter
    }

    private fun loadAdminVehicles() {
        viewLifecycleOwner.lifecycleScope.launch {
            // FIX: Gunakan background thread untuk ambil data DB agar tidak LAG
            val vehicles = vehicleRepository.getEventsByAdmin(adminEmail ?: return@launch)
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    adapter.updateData(vehicles, false)
                    tvProductCount?.text = vehicles.size.toString()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tvProductCount = null
    }
}
