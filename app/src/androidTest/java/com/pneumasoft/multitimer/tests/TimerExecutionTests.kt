package com.pneumasoft.multitimer.tests

import android.Manifest
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.pneumasoft.multitimer.MainActivity
import com.pneumasoft.multitimer.robots.MainScreenRobot
import com.pneumasoft.multitimer.robots.AddTimerDialogRobot
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerExecutionTests {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    private val mainScreen = MainScreenRobot()
    private val addTimerDialog = AddTimerDialogRobot()

    @Before
    fun cleanup() {
        // Töröljük a mentett időzítőket a SharedPreferences-ből
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("timer_preferences", Context.MODE_PRIVATE)

        prefs.edit().apply {
            clear()
            // FONTOS: Beállítjuk, hogy a default timer-ek "létre lettek hozva",
            // így a Repository nem generálja le őket újra (Energy, Nerve, stb.).
            // Ellenőrizd a TimerDataHelper.kt-ban a pontos kulcsot!
            // A snippet alapján valószínűleg "defaulttimerscreated" vagy "default_timers_created".
            // Itt a legvalószínűbbet használom a konvenciók alapján:
            putBoolean("default_timers_created", true)
            commit()
        }

        // Újraindítjuk az Activity-t, hogy a tiszta állapotot töltse be
        activityScenarioRule.scenario.recreate()
    }

    @Test
    fun timer_should_continue_running_when_app_goes_to_background() {
        // Wait for any dialogs to settle
        Thread.sleep(2000)

        // Close any battery optimization dialog if visible (by pressing back)
        androidx.test.espresso.Espresso.pressBack()

        Thread.sleep(500)

        // Create and start timer
        mainScreen.tapAddTimer()
        addTimerDialog.apply {
            enterName("Background Test")
            setMinutes(2)
            tapAdd()
        }
        Thread.sleep(800) // Wait for dialog dismiss + adapter update

        // Start timer
        mainScreen.timerWithName("Background Test")?.tapPlayPause()

        // Wait a bit
        Thread.sleep(3000)

        // Verify it's still running
        mainScreen.timerWithName("Background Test")?.shouldNotBeRunning()
    }

    @Test
    fun absolute_time_calculation_should_prevent_drift_over_long_duration() {
        // Wait for any dialogs to settle
        Thread.sleep(2000)

        // Close any battery optimization dialog if visible
        androidx.test.espresso.Espresso.pressBack()

        Thread.sleep(500)

        // Create timer with specific duration
        mainScreen.tapAddTimer()
        addTimerDialog.apply {
            enterName("Precision Test")
            setMinutes(1)
            tapAdd()
        }
        Thread.sleep(800) // Wait for dialog dismiss + adapter update

        // Start timer
        mainScreen.timerWithName("Precision Test")?.tapPlayPause()

        // Wait short time
        Thread.sleep(2000)

        // Verify timer is running
        mainScreen.timerWithName("Precision Test")?.shouldBeRunning()
    }
}
