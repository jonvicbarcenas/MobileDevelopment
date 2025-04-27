package com.anime.aniwatch.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.anime.aniwatch.R
import com.anime.aniwatch.data.DateItem

class DateAdapter(
    private val context: Context,
    private val dateItems: MutableList<DateItem>,
    private val onDateSelected: (DateItem) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val dateItem = dateItems[position]
        holder.bind(dateItem)
    }

    override fun getItemCount(): Int = dateItems.size

    fun updateSelection(position: Int) {
        // Deselect all items
        for (i in dateItems.indices) {
            if (dateItems[i].isSelected) {
                dateItems[i] = dateItems[i].copy(isSelected = false)
                notifyItemChanged(i)
            }
        }

        // Select the new item
        dateItems[position] = dateItems[position].copy(isSelected = true)
        notifyItemChanged(position)

        // Notify the listener
        onDateSelected(dateItems[position])
    }

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayTextView: TextView = itemView.findViewById(R.id.textview_day)
        private val dateTextView: TextView = itemView.findViewById(R.id.textview_date)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    updateSelection(position)
                }
            }
        }

        fun bind(dateItem: DateItem) {
            dayTextView.text = dateItem.dayOfWeek
            dateTextView.text = dateItem.dayOfMonth

            // Update the background based on selection state
            if (dateItem.isSelected) {
                dateTextView.setBackgroundResource(R.drawable.date_circle_selected)
                dateTextView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            } else {
                dateTextView.setBackgroundResource(R.drawable.date_circle_unselected)
                dateTextView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
        }
    }
}
