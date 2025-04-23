import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pneumasoft.multitimer.model.TimerItem

class TimerAdapter(
    private val timers: List<TimerItem>,
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
}
