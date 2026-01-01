package com.pneumasoft.multitimer.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.dsl.hasChildCount
import org.hamcrest.Matchers.allOf

class MainScreenRobot : BaseRobot() {
    
    fun tapAddTimer() {
        tapView(R.id.add_timer_button)
    }
    
    fun tapSettings() {
        openActionBarOverflowOrOptionsMenu(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        onView(withText("Settings")).perform(click())
    }
    
    // Updated to support block syntax
    fun timerWithName(name: String, block: (TimerItemRobot.() -> Unit)? = null): TimerItemRobot {
        val robot = TimerItemRobot(name)
        block?.let { robot.apply(it) }
        return robot
    }
    
    fun shouldShowTimerCount(count: Int) {
        onView(withId(R.id.timer_recycler_view))
            .check(matches(hasChildCount(count)))
    }
    
    fun shouldShowEmptyState() {
        onView(withId(R.id.timer_recycler_view)).check(matches(hasChildCount(0)))
    }
    
    // Fluent builder for inline assertions
    operator fun invoke(block: MainScreenRobot.() -> Unit) {
        this.apply(block)
    }
}
