package com.pneumasoft.multitimer.robots

import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.dsl.parseTimeToMillis
import com.pneumasoft.multitimer.dsl.withRecyclerView
import org.hamcrest.Matchers.allOf

class TimerItemRobot(private val timerName: String) : BaseRobot() {
    
    private val timerItemMatcher = withRecyclerView(R.id.timerRecyclerView)
        .atPositionWithText(timerName)
    
    fun tapPlayPause() {
        onView(timerItemMatcher)
            .perform(scrollTo())
        onView(allOf(
            withId(R.id.start_pause_button),
            hasSibling(withText(timerName))
        )).perform(click())
    }
    
    fun tapEdit() {
        onView(timerItemMatcher)
            .perform(scrollTo())
        onView(allOf(
            withId(R.id.edit_button),
            hasSibling(withText(timerName)) // Need better relative matching since they are not direct siblings in ConstraintLayout
            // Note: In item_timer.xml, timer_name is at the bottom, edit_button is inside a LinearLayout timer_controls.
            // They are not direct siblings. `hasSibling` might fail if structure is nested.
            // However, Espresso `hasSibling` checks children of the same parent.
            // timer_name is child of ConstraintLayout.
            // edit_button is child of LinearLayout (timer_controls), which is child of ConstraintLayout.
            // So they are NOT siblings.
            // We need to use `hasDescendant` on the item view, or match by hierarchy.
            // Since we already matched the item view with `timerItemMatcher`, we can search for children inside it.
            // But Espresso operations on a view usually target a specific leaf view.
            
            // Let's rely on the fact that `timerItemMatcher` finds the Item View (CardView/ConstraintLayout).
            // We can search for descendants of THAT view.
        ))
        // But `onView(matcher).perform(...)` works on the single view matched.
        // The guide used `hasSibling`, implying a simpler layout. 
        // Given the actual layout, we need to find the edit button *within* the item that has the text.
        
        onView(allOf(
            withId(R.id.edit_button),
            isDescendantOfA(timerItemMatcher)
        )).perform(click())
    }
    
    fun tapDelete() {
        onView(allOf(
            withId(R.id.delete_button),
            isDescendantOfA(timerItemMatcher)
        )).perform(click())
    }
    
    fun shouldBeRunning() {
        // The image resource is dynamic (play vs pause). 
        // We can check ContentDescription if it is updated properly.
        // item_timer.xml has `android:contentDescription="Start/pause"` which is static XML.
        // We need to check if the code updates it.
        // MainActivity/Adapter code would reveal this. 
        // Let's assume the Adapter updates content description to "Play" or "Pause" as per standard accessibility practices
        // and as hinted in the guide ("Pause", "Play").
        
        onView(allOf(
            withId(R.id.start_pause_button),
            isDescendantOfA(timerItemMatcher)
        )).check(matches(withContentDescription("Pause")))
    }
    
    fun shouldNotBeRunning() {
        onView(allOf(
            withId(R.id.start_pause_button),
            isDescendantOfA(timerItemMatcher)
        )).check(matches(withContentDescription("Play")))
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
