package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class ListFragment : Fragment() {

    private lateinit var viewModel: EventViewModel
    private lateinit var adapter: EventAdapter
    private lateinit var rvEvents: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvListTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
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

        val factory = ViewModelFactory(requireContext())
        viewModel = ViewModelProvider(requireActivity(), factory)[EventViewModel::class.java]

        rvEvents = view.findViewById(R.id.rvEvents)
        etSearch = view.findViewById(R.id.etSearch)
        tvListTitle = view.findViewById(R.id.tvListTitle)
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userRole = sharedPref.getString("user_role", "Customer")
        userEmail = sharedPref.getString("user_email", "") ?: ""
        isAdmin = userRole == "Admin"

        if (isAdmin) {
            tvListTitle.text = getString(R.string.list_title_admin)
            etSearch.hint = getString(R.string.list_search_hint_admin)
        }

        setupRecyclerView()
        observeViewModel()

        viewModel.fetchEventsFromApi(if (isAdmin) userEmail else null)

        swipeRefresh.setOnRefreshListener {
            viewModel.fetchEventsFromApi(if (isAdmin) userEmail else null)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(
            events = emptyList(),
            isAdmin = isAdmin,
            onItemClick = { event -> 
                val bundle = Bundle().apply { putInt("vehicle_id", event.id) }
                findNavController().navigate(R.id.navigation_detail, bundle)
            },
            onDeleteClick = { /* Opsional */ }
        )
        rvEvents.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) {
            applyFilter(etSearch.text.toString().trim())
            swipeRefresh.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!swipeRefresh.isRefreshing) {
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // PERBAIKAN: Menggunakan EventWrapper agar pesan error muncul dengan benar
        viewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                errorResId?.let {
                    Toast.makeText(context, getString(it), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyFilter(query: String) {
        val allEvents = viewModel.events.value ?: emptyList()
        
        val myUnits = if (isAdmin) {
            allEvents.filter { 
                it.adminEmail?.trim().equals(userEmail.trim(), ignoreCase = true) 
            }
        } else {
            allEvents
        }

        val filtered = if (query.isEmpty()) {
            myUnits
        } else {
            myUnits.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.vehicleType?.contains(query, ignoreCase = true) == true ||
                it.location?.contains(query, ignoreCase = true) == true
            }
        }

        adapter.updateData(filtered, isAdmin)
    }
}
