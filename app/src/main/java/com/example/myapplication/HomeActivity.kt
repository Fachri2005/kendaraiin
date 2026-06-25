package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val eventViewModel: EventViewModel by viewModels { ViewModelFactory(this) }
    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(this) }
    private lateinit var navController: NavController

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        setupBadgeObserver()
        setupNavigationListener()
    }

    private fun setupNavigationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_ticket) markNotificationsAsSeen()
        }
    }

    private fun markNotificationsAsSeen() {
        val userRole = userViewModel.getUserRole()
        if (userRole == "Customer") {
            val sharedPref = getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)
            val events = eventViewModel.events.value ?: return
            val userEmail = userViewModel.getUserEmail()

            lifecycleScope.launch(Dispatchers.Default) {
                val seenIds = events.filter { it.renterEmail == userEmail && it.effectiveStatus == "approved" }
                    .map { it.id.toString() }.toSet()
                
                val currentSeen = sharedPref.getStringSet("seen_approved_ids", emptySet()) ?: emptySet()
                sharedPref.edit().putStringSet("seen_approved_ids", currentSeen + seenIds).apply()
                
                withContext(Dispatchers.Main) { updateBadge(events) }
            }
        }
    }

    private fun setupBadgeObserver() {
        eventViewModel.events.observe(this) { events -> updateBadge(events) }
        val userEmail = userViewModel.getUserEmail()
        val userRole = userViewModel.getUserRole()
        eventViewModel.fetchEventsFromApi(if (userRole == "Admin") userEmail else null)
    }

    private fun updateBadge(events: List<Event>) {
        val userRole = userViewModel.getUserRole()
        val userEmail = userViewModel.getUserEmail()
        val badge = binding.bottomNavigation.getOrCreateBadge(R.id.navigation_ticket)

        lifecycleScope.launch(Dispatchers.Default) {
            val count = if (userRole == "Admin") {
                events.count { it.adminEmail?.trim().equals(userEmail?.trim(), ignoreCase = true) && it.effectiveStatus == "pending" }
            } else {
                val sharedPref = getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)
                val seenIds = sharedPref.getStringSet("seen_approved_ids", emptySet()) ?: emptySet()
                events.count { it.renterEmail == userEmail && it.effectiveStatus == "approved" && !seenIds.contains(it.id.toString()) }
            }

            withContext(Dispatchers.Main) {
                if (count > 0) {
                    badge.isVisible = true
                    badge.number = count
                } else {
                    badge.isVisible = false
                }
            }
        }
    }
}
