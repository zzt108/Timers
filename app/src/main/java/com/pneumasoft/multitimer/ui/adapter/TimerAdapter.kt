import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pneumasoft.multitimer.databinding.ItemTimerBinding
import com.pneumasoft.multitimer.model.TimerItem

class TimerAdapter(
    timers: List<TimerItem> = emptyList(),
    private val onStartPauseClick: (String) -> Unit,
    private val onResetClick: (String) -> Unit,
    private val onEditClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<TimerAdapter.TimerViewHolder>() {
    private var timers: List<TimerItem> = timers

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

            startPauseButton.text = if (timer.isRunning) "Pause" else "Start"
            startPauseButton.setOnClickListener { onStartPauseClick(timer.id) }

            resetButton.setOnClickListener { onResetClick(timer.id) }
            editButton.setOnClickListener { onEditClick(timer.id) }
            deleteButton.setOnClickListener { onDeleteClick(timer.id) }
        }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
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
