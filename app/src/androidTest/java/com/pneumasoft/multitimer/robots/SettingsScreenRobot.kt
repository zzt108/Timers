package com.pneumasoft.multitimer.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*

class SettingsScreenRobot : BaseRobot() {

    // Using shared SnoozeDuration enum from package

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
        // Find the text corresponding to the setting
        val title = when(type) {
            SnoozeDuration.ShortSnooze -> "Short Snooze Duration"
            SnoozeDuration.LongSnooze -> "Long Snooze Duration"
        }
        
        onView(withText(title)).perform(click())
        
        // Then select the value from the dialog/list
        onView(withText("$durationSeconds seconds")).perform(click())
    }
}
