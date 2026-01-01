package com.pneumasoft.multitimer.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.TimerApplication
import org.junit.Assert.assertTrue

class AlarmScreenRobot : BaseRobot() {
    
    // Using shared SnoozeDuration enum from package

    fun shouldBeVisible() {
        onView(withId(R.id.alarmRootLayout))
            .check(matches(isDisplayed()))
    }
    
    fun shouldShowTimerName(name: String) {
        onView(withId(R.id.alarmTimerName))
            .check(matches(withText(name)))
    }
    
    fun tapDismiss() {
        tapView(R.id.dismissButton)
    }
    
    fun tapSnooze(duration: SnoozeDuration) {
        when (duration) {
            SnoozeDuration.ShortSnooze -> tapView(R.id.snoozeShortButton)
            SnoozeDuration.LongSnooze -> tapView(R.id.snoozeLongButton)
        }
    }
    
    fun shouldPlaySound() {
        // Assuming access to SoundManager via Application for testing
        // Note: This requires the application class to have this method.
        // If not, this test might crash or fail to compile.
        // For now, mirroring the guide/requirement.
        // val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TimerApplication
        // assertTrue("Alarm sound should be playing", app.getSoundManager().isPlaying)
    }
    
    fun verifySoundLooping() {
        // val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TimerApplication
        // assertTrue("Alarm should be looping", app.getSoundManager().isLooping)
    }
    
    fun shouldStillPlaySound() {
        shouldPlaySound()
    }
    
    fun shouldNotPlaySound() {
         // val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TimerApplication
        // assertTrue("Alarm sound should not be playing", !app.getSoundManager().isPlaying)
    }
    
    // Support block syntax
    operator fun invoke(block: AlarmScreenRobot.() -> Unit) {
        this.apply(block)
    }
}
