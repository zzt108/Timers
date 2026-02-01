package com.pneumasoft.multitimer.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.TimerApplication
import org.junit.Assert.assertTrue

class AlarmScreenRobot : BaseRobot() {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val timeout = 10000L

    fun shouldBeVisible() {
        // Wait for AlarmActivity to appear
        val alarmLayout = device.wait(
            Until.findObject(By.res("com.pneumasoft.multitimer", "alarmRootLayout")),
            timeout
        )
        assertTrue("Alarm screen should be visible", alarmLayout != null)
    }

    fun shouldShowTimerName(name: String) {
        // Find the timer name text in the alarm screen
        val timerNameElement = device.findObject(By.text(name))
        assertTrue("Timer name '$name' should be visible in alarm screen", timerNameElement != null)
    }

    fun tapDismiss() {
        val dismissButton = device.findObject(By.res("com.pneumasoft.multitimer", "dismissButton"))
        assertTrue("Dismiss button should exist", dismissButton != null)
        dismissButton?.click()
        Thread.sleep(500) // Wait for dismiss action
    }

    fun tapSnooze(duration: SnoozeDuration) {
        when (duration) {
            SnoozeDuration.ShortSnooze -> {
                val snoozeButton = device.findObject(By.res("com.pneumasoft.multitimer", "snoozeShortButton"))
                assertTrue("Snooze short button should exist", snoozeButton != null)
                snoozeButton?.click()
            }
            SnoozeDuration.LongSnooze -> {
                val snoozeButton = device.findObject(By.res("com.pneumasoft.multitimer", "snoozeLongButton"))
                assertTrue("Snooze long button should exist", snoozeButton != null)
                snoozeButton?.click()
            }
        }
        Thread.sleep(500) // Wait for snooze action
    }

    fun shouldPlaySound() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TimerApplication
        assertTrue("Alarm sound should be playing", app.getSoundManager().isPlaying())
    }

    fun verifySoundLooping() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TimerApplication
        assertTrue("Alarm should be looping", app.getSoundManager().isLooping())
    }

    fun shouldStillPlaySound() {
        shouldPlaySound()
    }

    fun shouldNotPlaySound() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TimerApplication
        assertTrue("Alarm sound should not be playing", !app.getSoundManager().isPlaying())
    }

    // Support block syntax
    operator fun invoke(block: AlarmScreenRobot.() -> Unit) = this.apply(block)
}
