package com.example.myapplication

import android.net.Uri
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
import kotlinx.coroutines.launch

class ShopFragment : Fragment() {

    private lateinit var vehicleRepository: EventRepository
    private lateinit var userRepository: UserRepository
    private lateinit var adapter: EventAdapter
    private var adminEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shop, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vehicleRepository = EventRepository(requireContext())
        userRepository = UserRepository(requireContext())

        adminEmail = arguments?.getString("admin_email")

        val btnBack = view.findViewById<ImageView>(R.id.btnBackShop)
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        if (!adminEmail.isNullOrEmpty()) {
            setupShopHeader(view)
            setupRecyclerView(view)
            loadAdminVehicles()
        } else {
            Toast.makeText(context, "Data toko tidak ditemukan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupShopHeader(view: View) {
        val tvName = view.findViewById<TextView>(R.id.tvShopProfileName)
        val tvEmail = view.findViewById<TextView>(R.id.tvShopProfileEmail)

        viewLifecycleOwner.lifecycleScope.launch {
            val admin = userRepository.getUserByEmail(adminEmail!!)
            if (admin != null) {
                tvName.text = admin.name
                tvEmail.text = admin.email
            } else {
                tvName.text = "Penyewa Umum"
                tvEmail.text = adminEmail
            }
        }
    }

    private fun setupRecyclerView(view: View) {
        val rvShop = view.findViewById<RecyclerView>(R.id.rvShopVehicles)
        adapter = EventAdapter(
            events = emptyList(),
            isAdmin = false,
            onItemClick = { event ->
                val bundle = Bundle()
                bundle.putInt("vehicle_id", event.id)
                findNavController().navigate(R.id.navigation_detail, bundle)
            },
            onDeleteClick = {}
        )
        rvShop.adapter = adapter
    }

    private fun loadAdminVehicles() {
        val vehicles = vehicleRepository.getEventsByAdmin(adminEmail!!)
        adapter.updateData(vehicles, false)
    }
}
