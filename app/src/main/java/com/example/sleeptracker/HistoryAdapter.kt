package com.example.sleeptracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private var items: List<SleepModel> = emptyList()
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.tvDate)
        val bedtimeText: TextView = view.findViewById(R.id.tvBedtime)
        val wakeupText: TextView = view.findViewById(R.id.tvWakeup)
        val hoursText: TextView = view.findViewById(R.id.tvHours)
        val trackedText: TextView = view.findViewById(R.id.tvTrackedTime)
        val noteText: TextView = view.findViewById(R.id.tvDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sleep_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.dateText.text = item.dateLabel
        holder.bedtimeText.text = "Bedtime: ${item.bedtime}"
        holder.wakeupText.text = "Wake up: ${item.recommendedWakeup}"
        holder.hoursText.text = "Sleep goal: ${item.sleepHours} hours"
        holder.trackedText.text = if (item.actualTrackedDuration.isBlank()) {
            "Tracked time: --"
        } else {
            "Tracked time: ${item.actualTrackedDuration}"
        }
        holder.noteText.text = item.note
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(updatedItems: List<SleepModel>) {
        items = updatedItems
        notifyDataSetChanged()
    }
}
