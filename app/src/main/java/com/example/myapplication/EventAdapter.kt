package com.example.myapplication

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class EventAdapter(
    private var isAdmin: Boolean = false,
    private var isHistory: Boolean = false,
    private val onItemClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit,
    private val onStatusAction: ((Event, String) -> Unit)? = null
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvItemName)
        val tvPrice: TextView = view.findViewById(R.id.tvItemPrice)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
        val ivProduct: ImageView = view.findViewById(R.id.ivProduct)
        val tvTransmission: TextView = view.findViewById(R.id.tvTransmissionBadge)
        val tvSeats: TextView = view.findViewById(R.id.tvSeats)
        val tvLocation: TextView = view.findViewById(R.id.tvLocationItem)
        val tvRenterLabel: TextView = view.findViewById(R.id.tvRenterLabel)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val layoutActions: LinearLayout = view.findViewById(R.id.layoutActions)
        val btnCancel: Button = view.findViewById(R.id.btnActionCancel)
        val btnApprove: Button = view.findViewById(R.id.btnActionApprove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        val context = holder.itemView.context
        
        holder.tvName.text = event.name ?: context.getString(R.string.unknown_name)
        holder.tvPrice.text = context.getString(R.string.price_per_day_format, event.formattedPrice)
        holder.tvTransmission.text = event.transmission ?: context.getString(R.string.label_manual)
        holder.tvSeats.text = context.getString(R.string.seats_format, event.seats ?: "2")
        holder.tvLocation.text = event.location ?: "Jakarta"
        
        val imgSource = if (!event.imageUri.isNullOrBlank()) event.imageUri else android.R.drawable.ic_menu_gallery
        
        Glide.with(context)
            .load(imgSource)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .override(400, 300) // Optimasi RAM: Downsample gambar
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.ivProduct)

        if (isHistory) {
            val status = event.effectiveStatus
            holder.tvStatusBadge.visibility = View.VISIBLE
            
            holder.tvStatusBadge.text = when (status) {
                "pending" -> context.getString(R.string.status_pending)
                "approved" -> context.getString(R.string.status_approved)
                "canceled" -> context.getString(R.string.status_canceled)
                "completed" -> context.getString(R.string.status_completed)
                else -> status.replaceFirstChar { it.uppercase() }
            }
            
            val colorRes = when (status) {
                "approved" -> R.color.status_approved
                "canceled" -> R.color.status_canceled
                "completed" -> R.color.status_completed
                else -> R.color.status_pending
            }
            holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

            holder.tvRenterLabel.visibility = View.VISIBLE
            holder.tvRenterLabel.text = if (isAdmin) {
                context.getString(R.string.label_renter, event.renterEmail ?: "")
            } else {
                context.getString(R.string.label_rental_date, event.rentalStartDate ?: "")
            }

            if (status == "pending") {
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnApprove.visibility = if (isAdmin) View.VISIBLE else View.GONE
                holder.btnCancel.visibility = View.VISIBLE
                holder.btnCancel.text = if (isAdmin) context.getString(R.string.btn_reject) else context.getString(R.string.btn_cancel)
            } else {
                holder.layoutActions.visibility = View.GONE
            }
        } else {
            holder.tvStatusBadge.visibility = View.GONE
            holder.layoutActions.visibility = View.GONE
            holder.ivDelete.visibility = if (isAdmin) View.VISIBLE else View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(event) }
        holder.btnApprove.setOnClickListener { onStatusAction?.invoke(event, "approved") }
        holder.btnCancel.setOnClickListener { onStatusAction?.invoke(event, "canceled") }
        holder.ivDelete.setOnClickListener { onDeleteClick(event) }
    }

    fun updateData(newEvents: List<Event>, adminStatus: Boolean = false, historyStatus: Boolean = false) {
        this.isAdmin = adminStatus
        this.isHistory = historyStatus
        submitList(newEvents)
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
    }
}
