package com.pneumasoft.multitimer.robots

import android.content.Intent
import androidx.annotation.IdRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

abstract class BaseRobot {

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

    /**
     * Biztosítja, hogy az alkalmazás legyen az előtérben.
     * Ha nem, megpróbál visszalépni vagy újraindítani.
     */
    protected fun ensureAppIsForeground() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageName = context.packageName

        // Ellenőrizzük, hogy a mi csomagunk van-e elöl
        if (device.currentPackageName != packageName) {
            // 1. Próbálkozás: Visszalépés (pl. Notification Shade bezárása)
            device.pressBack()

            // Várunk kicsit
            if (!device.wait(Until.hasObject(By.pkg(packageName)), 2000)) {
                // 2. Próbálkozás: Explicit indítás
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                device.wait(Until.hasObject(By.pkg(packageName)), 5000)
            }
        }
    }

    // --- VISSZAÁLLÍTOTT RÉSZEK ---

    // Semantic assertions
    infix fun Int.shouldEqual(expected: Int) {
        assertEquals(expected, this)
    }

    fun <T> T.shouldExist() {
        assertNotNull(this)
    }
}
