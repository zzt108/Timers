package com.pneumasoft.multitimer.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.pneumasoft.multitimer.R
import org.hamcrest.Matchers.allOf

class AddTimerDialogRobot : BaseRobot() {
    
    fun enterName(name: String) {
        typeText(R.id.timer_name_edit, name)
    }
    
    fun setHours(hours: Int) {
        // Implementation: click UP 'hours' times
        repeat(hours) {
            tapView(R.id.hours_up_button)
        }
    }
    
    fun setMinutes(minutes: Int) {
        // Set SeekBar progress using helper
        onView(withId(R.id.minutes_slider)).perform(setProgress(minutes))
    }
    
    fun tapAdd() {
        onView(withText("Add")).perform(click())
    }
    
    fun shouldStillBeOpen() {
        onView(withId(R.id.timer_name_edit)).check(matches(isDisplayed()))
    }
    
    // Helper ViewAction for SeekBar
    private fun setProgress(progress: Int): androidx.test.espresso.ViewAction {
        return object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<android.view.View> {
                return isAssignableFrom(android.widget.SeekBar::class.java)
            }
            override fun getDescription(): String {
                return "Set SeekBar progress to $progress"
            }
            override fun perform(uiController: androidx.test.espresso.UiController?, view: android.view.View?) {
                (view as android.widget.SeekBar).progress = progress
            }
        }
    }

    // Support block syntax
    operator fun invoke(block: AddTimerDialogRobot.() -> Unit) {
        this.apply(block)
    }
}
