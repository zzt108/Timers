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

        // Bezárjuk a panelt, hogy ne zavarja a további teszteket
        device.pressBack()
    }

    fun shouldShowTimerName(name: String) {
        device.openNotification()
        val timerNameElement = device.wait(Until.findObject(By.text(name)), timeout)
        assertNotNull("Timer name should be visible in notification", timerNameElement)

        device.pressBack()
    }

    fun tapDismiss() {
        device.openNotification()
        val dismissButton = device.wait(Until.findObject(By.text("Dismiss")), timeout)

        assertNotNull("Dismiss button not found in notification", dismissButton)
        dismissButton.click()

        // Várunk kicsit és biztosítjuk, hogy a panel bezáródjon
        Thread.sleep(500)
        closeNotificationShade()
    }

    fun tapSnooze(duration: SnoozeDuration) {
        device.openNotification()

        // Megvárjuk, amíg az értesítés biztosan betöltődik, különben a findObject null-t adhat
        device.wait(Until.hasObject(By.text("Timer Complete!")), timeout)

        val snoozeButton = when (duration) {
            SnoozeDuration.ShortSnooze -> device.findObject(By.textContains("30"))
            SnoozeDuration.LongSnooze -> device.findObject(By.textContains("1m"))
        }

        assertNotNull("Snooze button for $duration not found. Check if notification text matches '30' or '1m'.", snoozeButton)

        snoozeButton.click()

        // Várunk a műveletre, majd explicit bezárjuk a panelt
        Thread.sleep(500)
        closeNotificationShade()
    }

    fun shouldNotBeVisible() {
        device.openNotification()
        val notification = device.findObject(By.text("Timer Complete!"))
        assertTrue("Notification should not be visible", notification == null)

        device.pressBack()
    }

    private fun closeNotificationShade() {
        // Kényszerített bezárás, hogy az Espresso visszakapja a vezérlést az app felett
        device.pressBack()
    }

    // Support block syntax
    operator fun invoke(block: AlarmNotificationRobot.() -> Unit) {
        this.apply(block)
    }
}
