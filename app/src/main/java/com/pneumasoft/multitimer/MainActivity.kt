package com.pneumasoft.multitimer

import TimerAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.multitimer.R
import com.pneumasoft.multitimer.viewmodel.TimerViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TimerViewModel by viewModels()
    private val adapter = TimerAdapter(
        emptyList(),
        onStartPauseClick = { id -> handleStartPause(id) },
        onResetClick = { id -> viewModel.resetTimer(id) },
        onEditClick = { id -> showEditTimerDialog(id) },
        onDeleteClick = { id -> viewModel.deleteTimer(id) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupAddButton()
        observeTimers()
    }

    private fun setupRecyclerView() {
        binding.timerRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupAddButton() {
        binding.addTimerButton.setOnClickListener {
            showAddTimerDialog()
        }
    }

    private fun observeTimers() {
        lifecycleScope.launch {
            viewModel.timers.collect { timers ->
                adapter.updateTimers(timers)
            }
        }
    }

    private fun handleStartPause(id: String) {
        val timer = viewModel.timers.value.find { it.id == id } ?: return
        if (timer.isRunning) {
            viewModel.pauseTimer(id)
        } else {
            viewModel.startTimer(id)
        }
    }

    private fun showAddTimerDialog() {
        // Implementation for dialog to add a new timer
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_timer, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.timer_name_edit)
        val minutesEditText = dialogView.findViewById<EditText>(R.id.timer_minutes_edit)
        val secondsEditText = dialogView.findViewById<EditText>(R.id.timer_seconds_edit)

        AlertDialog.Builder(this)
            .setTitle("Add New Timer")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString()
                val minutes = minutesEditText.text.toString().toIntOrNull() ?: 0
                val seconds = secondsEditText.text.toString().toIntOrNull() ?: 0
                val totalSeconds = minutes * 60 + seconds

                if (name.isNotBlank() && totalSeconds > 0) {
                    viewModel.addTimer(name, totalSeconds)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTimerDialog(id: String) {
        // Similar to add dialog but pre-fills values and updates existing timer
        val timer = viewModel.timers.value.find { it.id == id } ?: return

        // Implementation similar to showAddTimerDialog but with pre-filled values
        // and calls viewModel.updateTimer() instead
    }
}