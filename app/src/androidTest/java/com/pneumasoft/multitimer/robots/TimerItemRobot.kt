package com.pneumasoft.multitimer.robots

import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.dsl.clickChildViewWithId
import com.pneumasoft.multitimer.dsl.parseTimeToMillis
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue

class TimerItemRobot(private val timerName: String) : BaseRobot() {

    /**
     * Taps the play/pause button for this timer.
     * Uses declarative matching with auto-scroll instead of manual position lookup.
     */
    fun tapPlayPause() {
        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(timerName)),
                    clickChildViewWithId(R.id.start_pause_button)
                )
            )
    }

    /**
     * Taps the edit button for this timer.
     */
    fun tapEdit() {
        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(timerName)),
                    clickChildViewWithId(R.id.edit_button)
                )
            )
    }

    /**
     * Taps the delete button for this timer.
     */
    fun tapDelete() {
        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(timerName)),
                    clickChildViewWithId(R.id.delete_button)
                )
            )
    }

    /**
     * Asserts that the timer is currently running (shows "Pause" button).
     */
    fun shouldBeRunning() {
        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText(timerName))
                )
            )
            .check { view, _ ->
                val recyclerView = view as RecyclerView
                val position = findTimerPositionInView(recyclerView, timerName)
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                val itemView = viewHolder?.itemView
                val button = itemView?.findViewById<ImageButton>(R.id.start_pause_button)
                val description = button?.contentDescription?.toString()

                assertTrue(
                    "Expected 'Pause' button (running), but got '$description'",
                    description == "Pause"
                )
            }
    }

    /**
     * Asserts that the timer is NOT running (shows "Play" button).
     */
    fun shouldNotBeRunning() {
        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText(timerName))
                )
            )
            .check { view, _ ->
                val recyclerView = view as RecyclerView
                val position = findTimerPositionInView(recyclerView, timerName)
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                val itemView = viewHolder?.itemView
                val button = itemView?.findViewById<ImageButton>(R.id.start_pause_button)
                val description = button?.contentDescription?.toString()

                assertTrue(
                    "Expected 'Play' button (not running), but got '$description'",
                    description == "Play"
                )
            }
    }

    /**
     * Asserts that the timer displays the expected remaining time.
     */
    fun shouldShowRemainingTime(time: String) {
        onView(
            allOf(
                withId(R.id.timer_display),
                hasSibling(withText(timerName))
            )
        ).check(matches(withText(time)))
    }

    /**
     * Returns the remaining time in milliseconds for this timer.
     */
    fun getRemainingMillis(): Long {
        var remaining = 0L

        onView(withId(R.id.timer_recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText(timerName))
                )
            )
            .check { view, _ ->
                val recyclerView = view as RecyclerView
                val position = findTimerPositionInView(recyclerView, timerName)
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                val itemView = viewHolder?.itemView
                val textView = itemView?.findViewById<TextView>(R.id.timer_display)
                remaining = parseTimeToMillis(textView?.text.toString())
            }

        return remaining
    }

    /**
     * Helper method to find timer position AFTER scrolling has made it visible.
     * Only called from within a .check { } block where the item is guaranteed visible.
     */
    private fun findTimerPositionInView(recyclerView: RecyclerView, name: String): Int {
        for (i in 0 until (recyclerView.adapter?.itemCount ?: 0)) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
            val itemView = viewHolder?.itemView
            val nameView = itemView?.findViewById<TextView>(R.id.timer_name)
            if (nameView?.text?.toString() == name) {
                return i
            }
        }
        throw AssertionError("Timer with name '$name' not found after scrolling")
    }
}
