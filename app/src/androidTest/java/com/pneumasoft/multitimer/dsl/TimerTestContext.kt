package com.pneumasoft.multitimer.dsl

import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.pneumasoft.multitimer.MainActivity
import com.pneumasoft.multitimer.robots.AddTimerDialogRobot
import com.pneumasoft.multitimer.robots.AlarmScreenRobot
import com.pneumasoft.multitimer.robots.MainScreenRobot
import com.pneumasoft.multitimer.robots.SettingsScreenRobot
import java.time.Duration
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals

class TimerTestContext(
    private val scenario: ActivityScenario<MainActivity>
) {
    val mainScreen = MainScreenRobot()
    val addTimerDialog = AddTimerDialogRobot()
    val alarmScreen = AlarmScreenRobot()
    val settingsScreen = SettingsScreenRobot()
    
    // DSL helpers for Robots
    fun mainScreen(block: MainScreenRobot.() -> Unit) = mainScreen.apply(block)
    fun addTimerDialog(block: AddTimerDialogRobot.() -> Unit) = addTimerDialog.apply(block)
    fun alarmScreen(block: AlarmScreenRobot.() -> Unit) = alarmScreen.apply(block)
    fun settingsScreen(block: SettingsScreenRobot.() -> Unit) = settingsScreen.apply(block)
    
    // Time manipulation helpers
    fun waitFor(duration: Duration) {
        onView(isRoot()).perform(waitForMillis(duration.toMillis()))
    }
    
    fun fastForwardTime(duration: Duration) {
        SystemClock.sleep(duration.toMillis())
    }
    
    // Helper to create a timer quickly
    fun createTimer(name: String = "Timer", duration: Duration) {
        mainScreen.tapAddTimer()
        addTimerDialog.apply {
            enterName(name)
            // Duration split
            val hours = duration.toHours().toInt()
            val minutes = (duration.toMinutes() % 60).toInt()
            // We only have hours and minutes in the UI for now
            setHours(hours)
            setMinutes(minutes)
            tapAdd()
        }
    }
    
    fun startTimer(name: String) {
        mainScreen.timerWithName(name).tapPlayPause()
    }
    
    fun startAllTimers() {
        // Assuming "Timer" is the default from createTimer
        startTimer("Timer")
    }
    
    fun waitForCompletion() {
        // Wait enough time for the running timer to finish.
        // This is heuristic.
        waitFor(Duration.ofSeconds(6))
    }

    // Assertions with semantic naming
    // Define the infix assertion on the Duration type
    infix fun Duration.shouldEqual(expected: Duration) {
        assertEquals(expected.seconds, this.seconds)
    }
}

// DSL entry point
fun timerTest(block: TimerTestContext.() -> Unit) {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        TimerTestContext(scenario).apply(block)
    }
}

// Helper function for waiting
fun waitForMillis(millis: Long): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isRoot()
        }

        override fun getDescription(): String {
            return "Wait for $millis milliseconds."
        }

        override fun perform(uiController: UiController, view: View) {
            uiController.loopMainThreadUntilIdle()
            val startTime = System.currentTimeMillis()
            val endTime = startTime + millis

            while (System.currentTimeMillis() < endTime) {
                uiController.loopMainThreadForAtLeast(50)
            }
        }
    }
}
