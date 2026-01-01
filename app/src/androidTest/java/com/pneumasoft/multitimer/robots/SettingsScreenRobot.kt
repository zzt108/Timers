package com.pneumasoft.multitimer.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.pneumasoft.multitimer.R
import org.hamcrest.Matchers.allOf

class SettingsScreenRobot : BaseRobot() {

    enum class SnoozeDuration {
        ShortSnooze, LongSnooze
    }

    fun open() {
        // Assuming we are on Main Screen, we open settings via menu
        MainScreenRobot().tapSettings()
    }
    
    fun setSnoozeDuration(type: SnoozeDuration, durationSeconds: Int) {
        // This depends on how the Settings screen is implemented (PreferenceFragment or Custom).
        // Assuming standard PreferenceFragment.
        val key = when(type) {
            SnoozeDuration.ShortSnooze -> "snooze_short_duration"
            SnoozeDuration.LongSnooze -> "snooze_long_duration"
        }
        
        // In androidx.preference, we usually click the item by title or key (if possible).
        // Espresso with Preference needs `onData` or specific matchers.
        // Or if it's just a UI list:
        // Find the text corresponding to the setting
        val title = when(type) {
            SnoozeDuration.ShortSnooze -> "Short Snooze Duration"
            SnoozeDuration.LongSnooze -> "Long Snooze Duration"
        }
        
        onView(withText(title)).perform(click())
        
        // Then select the value from the dialog/list
        // This is a simplification. Real implementation needs to know the UI structure.
        // Assuming a ListPreference dialog pops up.
        onView(withText("$durationSeconds seconds")).perform(click())
    }
}
