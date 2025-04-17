package com.example.tempustrace.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tempustrace.R
import com.example.tempustrace.data.WorkDayWithBreaks
import java.time.Duration
import java.time.format.DateTimeFormatter

class RecentWorkdaysAdapter : RecyclerView.Adapter<RecentWorkdaysAdapter.ViewHolder>() {

    private var workdays: List<WorkDayWithBreaks> = emptyList()

    fun setData(workdays: List<WorkDayWithBreaks>) {
        this.workdays = workdays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_workday, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val workdayWithBreaks = workdays[position]
        holder.bind(workdayWithBreaks)
    }

    override fun getItemCount(): Int = workdays.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateText: TextView = view.findViewById(R.id.text_date)
        private val hoursText: TextView = view.findViewById(R.id.text_hours)
        private val timeRangeText: TextView = view.findViewById(R.id.text_time_range)
        private val breaksText: TextView = view.findViewById(R.id.text_breaks)

        fun bind(workdayWithBreaks: WorkDayWithBreaks) {
            val workday = workdayWithBreaks.workDay
            val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            // Format date
            dateText.text = dateFormatter.format(workday.date)

            // Calculate and format work hours
            workday.endTime?.let { endTime ->
                val totalWorkMinutes = Duration.between(workday.startTime, endTime).toMinutes()
                val totalBreakMinutes = workdayWithBreaks.breaks.sumOf { it.durationMinutes ?: 0 }
                val netWorkMinutes = totalWorkMinutes - totalBreakMinutes

                val hours = netWorkMinutes / 60
                val minutes = netWorkMinutes % 60
                hoursText.text = String.format("%d:%02d h", hours, minutes)

                // Format time range
                timeRangeText.text = "${timeFormatter.format(workday.startTime)} - ${timeFormatter.format(endTime)}"
            } ?: run {
                hoursText.text = "Ongoing"
                timeRangeText.text = "Started at ${timeFormatter.format(workday.startTime)}"
            }

            // Format breaks info
            val breakInfo = if (workdayWithBreaks.breaks.isEmpty()) {
                "No breaks"
            } else {
                val totalBreakMinutes = workdayWithBreaks.breaks.sumOf { it.durationMinutes ?: 0 }
                val hours = totalBreakMinutes / 60
                val minutes = totalBreakMinutes % 60
                if (hours > 0) {
                    "$hours h $minutes min in ${workdayWithBreaks.breaks.size} breaks"
                } else {
                    "$minutes min in ${workdayWithBreaks.breaks.size} breaks"
                }
            }
            breaksText.text = breakInfo
        }
    }
}