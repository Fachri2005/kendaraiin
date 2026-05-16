package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView

class ListFragment : Fragment() {

    private lateinit var repository: EventRepository
    private lateinit var adapter: EventAdapter
    private lateinit var rvEvents: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvListTitle: TextView
    private var isAdmin: Boolean = false
    private var userEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())
        rvEvents = view.findViewById(R.id.rvEvents)
        etSearch = view.findViewById(R.id.etSearch)
        tvListTitle = view.findViewById(R.id.tvListTitle)

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userRole = sharedPref.getString("user_role", "Customer")
        userEmail = sharedPref.getString("user_email", "") ?: ""
        isAdmin = userRole == "Admin"

        // Halaman Cari sekarang difokuskan untuk pencarian kendaraan/toko untuk semua user
        tvListTitle.text = "Hasil Pencarian"
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarList)
        toolbar.title = "Cari Kendaraan"

        setupRecyclerView()
        loadEvents()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    loadEvents()
                } else {
                    performSearch(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(
            events = emptyList(),
            isAdmin = false, // Di halaman cari, tidak ada opsi delete/edit manajemen
            onItemClick = { event -> 
                val bundle = Bundle()
                bundle.putInt("vehicle_id", event.id)
                findNavController().navigate(R.id.navigation_detail, bundle)
            },
            onDeleteClick = { }
        )
        rvEvents.adapter = adapter
    }

    private fun loadEvents() {
        // Tampilkan semua unit di halaman cari agar user bisa menemukan berbagai kendaraan
        val events = repository.getAllEvents()
        adapter.updateData(events, false)
    }

    private fun performSearch(query: String) {
        val results = repository.searchVehicles(query)
        adapter.updateData(results, false)
    }
}