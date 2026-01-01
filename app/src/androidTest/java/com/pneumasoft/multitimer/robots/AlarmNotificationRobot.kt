package com.pneumasoft.multitimer.robots

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull

class AlarmNotificationRobot : BaseRobot() {

    enum class SnoozeDuration { ShortSnooze, LongSnooze }

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val timeout = 5000L

    fun shouldBeVisible() {
        device.openNotification()
        device.wait(Until.hasObject(By.text("Timer Complete!")), timeout)

        val notification = device.findObject(By.text("Timer Complete!"))
        assertTrue("Notification should be visible", notification != null)
    }

    fun shouldShowTimerName(name: String) {
        device.openNotification()
        val timerNameElement = device.wait(Until.findObject(By.text(name)), timeout)
        assertNotNull("Timer name should be visible in notification", timerNameElement)
    }

    fun tapDismiss() {
        device.openNotification()
        val dismissButton = device.wait(Until.findObject(By.text("Dismiss")), timeout)
        dismissButton?.click()
        Thread.sleep(500) // Wait for action
    }

    fun tapSnooze(duration: SnoozeDuration) {
        device.openNotification()
        // Find the appropriate snooze button based on text pattern
        val snoozeButton = when (duration) {
            SnoozeDuration.ShortSnooze -> device.findObject(By.textContains("30"))
            SnoozeDuration.LongSnooze -> device.findObject(By.textContains("1m"))
        }
        snoozeButton?.click()
        Thread.sleep(500)
    }

    fun shouldNotBeVisible() {
        device.openNotification()
        val notification = device.findObject(By.text("Timer Complete!"))
        assertTrue("Notification should not be visible", notification == null)
    }
}
