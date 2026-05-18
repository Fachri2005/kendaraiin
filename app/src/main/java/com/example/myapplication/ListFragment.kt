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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Repository and ViewModel
        val repository = EventRepository(requireContext())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[EventViewModel::class.java]

        // Bind Views
        rvEvents = view.findViewById(R.id.rvEvents)
        etSearch = view.findViewById(R.id.etSearch)
        tvListTitle = view.findViewById(R.id.tvListTitle)
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // Session
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userRole = sharedPref.getString("user_role", "Customer")
        isAdmin = userRole == "Admin"

        setupRecyclerView()
        observeViewModel()

        // Fetch data from API
        viewModel.fetchEventsFromApi()

        // Swipe Refresh listener
        swipeRefresh.setOnRefreshListener {
            viewModel.fetchEventsFromApi()
        }

        // Search logic
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    viewModel.fetchEventsFromApi()
                } else {
                    viewModel.searchVehicles(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(
            events = emptyList(),
            isAdmin = false,
            onItemClick = { event -> 
                val bundle = Bundle()
                bundle.putInt("vehicle_id", event.id)
                findNavController().navigate(R.id.navigation_detail, bundle)
            },
            onDeleteClick = { }
        )
        rvEvents.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            adapter.updateData(events, false)
            swipeRefresh.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Use SwipeRefresh spinner or ProgressBar
            if (!swipeRefresh.isRefreshing) {
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            swipeRefresh.isRefreshing = false
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
