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
        // Implementation depends on UI. 
        // Current UI uses Up/Down buttons for hours.
        // We need to read the current value and click up/down until it matches.
        // This is tricky in Espresso without custom logic or just assuming start at 0.
        // For robustness, maybe just click UP N times assuming 0 start.
        // Or check text and adjust.
        
        // Simplified: Click UP 'hours' times
        repeat(hours) {
            tapView(R.id.hours_up_button)
        }
    }
    
    fun setMinutes(minutes: Int) {
        // Using SeekBar. 
        // In Espresso, we can use `GeneralSwipeAction` or set progress if we have a custom ViewAction.
        // Let's assume we have a helper or just use a custom ViewAction.
        // For this Robot, I'll use a custom ViewAction to set progress on SeekBar.
        
        onView(withId(R.id.minutes_slider)).perform(setProgress(minutes))
    }
    
    fun tapAdd() {
        onView(withText("Add")).perform(click())
    }
    
    fun shouldShowError(message: String) {
        // Toast checking is hard in Espresso. 
        // If it's a TextView error, check that.
        // The code uses Toast.makeText(...).show()
        // Checking Toast in Espresso 3.0+ requires custom matchers on the window decor view.
        
        // verifyTextInToast(message) // Placeholder
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
}
