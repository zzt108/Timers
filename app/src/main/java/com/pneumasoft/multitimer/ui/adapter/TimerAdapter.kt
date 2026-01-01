package com.pneumasoft.multitimer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.databinding.ItemTimerBinding
import com.pneumasoft.multitimer.model.TimerItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimerAdapter(
    private var timers: List<TimerItem> = emptyList(),
    private val onStartPauseClick: (String) -> Unit,
    private val onResetClick: (String) -> Unit,
    private val onEditClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<TimerAdapter.TimerViewHolder>() {
    class TimerViewHolder(val binding: ItemTimerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerViewHolder {
        val binding = ItemTimerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TimerViewHolder(binding)
    }

    override fun getItemCount() = timers.size

    override fun onBindViewHolder(holder: TimerViewHolder, position: Int) {
        val timer = timers[position]
        holder.binding.apply {
            timerName.text = timer.name
            timerDisplay.text = formatTime(timer.remainingSeconds)

            // Calculate seconds for the progress indicator
            val secondsValue = timer.remainingSeconds % 60

            // Set the progress on the actual ProgressBar view
            secondsProgress.progress = secondsValue

            // Update button icon based on timer state
            val isRunning = timer.isRunning
            startPauseButton.setImageResource(
                if (isRunning) R.drawable.ic_pause else R.drawable.ic_play
            )
            // Update content description for testing
            startPauseButton.contentDescription = if (isRunning) "Pause" else "Start"

            // Calculate and display expiration time
            if (isRunning) {
                val expirationTimeText = calculateExpirationTime(timer.remainingSeconds)
                timerExpirationTime.text = expirationTimeText
                timerExpirationTime.visibility = View.VISIBLE
            } else {
                timerExpirationTime.visibility = View.GONE
            }

            // Set click listeners
            startPauseButton.setOnClickListener { onStartPauseClick(timer.id) }
            resetButton.setOnClickListener { onResetClick(timer.id) }
            editButton.setOnClickListener { onEditClick(timer.id) }
            deleteButton.setOnClickListener { onDeleteClick(timer.id) }
        }
    }

    private fun calculateExpirationTime(remainingSeconds: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, remainingSeconds)

        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    // Format time to show only hours and minutes (no seconds)
    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) {
            "%d h %02d m".format(hours, minutes)
        } else {
            "%d min".format(minutes)
        }
    }

    fun updateTimers(newTimers: List<TimerItem>) {
        val oldTimers = timers
        this.timers = newTimers
        // Use DiffUtil to calculate the differences
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldTimers.size
            override fun getNewListSize() = newTimers.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldTimers[oldItemPosition].id == newTimers[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = oldTimers[oldItemPosition]
                val new = newTimers[newItemPosition]
                return old.name == new.name &&
                        old.remainingSeconds == new.remainingSeconds &&
                        old.isRunning == new.isRunning
            }
        })
        // Dispatch updates
        diffResult.dispatchUpdatesTo(this)
    }
}
