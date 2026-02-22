package com.pneumasoft.multitimer.tests

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.pneumasoft.multitimer.dsl.seconds
import com.pneumasoft.multitimer.dsl.timerTest
import com.pneumasoft.multitimer.robots.SnoozeDuration.ShortSnooze
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmNotificationTests {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun alarm_should_show_full_screen_notification_when_timer_completes() = timerTest {
        createTimer(name = "Alarm Test", duration = 6.seconds)
        mainScreen.timerWithName("Alarm Test").tapPlayPause()

        waitForCompletion()

        alarmScreen {
            shouldBeVisible()
            shouldShowTimerName("Alarm Test")
            shouldPlaySound()
            tapDismiss()
        }
    }

    @Test
    fun alarm_should_loop_sound_until_dismissed() = timerTest {
        createTimer(duration = 4.seconds)
        startAllTimers()

        waitForCompletion()

        alarmScreen.verifySoundLooping()

        // MOVED: waitFor is now called on TimerTestContext, not inside alarmScreen block
        waitFor(2.seconds)

        alarmScreen.shouldStillPlaySound()
        alarmScreen.tapDismiss()
        alarmScreen.shouldNotPlaySound()
    }

    @Test
    fun snooze_should_restart_timer_with_configured_duration() = timerTest {
        settingsScreen {
            open()
            setSnoozeDuration(ShortSnooze, 30)
            close()
        }
        createTimer(name = "Snooze Test", duration = 3.seconds)
        startTimer("Snooze Test")

        waitForCompletion()

        alarmScreen.shouldBeVisible()
        alarmScreen.tapSnooze(ShortSnooze)

        mainScreen {
            timerWithName("Snooze Test").shouldBeRunning()
            timerWithName("Snooze Test").shouldShowRemainingTime(listOf("0:30", "0:29", "0:28"))
        }
    }
}
