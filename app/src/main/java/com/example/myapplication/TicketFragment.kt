package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentTicketBinding

class TicketFragment : Fragment() {

    private var _binding: FragmentTicketBinding? = null
    private val binding get() = _binding!!

    // Menggunakan ViewModel dengan factory untuk sinkronisasi data
    private val eventViewModel: EventViewModel by viewModels { ViewModelFactory(requireContext()) }
    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(requireContext()) }
    
    private lateinit var adapter: EventAdapter
    private var isAdmin: Boolean = false
    private var userEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data user dari UserViewModel (Session)
        val userRole = userViewModel.getUserRole() ?: "Customer"
        userEmail = userViewModel.getUserEmail() ?: ""
        isAdmin = userRole == "Admin"

        // Update UI berdasarkan Role
        if (isAdmin) {
            binding.toolbarTicket.title = "Unit Tersewa"
            binding.tvEmptyTitle.text = "Belum Ada Unit Tersewa"
            binding.tvEmptyDesc.text = "Daftar unit milik Anda yang sedang disewa pelanggan akan muncul di sini."
        } else {
            binding.toolbarTicket.title = "Riwayat Sewa"
            binding.tvEmptyTitle.text = "Belum Ada Riwayat"
            binding.tvEmptyDesc.text = "Riwayat sewa kendaraan kamu akan muncul di sini."
        }

        setupRecyclerView()
        observeViewModel()

        // Panggil fetch data secara otomatis saat fragment dibuka
        loadData()

        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun loadData() {
        // Ambil data terbaru dari server XAMPP
        eventViewModel.fetchEventsFromApi(if (isAdmin) userEmail else null)
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(
            events = emptyList(),
            isAdmin = isAdmin,
            isHistory = true,
            onItemClick = { event ->
                val bundle = Bundle().apply { putInt("vehicle_id", event.id) }
                findNavController().navigate(R.id.navigation_detail, bundle)
            },
            onDeleteClick = {}
        )
        binding.rvTicket.adapter = adapter
    }

    private fun observeViewModel() {
        eventViewModel.events.observe(viewLifecycleOwner) { events ->
            val filteredList = if (isAdmin) {
                // Admin: Lihat unit MILIKNYA yang SEDANG DISEWA
                events.filter { 
                    it.adminEmail?.trim().equals(userEmail.trim(), ignoreCase = true) && it.isRegistered 
                }
            } else {
                // Customer: Lihat unit yang PERNAH/SEDANG DIA SEWA
                events.filter { 
                    it.renterEmail?.trim().equals(userEmail.trim(), ignoreCase = true) 
                }
            }

            if (filteredList.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvTicket.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvTicket.visibility = View.VISIBLE
                adapter.updateData(filteredList, isAdmin, true)
            }
            binding.swipeRefresh.isRefreshing = false
        }

        eventViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!binding.swipeRefresh.isRefreshing) {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        eventViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
