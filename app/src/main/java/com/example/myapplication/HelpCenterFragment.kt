package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class HelpCenterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help_center, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar = view.findViewById<Toolbar>(R.id.toolbarHelp)
        toolbar.setNavigationIcon(R.drawable.ic_chevron_right) // Or use a back icon if available
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<View>(R.id.btnContactSupport).setOnClickListener {
            val phoneNumber = "628123456789" // Ganti dengan nomor WA admin sesungguhnya
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber"
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "WhatsApp tidak terpasang", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
