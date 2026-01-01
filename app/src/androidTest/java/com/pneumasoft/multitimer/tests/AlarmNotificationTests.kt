package com.pneumasoft.multitimer.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pneumasoft.multitimer.dsl.seconds
import com.pneumasoft.multitimer.dsl.timerTest
import com.pneumasoft.multitimer.robots.SettingsScreenRobot.SnoozeDuration.ShortSnooze
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmNotificationTests {
    
    @Test
    fun `alarm should show full-screen notification when timer completes`() = timerTest {
        createTimer(name = "Alarm Test", duration = 5.seconds)
        mainScreen.timerWithName("Alarm Test").tapPlayPause()
        
        waitForCompletion()
        
        alarmScreen {
            shouldBeVisible()
            shouldShowTimerName("Alarm Test")
            shouldPlaySound()
        }
    }
    
    @Test
    fun `alarm should loop sound until dismissed`() = timerTest {
        createTimer(duration = 3.seconds)
        startAllTimers()
        
        waitForCompletion()
        
        alarmScreen {
            verifySoundLooping()
            waitFor(5.seconds) // Sound should still be playing
            shouldStillPlaySound()
            
            tapDismiss()
            shouldNotPlaySound()
        }
    }
    
    @Test
    fun `snooze should restart timer with configured duration`() = timerTest {
        settingsScreen {
            open()
            setSnoozeDuration(ShortSnooze, 30)
        }
        
        createTimer(name = "Snooze Test", duration = 2.seconds)
        startTimer("Snooze Test")
        waitForCompletion()
        
        alarmScreen {
            tapSnooze(ShortSnooze)
        }
        
        mainScreen {
            timerWithName("Snooze Test") {
                shouldBeRunning()
                shouldShowRemainingTime("0:30")
            }
        }
    }

    // Helper extensions to match the guide's DSL usage
    private fun com.pneumasoft.multitimer.dsl.TimerTestContext.createTimer(name: String = "Timer 1", duration: java.time.Duration) {
        // Implementation using robots
        mainScreen.tapAddTimer()
        addTimerDialog.apply {
            enterName(name)
            // Duration split
            val hours = duration.toHours().toInt()
            val minutes = duration.toMinutesPart()
            setHours(hours)
            setMinutes(minutes)
            tapAdd()
        }
    }

    private fun com.pneumasoft.multitimer.dsl.TimerTestContext.startTimer(name: String) {
        mainScreen.timerWithName(name).tapPlayPause()
    }
    
    private fun com.pneumasoft.multitimer.dsl.TimerTestContext.startAllTimers() {
        // Assuming we only have one or we loop
        // For this test context, maybe just start the last created?
        // Or implement loop in MainScreenRobot
        startTimer("Timer 1") // Default
    }

    private fun com.pneumasoft.multitimer.dsl.TimerTestContext.waitForCompletion() {
        // Wait enough time
        waitFor(6.seconds)
    }
}
