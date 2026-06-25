package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListFragment : Fragment() {

    private lateinit var viewModel: EventViewModel
    private lateinit var adapter: EventAdapter
    private lateinit var rvEvents: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvListTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmpty: LinearLayout
    
    private var isAdmin: Boolean = false
    private var userEmail: String = ""
    private var filterJob: Job? = null

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
        // Menggunakan scope 'this' agar ViewModel dibersihkan saat fragment hancur
        viewModel = ViewModelProvider(this, factory)[EventViewModel::class.java]

        rvEvents = view.findViewById(R.id.rvEvents)
        etSearch = view.findViewById(R.id.etSearch)
        tvListTitle = view.findViewById(R.id.tvListTitle)
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("user_email", "") ?: ""
        isAdmin = sharedPref.getString("user_role", "Customer") == "Admin"

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
            isAdmin = isAdmin,
            onItemClick = { event -> 
                val bundle = Bundle().apply { putInt("vehicle_id", event.id) }
                findNavController().navigate(R.id.navigation_detail, bundle)
            },
            onDeleteClick = { event ->
                // Panggil viewModel.deleteEvent jika diperlukan
                viewModel.deleteEvent(event.id, userEmail) {
                    Toast.makeText(context, R.string.success_delete, Toast.LENGTH_SHORT).show()
                }
            }
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

        viewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                if (errorResId != null && errorResId != 0 && isAdded) {
                    Toast.makeText(context ?: return@let, getString(errorResId), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyFilter(query: String) {
        filterJob?.cancel() // Batalkan proses filter sebelumnya (Debounce)
        
        val allEvents = viewModel.events.value ?: emptyList()
        
        filterJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            // Delay 300ms untuk memberikan efek debounce saat mengetik
            delay(300)
            
            val trimmedQuery = query.lowercase().trim()
            val trimmedUserEmail = userEmail.lowercase().trim()

            val filtered = allEvents.filter { event ->
                val matchesVisibility = if (isAdmin) {
                    event.adminEmail?.lowercase()?.trim() == trimmedUserEmail
                } else {
                    !event.isRegistered 
                }
                
                val matchesQuery = trimmedQuery.isEmpty() || 
                        event.name?.contains(trimmedQuery, ignoreCase = true) == true || 
                        event.vehicleType?.contains(trimmedQuery, ignoreCase = true) == true || 
                        event.location?.contains(trimmedQuery, ignoreCase = true) == true
                
                matchesVisibility && matchesQuery
            }

            withContext(Dispatchers.Main) {
                if (isAdded) {
                    adapter.updateData(filtered, isAdmin)
                    // Tampilkan Empty State jika hasil pencarian kosong
                    layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        filterJob?.cancel()
        super.onDestroyView()
    }
}
