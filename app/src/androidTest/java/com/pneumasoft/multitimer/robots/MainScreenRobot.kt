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
        tapView(R.id.addTimerButton)
    }
    
    fun tapSettings() {
        openActionBarOverflowOrOptionsMenu(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        onView(withText("Settings")).perform(click())
    }
    
    fun timerWithName(name: String) = TimerItemRobot(name)
    
    fun shouldShowTimerCount(count: Int) {
        onView(withId(R.id.timerRecyclerView))
            .check(matches(hasChildCount(count)))
    }
    
    // Check if we have an empty state view in main activity layout
    // MainActivity.kt does not reference an empty view, it just sets up RecyclerView.
    // If there is one in XML but hidden, we can check it.
    // If not, we might need to rely on child count 0.
    fun shouldShowEmptyState() {
        // verifyVisible(R.id.emptyStateTextView) 
        // Assuming there isn't one based on partial code, but I'll add the method and comment it out or make it check 0 items.
        onView(withId(R.id.timerRecyclerView)).check(matches(hasChildCount(0)))
    }
    
    // Fluent builder for inline assertions
    inline fun verify(block: MainScreenRobot.() -> Unit) {
        this.apply(block)
    }
}
