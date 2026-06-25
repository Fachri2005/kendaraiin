package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentTicketBinding

class TicketFragment : Fragment() {

    private var _binding: FragmentTicketBinding? = null
    private val binding get() = _binding!!

    private val eventViewModel: EventViewModel by activityViewModels { ViewModelFactory(requireContext()) }
    private val userViewModel: UserViewModel by activityViewModels { ViewModelFactory(requireContext()) }
    
    private lateinit var adapter: EventAdapter
    private var currentStatusFilter: String = "semua"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isAdmin = userViewModel.getUserRole()?.equals("Admin", ignoreCase = true) == true

        if (isAdmin) {
            binding.toolbarTicket.title = getString(R.string.ticket_title_admin)
            binding.tvEmptyTitle.text = getString(R.string.ticket_empty_title_admin)
            binding.tvEmptyDesc.text = getString(R.string.ticket_empty_desc_admin)
        }

        setupRecyclerView(isAdmin)
        setupFilters()
        observeViewModel()
        loadData()
        binding.swipeRefresh.setOnRefreshListener { loadData() }
    }

    private fun setupRecyclerView(isAdmin: Boolean) {
        val userEmail = userViewModel.getUserEmail() ?: ""
        adapter = EventAdapter(
            isAdmin = isAdmin,
            isHistory = true,
            onItemClick = { event ->
                val bundle = Bundle().apply { putInt("vehicle_id", event.id) }
                findNavController().navigate(R.id.navigation_detail, bundle)
            },
            onDeleteClick = {},
            onStatusAction = { event, newStatus ->
                eventViewModel.updateRentalStatus(event.id, newStatus, if (isAdmin) userEmail else null)
            }
        )
        binding.rvTicket.adapter = adapter
    }

    private fun observeViewModel() {
        eventViewModel.events.observe(viewLifecycleOwner) { applyFilters() }
        eventViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (_binding == null) return@observe
            if (!binding.swipeRefresh.isRefreshing) binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        eventViewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorResId ->
                errorResId?.let { if (isAdded) Toast.makeText(context, getString(it), Toast.LENGTH_SHORT).show() }
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun applyFilters() {
        if (_binding == null) return
        val allEvents = eventViewModel.events.value ?: emptyList()
        val userEmail = userViewModel.getUserEmail() ?: ""
        val isAdmin = userViewModel.getUserRole()?.equals("Admin", ignoreCase = true) == true
        
        val filteredList = allEvents.filter { event ->
            val status = event.effectiveStatus
            val isMine = if (isAdmin) {
                event.adminEmail?.trim().equals(userEmail.trim(), ignoreCase = true) && status.isNotEmpty()
            } else {
                event.renterEmail?.trim().equals(userEmail.trim(), ignoreCase = true)
            }
            val matchesStatus = if (currentStatusFilter == "semua") true else status == currentStatusFilter
            isMine && matchesStatus
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

    private fun loadData() {
        val userEmail = userViewModel.getUserEmail()
        val isAdmin = userViewModel.getUserRole()?.equals("Admin", ignoreCase = true) == true
        eventViewModel.fetchEventsFromApi(if (isAdmin) userEmail else null)
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener { currentStatusFilter = "semua"; applyFilters() }
        binding.chipPending.setOnClickListener { currentStatusFilter = "pending"; applyFilters() }
        binding.chipActive.setOnClickListener { currentStatusFilter = "approved"; applyFilters() }
        binding.chipFinished.setOnClickListener { currentStatusFilter = "completed"; applyFilters() }
        binding.chipCanceled.setOnClickListener { currentStatusFilter = "canceled"; applyFilters() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
