package com.pneumasoft.multitimer.robots

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

abstract class BaseRobot {
    // Common actions
    protected fun tapView(@IdRes id: Int) {
        onView(withId(id)).perform(click())
    }
    
    protected fun typeText(@IdRes id: Int, text: String) {
        onView(withId(id)).perform(replaceText(text), closeSoftKeyboard())
    }
    
    protected fun verifyVisible(@IdRes id: Int) {
        onView(withId(id)).check(matches(isDisplayed()))
    }
    
    protected fun verifyText(@IdRes id: Int, text: String) {
        onView(withId(id)).check(matches(withText(text)))
    }

    // Semantic assertions
    infix fun Int.shouldEqual(expected: Int) {
        assertEquals(expected, this)
    }
    
    fun <T> T.shouldExist() {
        assertNotNull(this)
    }
}
