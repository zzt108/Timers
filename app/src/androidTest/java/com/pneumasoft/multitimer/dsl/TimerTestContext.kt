package com.pneumasoft.multitimer.dsl

import android.util.Log
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.pneumasoft.multitimer.MainActivity
import com.pneumasoft.multitimer.robots.AddTimerDialogRobot
import com.pneumasoft.multitimer.robots.AlarmScreenRobot
import com.pneumasoft.multitimer.robots.MainScreenRobot
import com.pneumasoft.multitimer.robots.SettingsScreenRobot
import java.time.Duration
import org.hamcrest.Matcher

fun timerTest(block: TimerTestContext.() -> Unit) {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        TimerTestContext(scenario).apply {
            dismissStartupDialogs()
            deleteAllTimers()
            block()
        }
    }
}

class TimerTestContext(private val scenario: ActivityScenario<MainActivity>) {

    val mainScreen = MainScreenRobot()
    val addTimerDialog = AddTimerDialogRobot()
    val alarmScreen = AlarmScreenRobot()
    val settingsScreen = SettingsScreenRobot()

    fun mainScreen(block: MainScreenRobot.() -> Unit) = mainScreen.apply(block)
    fun addTimerDialog(block: AddTimerDialogRobot.() -> Unit) = addTimerDialog.apply(block)
    fun alarmScreen(block: AlarmScreenRobot.() -> Unit) = alarmScreen.apply(block)
    fun settingsScreen(block: SettingsScreenRobot.() -> Unit) = settingsScreen.apply(block)

    // Implementation of waitFor
    fun waitFor(duration: Duration) {
        onView(isRoot()).perform(waitForMillis(duration.toMillis()))
    }

    fun waitForCompletion() {
        // Wait for 20 seconds (accommodating the 15s minimum timer)
        waitFor(Duration.ofSeconds(20))
    }

    fun dismissStartupDialogs() {
        // Dismiss "Later" buttons (Battery, Exact Alarm)
        repeat(3) {
            try {
                onView(ViewMatchers.withText("Later")).perform(ViewActions.click())
                Thread.sleep(500)
            } catch (e: Exception) {}
        }

        // Handle system permission dialogs (POST_NOTIFICATIONS)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val allowButton = device.findObject(By.text("Allow"))
        if (allowButton != null) {
            allowButton.click()
        }
    }

    fun deleteAllTimers() {
        scenario.onActivity { activity ->
            val viewModel =
                    androidx.lifecycle.ViewModelProvider(activity)
                            .get(com.pneumasoft.multitimer.viewmodel.TimerViewModel::class.java)
            viewModel.clearAllTimers()
        }

        // Poll for empty state with timeout
        val startTime = System.currentTimeMillis()
        val timeout = 5000L // 5 seconds
        var isEmpty = false

        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                mainScreen.shouldShowEmptyState()
                isEmpty = true
                break
            } catch (e: Throwable) {
                // Not empty yet, wait a bit
                Thread.sleep(200)
            }
        }

        if (!isEmpty) {
            Log.e("TimerTestContext", "Timeout waiting for all timers to be deleted")
        }
    }

    fun createTimer(name: String = "Timer", duration: Duration) {
        mainScreen.tapAddTimer()
        addTimerDialog.apply {
            enterName(name)
            val hours = duration.toHours().toInt()
            val minutes = (duration.toMinutes() % 60).toInt()
            setHours(hours)
            setMinutes(minutes)
            tapAdd()
        }
    }

    fun startTimer(name: String) = mainScreen.timerWithName(name).tapPlayPause()
    fun startAllTimers() = startTimer("Timer")

    private fun waitForMillis(millis: Long): ViewAction =
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isRoot()
                override fun getDescription(): String = "Wait for $millis milliseconds."
                override fun perform(uiController: UiController?, view: View?) {
                    uiController?.loopMainThreadUntilIdle()
                    val startTime = System.currentTimeMillis()
                    val endTime = startTime + millis
                    while (System.currentTimeMillis() < endTime) {
                        uiController?.loopMainThreadForAtLeast(50)
                    }
                }
            }
}
