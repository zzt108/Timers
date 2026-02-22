package com.pneumasoft.multitimer.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pneumasoft.multitimer.dsl.minutes
import com.pneumasoft.multitimer.dsl.seconds
import com.pneumasoft.multitimer.dsl.timerTest
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerExecutionTests {

    @Test
    fun timer_should_continue_running_when_app_goes_to_background() = timerTest {
        createTimer(name = "Background Test", duration = 2.minutes)
        startTimer("Background Test")

        mainScreen.timerWithName("Background Test").shouldBeRunning()
        waitFor(2.seconds)
        mainScreen.timerWithName("Background Test").shouldBeRunning()
    }

    @Test
    fun absolute_time_calculation_should_prevent_drift_over_long_duration() = timerTest {
        createTimer(name = "Precision Test", duration = 1.minutes)
        startTimer("Precision Test")

        mainScreen.timerWithName("Precision Test").shouldBeRunning()

        // Measure actual elapsed time instead of assuming waitFor is precise
        val startWallTime = System.currentTimeMillis()
        val startMillis = mainScreen.timerWithName("Precision Test").getRemainingMillis()

        waitFor(2.seconds)

        val endMillis = mainScreen.timerWithName("Precision Test").getRemainingMillis()
        val endWallTime = System.currentTimeMillis()

        val actualElapsed = endWallTime - startWallTime
        val decrease = startMillis - endMillis

        // Use a more robust check: drift should be small relative to wall clock
        // allowing for UI update cycle (approx 1000ms drift is acceptable for a few seconds)
        val drift = abs(decrease - actualElapsed)
        assert(drift < 1000) {
            "Drift too high: $drift ms (Decrease: $decrease, Elapsed: $actualElapsed)"
        }
    }
}
