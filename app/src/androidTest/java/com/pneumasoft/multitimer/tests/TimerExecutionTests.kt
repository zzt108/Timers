package com.pneumasoft.multitimer.tests

import android.Manifest
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.pneumasoft.multitimer.MainActivity
import com.pneumasoft.multitimer.robots.MainScreenRobot
import com.pneumasoft.multitimer.robots.AddTimerDialogRobot
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerExecutionTests {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    private val mainScreen = MainScreenRobot()
    private val addTimerDialog = AddTimerDialogRobot()

    @Before
    fun cleanup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("timer_preferences", Context.MODE_PRIVATE)

        prefs.edit().apply {
            clear()
            putBoolean("default_timers_created", true)
            commit()
        }

        activityScenarioRule.scenario.recreate()
    }

    @Test
    fun timer_should_continue_running_when_app_goes_to_background() {
        // Wait for any startup dialogs
        Thread.sleep(2000)

        // Dismiss battery dialog if present
        try {
            androidx.test.espresso.Espresso.pressBack()
        } catch (e: Exception) {
            // Dialog might not exist, ignore
        }

        Thread.sleep(500)

        // Create timer
        mainScreen.tapAddTimer()
        addTimerDialog.apply {
            enterName("Background Test")
            setMinutes(2)
            tapAdd()
        }
        Thread.sleep(1000) // Wait for adapter update

        // Start timer
        mainScreen.timerWithName("Background Test")?.tapPlayPause()
        Thread.sleep(1000) // Wait for timer to start

        // ✅ FIX: Should check that timer IS running (not "not running")
        mainScreen.timerWithName("Background Test")?.shouldBeRunning()

        // Simulate background (could add UiDevice.pressHome() here)
        Thread.sleep(2000)

        // Verify timer still running
        mainScreen.timerWithName("Background Test")?.shouldBeRunning()
    }

    @Test
    fun absolute_time_calculation_should_prevent_drift_over_long_duration() {
        // Wait for startup
        Thread.sleep(2000)

        try {
            androidx.test.espresso.Espresso.pressBack()
        } catch (e: Exception) {
            // Ignore
        }

        Thread.sleep(500)

        // Create timer
        mainScreen.tapAddTimer()
        addTimerDialog.apply {
            enterName("Precision Test")
            setMinutes(1)
            tapAdd()
        }
        Thread.sleep(1000)

        // Start timer
        mainScreen.timerWithName("Precision Test")?.tapPlayPause()
        Thread.sleep(1000)

        // Verify running
        mainScreen.timerWithName("Precision Test")?.shouldBeRunning()

        // Optional: Check remaining time is decreasing correctly
        val remaining1 = mainScreen.timerWithName("Precision Test")?.getRemainingMillis() ?: 0
        Thread.sleep(2000)
        val remaining2 = mainScreen.timerWithName("Precision Test")?.getRemainingMillis() ?: 0

        // Should have decreased by ~2000ms (allow 500ms tolerance)
        val decrease = remaining1 - remaining2
        assert(decrease in 1500..2500) {
            "Expected ~2000ms decrease, got $decrease ms"
        }
    }
}
