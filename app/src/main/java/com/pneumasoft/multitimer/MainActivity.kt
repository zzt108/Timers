package com.pneumasoft.multitimer

import TimerAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pneumasoft.multitimer.databinding.ActivityMainBinding
import com.pneumasoft.multitimer.viewmodel.TimerViewModel
import kotlinx.coroutines.launch
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.pneumasoft.multitimer.services.TimerService
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TimerViewModel by viewModels()
    private val adapter = TimerAdapter(
        onStartPauseClick = { id -> handleStartPause(id) },
        onResetClick = { id -> viewModel.resetTimer(id) },
        onEditClick = { id -> showEditTimerDialog(id) },
        onDeleteClick = { id -> viewModel.deleteTimer(id) }
    )

    private var timerService: TimerService? = null
    private var isServiceBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permissions for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }

        setupRecyclerView()
        setupAddButton()
        observeTimers()
    }

    private fun bindTimerService() {
        val serviceIntent = Intent(this, TimerService::class.java)

        // First start as foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Then bind to it
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                // Permission already granted, safe to proceed with notifications
                initializeTimerService()
            }
        } else {
            // For lower Android versions, just initialize directly
            initializeTimerService()
        }
    }

    private fun initializeTimerService() {
        // Only bind to service if permissions are granted
        bindTimerService()
    }

    // Improve the permission result handling
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize service
                initializeTimerService()
            } else {
                // Permission denied - inform user that timers may not function properly
                Toast.makeText(
                    this,
                    "Timer notifications will not work without permission. Timers may stop in background.",
                    Toast.LENGTH_LONG
                ).show()

                // Still initialize, but service will have limited functionality
                initializeTimerService()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, TimerService::class.java))
        } else {
            startService(Intent(this, TimerService::class.java))
        }
    }

    override fun onStop() {
        super.onStop()
        // Don't unbind here if you want the service to continue in the background
        // Only unbind if you want the service to stop when the app is not visible
    }

    override fun onDestroy() {
        super.onDestroy()
        // Properly clean up service connection
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 100
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
        // Inflate the dialog view
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_timer, null)

        // Get references to the UI components
        val nameEditText = dialogView.findViewById<EditText>(R.id.timer_name_edit)
        val hoursValue = dialogView.findViewById<TextView>(R.id.hours_value)
        val minutesValue = dialogView.findViewById<TextView>(R.id.minutes_value)
        val timerDisplayText = dialogView.findViewById<TextView>(R.id.timer_display_text)
        val hoursUpButton = dialogView.findViewById<ImageButton>(R.id.hours_up_button)
        val hoursDownButton = dialogView.findViewById<ImageButton>(R.id.hours_down_button)
        val minutesSlider = dialogView.findViewById<Slider>(R.id.minutes_slider)

        // Initialize time values
        var hours = 0
        var minutes = 0

        // Create function to update the display
        fun updateDisplay() {
            val formattedHours = String.format("%02d", hours)
            val formattedMinutes = String.format("%02d", minutes)
            timerDisplayText.text = "$formattedHours h $formattedMinutes m"
            hoursValue.text = formattedHours
            minutesValue.text = "$formattedMinutes minutes"
        }

        // Set up hours controls
        hoursUpButton.setOnClickListener {
            hours = (hours + 1) % 24  // Wrap around after 23
            updateDisplay()
        }

        hoursDownButton.setOnClickListener {
            hours = if (hours > 0) hours - 1 else 23  // Wrap to 23 when going below 0
            updateDisplay()
        }

        // Set up minutes slider
        minutesSlider.addOnChangeListener { _, value, _ ->
            minutes = value.toInt()
            updateDisplay()
        }

        // Initial display update
        updateDisplay()

        // Show the dialog
        AlertDialog.Builder(this)
            .setTitle("Add New Timer")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString()
                val totalSeconds = hours * 3600 + minutes * 60

                if (totalSeconds > 0) {
                    if (name.isBlank()) {
                        // If name is empty, use position in list as default
                        val position = viewModel.timers.value.size + 1
                        viewModel.addTimer("Timer $position", totalSeconds)
                    } else {
                        viewModel.addTimer(name, totalSeconds)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Timer duration must be greater than zero",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTimerDialog(id: String) {
        // Find the timer to edit
        val timer = viewModel.timers.value.find { it.id == id } ?: return

        // Inflate the dialog view
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_timer, null)

        // Get references to the UI components
        val nameEditText = dialogView.findViewById<EditText>(R.id.timer_name_edit)
        val hoursValue = dialogView.findViewById<TextView>(R.id.hours_value)
        val minutesValue = dialogView.findViewById<TextView>(R.id.minutes_value)
        val timerDisplayText = dialogView.findViewById<TextView>(R.id.timer_display_text)
        val hoursUpButton = dialogView.findViewById<ImageButton>(R.id.hours_up_button)
        val hoursDownButton = dialogView.findViewById<ImageButton>(R.id.hours_down_button)
        val minutesSlider = dialogView.findViewById<Slider>(R.id.minutes_slider)

        // Extract current hours and minutes from timer
        val totalSeconds = timer.durationSeconds
        var hours = totalSeconds / 3600
        var minutes = (totalSeconds % 3600) / 60

        // Pre-fill the name field
        nameEditText.setText(timer.name)

        // Create function to update the display
        fun updateDisplay() {
            val formattedHours = String.format("%02d", hours)
            val formattedMinutes = String.format("%02d", minutes)
            timerDisplayText.text = "$formattedHours h $formattedMinutes m"
            hoursValue.text = formattedHours
            minutesValue.text = "$formattedMinutes minutes"
        }

        // Set up hours controls
        hoursUpButton.setOnClickListener {
            hours = (hours + 1) % 24
            updateDisplay()
        }

        hoursDownButton.setOnClickListener {
            hours = if (hours > 0) hours - 1 else 23
            updateDisplay()
        }

        // Set up minutes slider with initial value
        minutesSlider.value = minutes.toFloat()
        minutesSlider.addOnChangeListener { _, value, _ ->
            minutes = value.toInt()
            updateDisplay()
        }

        // Initial display update
        updateDisplay()

        // Show the dialog
        AlertDialog.Builder(this)
            .setTitle("Edit Timer")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = nameEditText.text.toString()
                val totalSeconds = hours * 3600 + minutes * 60

                if (totalSeconds > 0) {
                    if (name.isBlank()) {
                        // If name is empty, use position in list
                        val position = viewModel.timers.value.indexOf(timer) + 1
                        viewModel.updateTimer(id, "Timer $position", totalSeconds)
                    } else {
                        viewModel.updateTimer(id, name, totalSeconds)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Timer duration must be greater than zero",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}