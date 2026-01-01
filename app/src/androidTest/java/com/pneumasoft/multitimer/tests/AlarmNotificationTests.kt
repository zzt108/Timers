package com.pneumasoft.multitimer.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pneumasoft.multitimer.dsl.seconds
import com.pneumasoft.multitimer.dsl.timerTest
import com.pneumasoft.multitimer.robots.SnoozeDuration.ShortSnooze
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmNotificationTests {
    
    @Test
    fun alarm_should_show_full_screen_notification_when_timer_completes() = timerTest {
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
    fun alarm_should_loop_sound_until_dismissed() = timerTest {
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
    fun snooze_should_restart_timer_with_configured_duration() = timerTest {
        settingsScreen {
            open()
            setSnoozeDuration(ShortSnooze, 30)
            close()
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
}
