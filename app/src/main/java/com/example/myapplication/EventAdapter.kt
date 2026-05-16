package com.example.myapplication

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(
    private var events: List<Event>,
    private var isAdmin: Boolean = false,
    private var isHistory: Boolean = false,
    private val onItemClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvItemName)
        val tvPrice: TextView = view.findViewById(R.id.tvItemPrice)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
        val ivProduct: ImageView = view.findViewById(R.id.ivProduct)
        val tvTransmission: TextView = view.findViewById(R.id.tvTransmissionBadge)
        val tvSeats: TextView = view.findViewById(R.id.tvSeats)
        val tvLocation: TextView = view.findViewById(R.id.tvLocationItem)
        val tvRenterLabel: TextView = view.findViewById(R.id.tvRenterLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        
        holder.tvName.text = event.name
        
        val basePrice = event.price.replace(" / hari", "").replace(" / Hari", "")
        holder.tvPrice.text = "$basePrice / hari"
        
        holder.tvTransmission.text = event.transmission ?: "Matic"
        holder.tvSeats.text = event.seats ?: "5 Kursi"
        holder.tvLocation.text = event.location ?: "Jakarta"
        
        if (!event.imageUri.isNullOrEmpty()) {
            try {
                holder.ivProduct.setPadding(0, 0, 0, 0)
                holder.ivProduct.setImageURI(Uri.parse(event.imageUri))
            } catch (e: Exception) {
                holder.ivProduct.setImageResource(android.R.drawable.ic_menu_gallery)
                holder.ivProduct.setPadding(30, 30, 30, 30)
            }
        } else {
            holder.ivProduct.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.ivProduct.setPadding(30, 30, 30, 30)
        }

        // Show delete only for Admin management in Home, NOT in History
        holder.ivDelete.visibility = if (isAdmin && !isHistory) View.VISIBLE else View.GONE

        // History specific: Show who rented the unit (for Admin)
        if (isHistory && isAdmin && !event.renterEmail.isNullOrEmpty()) {
            holder.tvRenterLabel.visibility = View.VISIBLE
            holder.tvRenterLabel.text = "Penyewa: ${event.renterEmail}"
        } else {
            holder.tvRenterLabel.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(event) }
        holder.ivDelete.setOnClickListener { onDeleteClick(event) }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<Event>, adminStatus: Boolean = false, historyStatus: Boolean = false) {
        events = newEvents
        isAdmin = adminStatus
        isHistory = historyStatus
        notifyDataSetChanged()
    }
}