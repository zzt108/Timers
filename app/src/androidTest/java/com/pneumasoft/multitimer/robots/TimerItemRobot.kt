package com.pneumasoft.multitimer.robots

import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.dsl.parseTimeToMillis
import com.pneumasoft.multitimer.dsl.withRecyclerView
import org.hamcrest.Matchers.allOf

class TimerItemRobot(private val timerName: String) : BaseRobot() {
    
    // We need to match the item view in the recycler view that contains the name.
    // withRecyclerView is a custom matcher (implied by DSL).
    // If not available, we have to construct it. But let's assume it works as imported.
    private val timerItemMatcher = withRecyclerView(R.id.timer_recycler_view)
        .atPositionWithText(timerName)
    
    fun tapPlayPause() {
        // We find the play/pause button INSIDE the item that matches the timer name
        onView(allOf(
            withId(R.id.start_pause_button),
            isDescendantOfA(hasDescendant(withText(timerName)))
        )).perform(click())
    }
    
    fun tapEdit() {
        onView(allOf(
            withId(R.id.edit_button),
            isDescendantOfA(hasDescendant(withText(timerName)))
        )).perform(click())
    }
    
    fun tapDelete() {
        onView(allOf(
            withId(R.id.delete_button),
            isDescendantOfA(hasDescendant(withText(timerName)))
        )).perform(click())
    }
    
    fun shouldBeRunning() {
        // Assuming the adapter updates the content description or icon
        // For simplicity, checking if content description is "Pause" (meaning it is running and can be paused)
        onView(allOf(
            withId(R.id.start_pause_button),
            isDescendantOfA(hasDescendant(withText(timerName)))
        )).check(matches(withContentDescription("Pause")))
    }
    
    fun shouldNotBeRunning() {
        onView(allOf(
            withId(R.id.start_pause_button),
            isDescendantOfA(hasDescendant(withText(timerName)))
        )).check(matches(withContentDescription("Play")))
    }
    
    fun shouldShowRemainingTime(time: String) {
        onView(allOf(
            withId(R.id.timer_display),
            isDescendantOfA(hasDescendant(withText(timerName)))
        )).check(matches(withText(time)))
    }
    
    // Support block syntax for cleaner tests
    operator fun invoke(block: TimerItemRobot.() -> Unit) {
        this.apply(block)
    }

    fun getRemainingMillis(): Long {
        var remaining = 0L
        onView(allOf(
            withId(R.id.timer_display),
            isDescendantOfA(hasDescendant(withText(timerName)))
        )).check { view, _ ->
            val textView = view as TextView
            remaining = parseTimeToMillis(textView.text.toString())
        }
        return remaining
    }
}
