package com.example.myapplication

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

        holder.ivDelete.visibility = if (isAdmin && !isHistory) View.VISIBLE else View.GONE

        // Tampilkan Informasi Sewa & Sisa Waktu di Riwayat
        if (isHistory) {
            holder.tvRenterLabel.visibility = View.VISIBLE
            val remainingText = getRemainingTimeText(event.rentalStartDate, event.rentalDuration)
            
            if (isAdmin && !event.renterEmail.isNullOrEmpty()) {
                val baseText = "Penyewa: ${event.renterEmail}"
                holder.tvRenterLabel.text = if (remainingText.isNotEmpty()) "$baseText\n$remainingText" else baseText
            } else if (!event.rentalStartDate.isNullOrEmpty()) {
                // Untuk Customer
                val baseText = "Sewa: ${event.rentalStartDate} (${event.rentalDuration} Hari)"
                holder.tvRenterLabel.text = if (remainingText.isNotEmpty()) "$baseText | $remainingText" else baseText
            } else {
                holder.tvRenterLabel.visibility = View.GONE
            }
        } else {
            holder.tvRenterLabel.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(event) }
        holder.ivDelete.setOnClickListener { onDeleteClick(event) }
    }

    private fun getRemainingTimeText(startDateStr: String?, duration: Int?): String {
        if (startDateStr.isNullOrEmpty() || duration == null) return ""
        
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.parse(startDateStr) ?: return ""
            
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.add(Calendar.DAY_OF_YEAR, duration)
            
            val endDate = calendar.time
            val currentTime = Calendar.getInstance().time
            
            val diffInMillis = endDate.time - currentTime.time
            val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)
            
            when {
                diffInMillis <= 0 -> "Selesai"
                diffInDays >= 1 -> "Sisa: $diffInDays Hari"
                else -> {
                    val diffInHours = diffInMillis / (60 * 60 * 1000)
                    if (diffInHours >= 1) "Sisa: $diffInHours Jam" else "Sisa: Kurang dari 1 jam"
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<Event>, adminStatus: Boolean = false, historyStatus: Boolean = false) {
        val diffCallback = EventDiffCallback(events, newEvents)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        events = newEvents
        isAdmin = adminStatus
        isHistory = historyStatus
        
        diffResult.dispatchUpdatesTo(this)
    }

    class EventDiffCallback(
        private val oldList: List<Event>,
        private val newList: List<Event>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
