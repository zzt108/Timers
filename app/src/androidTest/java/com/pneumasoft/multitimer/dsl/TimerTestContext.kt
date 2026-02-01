package com.pneumasoft.multitimer.dsl

import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.pneumasoft.multitimer.MainActivity
import com.pneumasoft.multitimer.robots.AddTimerDialogRobot
import com.pneumasoft.multitimer.robots.AlarmScreenRobot
import com.pneumasoft.multitimer.robots.MainScreenRobot
import com.pneumasoft.multitimer.robots.SettingsScreenRobot
import java.time.Duration
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals

class TimerTestContext(private val scenario: ActivityScenario<MainActivity>) {
    val mainScreen = MainScreenRobot()
    val addTimerDialog = AddTimerDialogRobot()
    val alarmScreen = AlarmScreenRobot()
    val settingsScreen = SettingsScreenRobot()

    fun mainScreen(block: MainScreenRobot.() -> Unit) = mainScreen.apply(block)
    fun addTimerDialog(block: AddTimerDialogRobot.() -> Unit) = addTimerDialog.apply(block)
    fun alarmScreen(block: AlarmScreenRobot.() -> Unit) = alarmScreen.apply(block)
    fun settingsScreen(block: SettingsScreenRobot.() -> Unit) = settingsScreen.apply(block)

    // A hiányzó waitFor implementáció
    fun waitFor(duration: Duration) {
        onView(isRoot()).perform(waitForMillis(duration.toMillis()))
    }

    fun waitForCompletion() {
        // Vár 10 másodpercet (vagy amennyi a teszt timereknek kell)
        waitFor(Duration.ofSeconds(10))
    }

    fun dismissStartupDialogs() {
        try {
            onView(ViewMatchers.withText("Later")).perform(ViewActions.click())
        } catch (e: Exception) {
            // Ignored
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

    private fun waitForMillis(millis: Long): ViewAction = object : ViewAction {
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
