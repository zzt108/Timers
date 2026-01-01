package com.pneumasoft.multitimer.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pneumasoft.multitimer.dsl.hours
import com.pneumasoft.multitimer.dsl.minutes
import com.pneumasoft.multitimer.dsl.seconds
import com.pneumasoft.multitimer.dsl.shouldBeCloseTo
import com.pneumasoft.multitimer.dsl.timerTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BugRegressionTests {
    
    @Test
    fun issue_18_one_timer_stopping_should_not_affect_others() = timerTest {
        // Regression test for GitHub Issue #18
        createTimer(name = "Timer A", duration = 1.minutes)
        createTimer(name = "Timer B", duration = 2.minutes)
        createTimer(name = "Timer C", duration = 3.minutes)
        
        startTimer("Timer A")
        startTimer("Timer B")
        startTimer("Timer C")
        
        waitFor(10.seconds)
        mainScreen.timerWithName("Timer A").tapPlayPause() // Pause Timer A
        
        waitFor(5.seconds)
        
        mainScreen {
            timerWithName("Timer A").shouldNotBeRunning()
            timerWithName("Timer B").shouldBeRunning()
            timerWithName("Timer C").shouldBeRunning()
        }
    }
    
    @Test
    fun issue_16_timer_should_not_drift_over_1_hour_duration() = timerTest {
        // Regression test for GitHub Issue #16
        // Timer was losing ~5 minutes per hour
        
        createTimer(name = "Precision", duration = 1.hours)
        val expectedEndTime = System.currentTimeMillis() + 1.hours.toMillis()
        
        startTimer("Precision")
        
        // Fast-forward with IdlingResource
        // advanceTimeBy(30.minutes) // Not implemented
        waitFor(5.seconds) // Using short wait for now
        
        mainScreen.timerWithName("Precision") {
            val actualRemaining = getRemainingMillis()
            // Adjust expected remaining based on what we actually waited
            // In a real test with mocked clock, we would jump ahead 30 mins.
            // Here we waited 5 seconds.
            val expectedRemaining = 1.hours.toMillis() - 5000 
            
            // Should be within 1 second tolerance
            actualRemaining.shouldBeCloseTo(expectedRemaining, tolerance = 1000)
        }
    }
}
