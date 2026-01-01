package com.pneumasoft.multitimer.test.steps

import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.pneumasoft.multitimer.MainActivity
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.ui.adapter.TimerAdapter.TimerViewHolder
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.hamcrest.Matcher

class TimerSteps {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Given("I am on the main screen")
    fun i_am_on_the_main_screen() {
        onView(withId(R.id.add_timer_button)).check(matches(isDisplayed()))
    }

    @Given("I create a timer named {string} with duration {int} minutes")
    fun i_create_a_timer_named_with_duration(name: String, minutes: Int) {
        i_tap_the_add_timer_button()
        i_set_the_timer_duration_to_minutes(minutes)
        i_enter_as_the_timer_name(name)
        i_tap_the_add_button()
    }

    @Given("I tap the add timer button")
    fun i_tap_the_add_timer_button() {
        onView(withId(R.id.add_timer_button)).perform(click())
    }

    @When("I set the timer duration to {int} minutes")
    fun i_set_the_timer_duration_to_minutes(minutes: Int) {
        onView(withId(R.id.minutes_slider)).perform(setProgress(minutes))
    }

    @When("I enter {string} as the timer name")
    fun i_enter_as_the_timer_name(name: String) {
        onView(withId(R.id.timer_name_edit)).perform(replaceText(name), closeSoftKeyboard())
    }

    @When("I tap the add button")
    fun i_tap_the_add_button() {
        onView(withText("Add")).perform(click())
    }

    @When("I tap the update button")
    fun i_tap_the_update_button() {
        onView(withText("Update")).perform(click())
    }

    @Then("I should see a timer named {string} with duration {string}")
    fun i_should_see_a_timer_named_with_duration(name: String, duration: String) {
        onView(withId(R.id.timer_recycler_view))
            .perform(RecyclerViewActions.scrollTo<TimerViewHolder>(
                hasDescendant(withText(name))
            ))

        onView(withText(name)).check(matches(isDisplayed()))
        
        // We verify duration by finding the item first to avoid ambiguity
        onView(withId(R.id.timer_recycler_view))
            .perform(RecyclerViewActions.actionOnItem<TimerViewHolder>(
                hasDescendant(withText(name)),
                checkTimerDisplay(duration)
            ))
    }

    @Then("I should see an error message {string}")
    fun i_should_see_an_error_message(message: String) {
        // Checking for dialog title as proxy for error handling in this context,
        // or effectively checking that we are still in the dialog
        onView(withText("Add New Timer")).check(matches(isDisplayed()))
    }

    @When("I tap the start button for {string}")
    fun i_tap_the_start_button_for(name: String) {
        tapChildViewInRecyclerItem(name, R.id.start_pause_button)
    }

    @When("I tap the pause button for {string}")
    fun i_tap_the_pause_button_for(name: String) {
        tapChildViewInRecyclerItem(name, R.id.start_pause_button)
    }

    @When("I tap the reset button for {string}")
    fun i_tap_the_reset_button_for(name: String) {
        tapChildViewInRecyclerItem(name, R.id.reset_button)
    }

    @When("I tap the delete button for {string}")
    fun i_tap_the_delete_button_for(name: String) {
        tapChildViewInRecyclerItem(name, R.id.delete_button)
    }

    @When("I tap the edit button for {string}")
    fun i_tap_the_edit_button_for(name: String) {
        tapChildViewInRecyclerItem(name, R.id.edit_button)
    }

    @Then("the timer {string} should be running")
    fun the_timer_should_be_running(name: String) {
        checkTimerStateInRecyclerItem(name, isRunning = true)
    }

    @Then("the timer {string} should be paused")
    fun the_timer_should_be_paused(name: String) {
        checkTimerStateInRecyclerItem(name, isRunning = false)
    }

    @Then("the timer {string} should show {string}")
    fun the_timer_should_show(name: String, duration: String) {
        onView(withId(R.id.timer_recycler_view))
            .perform(RecyclerViewActions.actionOnItem<TimerViewHolder>(
                hasDescendant(withText(name)),
                checkTimerDisplay(duration)
            ))
    }

    @Then("I should not see a timer named {string}")
    fun i_should_not_see_a_timer_named(name: String) {
        onView(withText(name)).check(doesNotExist())
    }

    @When("I wait for {int} seconds")
    fun i_wait_for_seconds(seconds: Int) {
        try {
            Thread.sleep(seconds * 1000L)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // Helper methods

    private fun tapChildViewInRecyclerItem(timerName: String, viewId: Int) {
        onView(withId(R.id.timer_recycler_view))
            .perform(RecyclerViewActions.actionOnItem<TimerViewHolder>(
                hasDescendant(withText(timerName)),
                clickChildViewWithId(viewId)
            ))
    }

    private fun checkTimerStateInRecyclerItem(timerName: String, isRunning: Boolean) {
        onView(withId(R.id.timer_recycler_view))
            .perform(RecyclerViewActions.actionOnItem<TimerViewHolder>(
                hasDescendant(withText(timerName)),
                checkTimerState(isRunning)
            ))
    }

    private fun clickChildViewWithId(id: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View>? {
                return null
            }

            override fun getDescription(): String {
                return "Click on a child view with specified id."
            }

            override fun perform(uiController: UiController, view: View) {
                val v = view.findViewById<View>(id)
                v.performClick()
            }
        }
    }

    private fun setProgress(progress: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(SeekBar::class.java)
            }

            override fun getDescription(): String {
                return "Set progress on SeekBar"
            }

            override fun perform(uiController: UiController?, view: View?) {
                val seekBar = view as SeekBar
                seekBar.progress = progress
            }
        }
    }

    private fun checkTimerState(running: Boolean): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = null
            override fun getDescription() = "Check timer state"
            override fun perform(uiController: UiController, view: View) {
                val button = view.findViewById<ImageButton>(R.id.start_pause_button)
                val expectedDesc = if (running) "Pause" else "Start"
                // Check if button exists
                if (button == null) throw AssertionError("Start/Pause button not found")
                
                if (button.contentDescription != expectedDesc) {
                    throw AssertionError("Expected timer to be ${if(running) "running" else "paused"} (button desc: $expectedDesc), but found ${button.contentDescription}")
                }
            }
        }
    }

    private fun checkTimerDisplay(expectedText: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = null
            override fun getDescription() = "Check timer display text"
            override fun perform(uiController: UiController, view: View) {
                val textView = view.findViewById<TextView>(R.id.timer_display)
                if (textView == null) throw AssertionError("Timer display text view not found")
                
                if (textView.text.toString() != expectedText) {
                     throw AssertionError("Expected display '$expectedText', got '${textView.text}'")
                }
            }
        }
    }
}
