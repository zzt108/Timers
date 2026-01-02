package com.pneumasoft.multitimer.robots

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.dsl.clickChildViewWithId
import com.pneumasoft.multitimer.dsl.parseTimeToMillis
import com.pneumasoft.multitimer.dsl.withRecyclerView
import org.hamcrest.Matchers.allOf

class TimerItemRobot(private val timerName: String) : BaseRobot() {

    // Ensure this ID matches your XML (likely R.id.timer_recycler_view or timerrecyclerview)
    private val timerItemMatcher = withRecyclerView(R.id.timer_recycler_view)
        .atPositionWithText(timerName)

    fun tapPlayPause() {
        // First wait for RecyclerView to settle
        Thread.sleep(500)

        // Find the position of the timer item
        val position = findTimerPosition()

        // Perform action at specific position
        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    clickChildViewWithId(R.id.start_pause_button)
                )
            )
    }

    private fun findTimerPosition(): Int {
        var itemPosition = -1
        onView(withId(R.id.timer_recycler_view))
            .check { view, noViewFoundException ->
                if (noViewFoundException != null) {
                    throw noViewFoundException
                }
                val recyclerView = view as RecyclerView
                for (i in 0 until recyclerView.adapter!!.itemCount) {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
                    val itemView = viewHolder?.itemView
                    val nameView = itemView?.findViewById<TextView>(R.id.timer_name)
                    if (nameView?.text?.toString() == timerName) {
                        itemPosition = i
                        break
                    }
                }
            }

        if (itemPosition == -1) {
            throw AssertionError("Timer with name '$timerName' not found in RecyclerView")
        }

        return itemPosition
    }

    fun tapEdit() {
        val position = findTimerPosition()
        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    clickChildViewWithId(R.id.edit_button)
                )
            )
    }

    fun tapDelete() {
        val position = findTimerPosition()
        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    clickChildViewWithId(R.id.delete_button)
                )
            )
    }

    fun shouldBeRunning() {
        onView(allOf(
            withId(R.id.start_pause_button),
            isDescendantOfA(timerItemMatcher)
        )).check(matches(withContentDescription("Pause")))
    }

    fun shouldNotBeRunning() {
        val position = findTimerPosition()
        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    clickChildViewWithId(R.id.start_pause_button)  // Use this pattern
                )
            )
    }

    fun shouldShowRemainingTime(time: String) {
        onView(allOf(
            withId(R.id.timer_display),
            isDescendantOfA(timerItemMatcher)
        )).check(matches(withText(time)))
    }

    fun getRemainingMillis(): Long {
        var remaining = 0L
        onView(allOf(
            withId(R.id.timer_display),
            isDescendantOfA(timerItemMatcher)
        )).check { view, _ ->
            val textView = view as TextView
            remaining = parseTimeToMillis(textView.text.toString())
        }
        return remaining
    }
}
