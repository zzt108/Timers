package com.pneumasoft.multitimer.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*

class SettingsScreenRobot : BaseRobot() {

    // Using shared SnoozeDuration enum from package

    fun open() {
        // Assuming we are on Main Screen, we open settings via menu
        MainScreenRobot().tapSettings()
    }
    
    fun close() {
        pressBack()
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
            SnoozeDuration.ShortSnooze -> "Short Snooze Button"
            SnoozeDuration.LongSnooze -> "Long Snooze Button"
        }
        
        onView(withText(title)).perform(click())
        
        // Then select the value from the dialog/list
        // The array entries are "30 seconds", "1 minute", etc.
        // But the input durationSeconds is Int.
        // We need to map durationSeconds to the display text.
        val selectionText = when(durationSeconds) {
            30 -> "30 seconds"
            60 -> "1 minute"
            300 -> "5 minutes"
            600 -> "10 minutes"
            else -> "$durationSeconds seconds" // Fallback
        }
        
        onView(withText(selectionText)).perform(click())
    }
}
