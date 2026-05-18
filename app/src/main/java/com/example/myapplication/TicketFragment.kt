package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class TicketFragment : Fragment() {

    private lateinit var viewModel: EventViewModel
    private lateinit var adapter: EventAdapter
    private lateinit var rvTicket: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
    private var isAdmin: Boolean = false
    private var userEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ticket, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = EventRepository(requireContext())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(requireActivity(), factory)[EventViewModel::class.java]

        rvTicket = view.findViewById(R.id.rvTicket)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = view.findViewById(R.id.tvEmptyDesc)
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userRole = sharedPref.getString("user_role", "Customer")
        userEmail = sharedPref.getString("user_email", "") ?: ""
        isAdmin = userRole == "Admin"

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarTicket)
        if (isAdmin) {
            toolbar.title = "Unit Tersewa"
            tvEmptyTitle.text = "Belum Ada Unit Tersewa"
            tvEmptyDesc.text = "Daftar unit milik Anda yang sedang disewa pelanggan akan muncul di sini."
        } else {
            toolbar.title = "Riwayat Sewa"
            tvEmptyTitle.text = "Belum Ada Riwayat"
            tvEmptyDesc.text = "Riwayat sewa kendaraan kamu akan muncul di sini."
        }

        setupRecyclerView()
        observeViewModel()

        swipeRefresh.setOnRefreshListener {
            viewModel.fetchEventsFromApi()
        }
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
        rvTicket.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            val filteredList = if (isAdmin) {
                // Admin: Lihat unit MILIKNYA yang SEDANG DISEWA (isRegistered = true)
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
                layoutEmpty.visibility = View.VISIBLE
                rvTicket.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                rvTicket.visibility = View.VISIBLE
                adapter.updateData(filteredList, isAdmin, true)
            }
            swipeRefresh.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!swipeRefresh.isRefreshing) {
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
}
