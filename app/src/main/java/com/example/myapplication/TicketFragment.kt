package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView

class TicketFragment : Fragment() {

    private lateinit var repository: EventRepository
    private lateinit var adapter: EventAdapter
    private lateinit var rvTicket: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var ivEmptyIcon: ImageView
    
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

        repository = EventRepository(requireContext())
        rvTicket = view.findViewById(R.id.rvTicket)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = view.findViewById(R.id.tvEmptyDesc)
        ivEmptyIcon = view.findViewById(R.id.ivEmptyIcon)

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userRole = sharedPref.getString("user_role", "Admin") // Defaulting to check logic
        val userName = sharedPref.getString("user_name", "Pengguna")
        userEmail = sharedPref.getString("user_email", "") ?: ""
        isAdmin = userRole == "Admin"

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarTicket)
        if (isAdmin) {
            toolbar.title = "Unit Tersewa"
            tvEmptyTitle.text = "Belum Ada Unit Tersewa"
            tvEmptyDesc.text = "Daftar unit milik Anda yang sedang disewa oleh pelanggan akan muncul di sini."
        } else {
            toolbar.title = "Riwayat Sewa"
            tvEmptyTitle.text = "Belum Ada Riwayat"
            tvEmptyDesc.text = "Riwayat sewa kendaraan kamu akan muncul di sini setelah kamu melakukan transaksi."
        }

        setupRecyclerView()
        loadHistory()
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(
            events = emptyList(),
            isAdmin = isAdmin,
            isHistory = true,
            onItemClick = { event ->
                val bundle = Bundle()
                bundle.putInt("vehicle_id", event.id)
                findNavController().navigate(R.id.navigation_detail, bundle)
            },
            onDeleteClick = {}
        )
        rvTicket.adapter = adapter
    }

    private fun loadHistory() {
        val historyList = if (isAdmin) {
            repository.getRentedUnitsByAdmin(userEmail)
        } else {
            repository.getEventsByRenter(userEmail)
        }

        if (historyList.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvTicket.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rvTicket.visibility = View.VISIBLE
            // Update data: isAdmin tells the adapter if it should show renter labels (for history)
            adapter.updateData(historyList, isAdmin, true)
        }
    }
}