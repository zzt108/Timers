# GUIDE - Testing with Kotlin DSL and Page Object Pattern

**Version:** 1.0
**Date:** 2026-01-01
**Status:** Active
**Project:** Timers (MultiTimer Android App)

***

## Overview

This guide establishes testing standards using **expressive Kotlin DSLs** and **Page Object Pattern** instead of Cucumber/Gherkin. Tests are written in pure Kotlin with semantic naming that reads like natural language while maintaining refactoring safety and AI-interpretability.

***

## Core Principles

### 1. Readability Through DSLs

Tests should read like specifications without sacrificing type safety.

```kotlin
// ✅ GOOD - Reads like English, refactor-safe
@Test
fun `timer should ring alarm when elapsed`() = timerTest {
    createTimer(name = "Quick Task", duration = 5.seconds)
    startTimer()
    waitForCompletion()
    
    alarmScreen {
        shouldBeVisible()
        shouldPlaySound()
        tapDismiss()
    }
    
    mainScreen {
        timerWithName("Quick Task").shouldBeReset()
    }
}

// ❌ BAD - Imperative, unclear intent
@Test
fun test1() {
    val timer = Timer(300000)
    timer.start()
    Thread.sleep(300000)
    assertTrue(service.isAlarmRinging)
}
```


### 2. Page Object Pattern

Encapsulate screen interactions in reusable objects.

```kotlin
// Page Object for MainActivity
class MainScreenRobot {
    fun tapAddTimer() {
        onView(withId(R.id.addTimerButton)).perform(click())
    }
    
    fun timerWithName(name: String) = TimerItemRobot(name)
    
    fun shouldShowTimerCount(count: Int) {
        onView(withId(R.id.timerRecyclerView))
            .check(matches(hasChildCount(count)))
    }
}

// Page Object for individual timer items
class TimerItemRobot(private val timerName: String) {
    private val matcher = allOf(
        withId(R.id.timerName),
        withText(timerName)
    )
    
    fun shouldBeRunning() {
        onView(withId(R.id.playPauseButton))
            .check(matches(withContentDescription("Pause")))
    }
    
    fun tapPlayPause() {
        onView(matcher).perform(scrollTo(), click())
        onView(withId(R.id.playPauseButton)).perform(click())
    }
    
    fun shouldShowRemainingTime(time: String) {
        onView(withId(R.id.timerRemainingTime))
            .check(matches(withText(time)))
    }
}
```


### 3. Test Context DSL

Provide a fluent context for test scenarios.

```kotlin
// Base test DSL
class TimerTestContext(
    private val scenario: ActivityScenario<MainActivity>
) {
    val mainScreen = MainScreenRobot()
    val addTimerDialog = AddTimerDialogRobot()
    val alarmScreen = AlarmScreenRobot()
    val settingsScreen = SettingsScreenRobot()
    
    // Time manipulation helpers
    fun waitFor(duration: Duration) {
        onView(isRoot()).perform(waitForMillis(duration.inWholeMilliseconds))
    }
    
    fun fastForwardTime(duration: Duration) {
        // Advance system time for testing (requires test API)
        SystemClock.sleep(duration.inWholeMilliseconds)
    }
    
    // Assertions with semantic naming
    infix fun Int.seconds.shouldEqual(expected: Duration) {
        assertEquals(expected.inWholeSeconds, this.seconds.inWholeSeconds)
    }
}

// DSL entry point
fun timerTest(block: TimerTestContext.() -> Unit) {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        TimerTestContext(scenario).apply(block)
    }
}
```


***

## Test Structure Standards

### Naming Convention

```kotlin
// Format: `[subject] should [behavior] when [condition]`
@Test
fun `timer should pause when play button tapped while running`()

@Test
fun `alarm notification should appear on lock screen when timer completes`()

@Test
fun `multiple timers should run independently without interference`()
```


### Organize by Feature Module

```
app/src/androidTest/kotlin/com/pneumasoft/multitimer/
├── tests/
│   ├── TimerCreationTests.kt
│   ├── TimerExecutionTests.kt
│   ├── AlarmNotificationTests.kt
│   ├── BackgroundServiceTests.kt
│   └── SettingsIntegrationTests.kt
├── robots/              # Page Objects
│   ├── MainScreenRobot.kt
│   ├── AddTimerDialogRobot.kt
│   ├── AlarmScreenRobot.kt
│   └── SettingsScreenRobot.kt
├── dsl/                 # Test DSL helpers
│   ├── TimerTestContext.kt
│   ├── TimerMatchers.kt
│   └── TimeHelpers.kt
└── fixtures/            # Test data
    └── TimerFixtures.kt
```


***

## Implementation Examples

### Example 1: Timer Creation Flow

```kotlin
// File: app/src/androidTest/kotlin/tests/TimerCreationTests.kt

class TimerCreationTests {
    
    @Test
    fun `should create timer with custom name and duration`() = timerTest {
        mainScreen {
            tapAddTimer()
        }
        
        addTimerDialog {
            enterName("Morning Routine")
            setHours(0)
            setMinutes(25)
            tapAdd()
        }
        
        mainScreen {
            shouldShowTimerCount(1)
            timerWithName("Morning Routine") {
                shouldShowRemainingTime("25:00")
                shouldNotBeRunning()
            }
        }
    }
    
    @Test
    fun `should create timer with default name when name is empty`() = timerTest {
        mainScreen.tapAddTimer()
        
        addTimerDialog {
            // Leave name empty
            setMinutes(10)
            tapAdd()
        }
        
        mainScreen {
            timerWithName("Timer 1").shouldExist()
        }
    }
    
    @Test
    fun `should prevent creating timer with zero duration`() = timerTest {
        mainScreen.tapAddTimer()
        
        addTimerDialog {
            setHours(0)
            setMinutes(0)
            tapAdd()
            
            shouldShowError("Duration must be at least 1 minute")
            shouldStillBeOpen()
        }
    }
}
```


### Example 2: Timer Execution with Background Service

```kotlin
// File: app/src/androidTest/kotlin/tests/TimerExecutionTests.kt

@RunWith(AndroidJUnit4::class)
class TimerExecutionTests {
    
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = 
        GrantPermissionRule.grant(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.SCHEDULE_EXACT_ALARM
        )
    
    @Test
    fun `timer should continue running when app goes to background`() = timerTest {
        // Create and start timer
        createTimer(name = "Background Test", duration = 2.minutes)
        mainScreen.timerWithName("Background Test").tapPlayPause()
        
        // Send app to background
        sendAppToBackground()
        waitFor(30.seconds)
        
        // Bring app back
        bringAppToForeground()
        
        mainScreen.timerWithName("Background Test") {
            shouldBeRunning()
            shouldShowRemainingTime(approximately = "1:30")
        }
    }
    
    @Test
    fun `absolute time calculation should prevent drift over long duration`() = timerTest {
        createTimer(name = "Precision Test", duration = 1.hours)
        
        val startTime = System.currentTimeMillis()
        mainScreen.timerWithName("Precision Test").tapPlayPause()
        
        // Simulate time passage without CPU sleep (use IdlingResource)
        waitFor(30.minutes)
        
        mainScreen.timerWithName("Precision Test") {
            val expectedRemaining = (startTime + 1.hours.inWholeMilliseconds) - 
                                   System.currentTimeMillis()
            shouldShowRemainingTime(expectedRemaining.toMinutesSeconds())
        }
    }
}
```


### Example 3: Alarm Notification and Lock Screen

```kotlin
// File: app/src/androidTest/kotlin/tests/AlarmNotificationTests.kt

class AlarmNotificationTests {
    
    @Test
    fun `alarm should show full-screen notification when timer completes`() = timerTest {
        createTimer(name = "Alarm Test", duration = 5.seconds)
        mainScreen.timerWithName("Alarm Test").tapPlayPause()
        
        waitForCompletion()
        
        alarmScreen {
            shouldBeVisible()
            shouldShowTimerName("Alarm Test")
            shouldPlaySound()
        }
    }
    
    @Test
    fun `alarm should loop sound until dismissed`() = timerTest {
        createTimer(duration = 3.seconds)
        startAllTimers()
        
        waitForCompletion()
        
        alarmScreen {
            verifySoundLooping()
            waitFor(5.seconds) // Sound should still be playing
            shouldStillPlaySound()
            
            tapDismiss()
            shouldNotPlaySound()
        }
    }
    
    @Test
    fun `snooze should restart timer with configured duration`() = timerTest {
        settingsScreen {
            open()
            setSnoozeDuration(ShortSnooze, 30.seconds)
        }
        
        createTimer(name = "Snooze Test", duration = 2.seconds)
        startTimer("Snooze Test")
        waitForCompletion()
        
        alarmScreen {
            tapSnooze(ShortSnooze)
        }
        
        mainScreen {
            timerWithName("Snooze Test") {
                shouldBeRunning()
                shouldShowRemainingTime("0:30")
            }
        }
    }
}
```


### Example 4: Edge Cases and Bug Regression

```kotlin
// File: app/src/androidTest/kotlin/tests/BugRegressionTests.kt

class BugRegressionTests {
    
    @Test
    fun `issue 18 - one timer stopping should not affect others`() = timerTest {
        // Regression test for GitHub Issue #18
        createTimer(name = "Timer A", duration = 1.minutes)
        createTimer(name = "Timer B", duration = 2.minutes)
        createTimer(name = "Timer C", duration = 3.minutes)
        
        startAllTimers()
        
        waitFor(10.seconds)
        mainScreen.timerWithName("Timer A").tapPlayPause() // Pause Timer A
        
        waitFor(5.seconds)
        
        mainScreen {
            timerWithName("Timer A").shouldNotBeRunning()
            timerWithName("Timer B").shouldBeRunning()
            timerWithName("Timer C").shouldBeRunning()
        }
    }
    
    @Test
    fun `issue 16 - timer should not drift over 1 hour duration`() = timerTest {
        // Regression test for GitHub Issue #16
        // Timer was losing ~5 minutes per hour
        
        createTimer(name = "Precision", duration = 1.hours)
        val expectedEndTime = System.currentTimeMillis() + 1.hours.inWholeMilliseconds
        
        startTimer("Precision")
        
        // Fast-forward with IdlingResource
        advanceTimeBy(30.minutes)
        
        mainScreen.timerWithName("Precision") {
            val actualRemaining = getRemainingMillis()
            val expectedRemaining = expectedEndTime - System.currentTimeMillis()
            
            // Should be within 1 second tolerance
            actualRemaining.shouldBeCloseTo(expectedRemaining, tolerance = 1000)
        }
    }
}
```


***

## Robot Pattern Implementation Guide

### Base Robot Class

```kotlin
// File: app/src/androidTest/kotlin/robots/BaseRobot.kt

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
    
    // Semantic assertions
    infix fun Int.shouldEqual(expected: Int) {
        assertEquals(expected, this)
    }
    
    fun <T> T.shouldExist() {
        assertNotNull(this)
    }
}
```


### Main Screen Robot

```kotlin
// File: app/src/androidTest/kotlin/robots/MainScreenRobot.kt

class MainScreenRobot : BaseRobot() {
    
    fun tapAddTimer() {
        tapView(R.id.addTimerButton)
    }
    
    fun tapSettings() {
        openActionBarOverflowOrOptionsMenu(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        onView(withText("Settings")).perform(click())
    }
    
    fun timerWithName(name: String) = TimerItemRobot(name)
    
    fun shouldShowTimerCount(count: Int) {
        onView(withId(R.id.timerRecyclerView))
            .check(matches(hasChildCount(count)))
    }
    
    fun shouldShowEmptyState() {
        verifyVisible(R.id.emptyStateTextView)
    }
    
    // Fluent builder for inline assertions
    inline fun verify(block: MainScreenRobot.() -> Unit) {
        this.apply(block)
    }
}
```


### Timer Item Robot

```kotlin
// File: app/src/androidTest/kotlin/robots/TimerItemRobot.kt

class TimerItemRobot(private val timerName: String) : BaseRobot() {
    
    private val timerItemMatcher = withRecyclerView(R.id.timerRecyclerView)
        .atPositionWithText(timerName)
    
    fun tapPlayPause() {
        onView(timerItemMatcher)
            .perform(scrollTo())
        onView(allOf(
            withId(R.id.playPauseButton),
            hasSibling(withText(timerName))
        )).perform(click())
    }
    
    fun tapEdit() {
        onView(timerItemMatcher)
            .perform(longClick())
        onView(withText("Edit")).perform(click())
    }
    
    fun tapDelete() {
        onView(timerItemMatcher)
            .perform(longClick())
        onView(withText("Delete")).perform(click())
    }
    
    fun shouldBeRunning() {
        onView(allOf(
            withId(R.id.playPauseButton),
            hasSibling(withText(timerName))
        )).check(matches(withContentDescription("Pause")))
    }
    
    fun shouldNotBeRunning() {
        onView(allOf(
            withId(R.id.playPauseButton),
            hasSibling(withText(timerName))
        )).check(matches(withContentDescription("Play")))
    }
    
    fun shouldShowRemainingTime(time: String) {
        onView(allOf(
            withId(R.id.timerRemainingTime),
            hasSibling(withText(timerName))
        )).check(matches(withText(time)))
    }
    
    fun getRemainingMillis(): Long {
        var remaining = 0L
        onView(allOf(
            withId(R.id.timerRemainingTime),
            hasSibling(withText(timerName))
        )).check { view, _ ->
            val textView = view as TextView
            remaining = parseTimeToMillis(textView.text.toString())
        }
        return remaining
    }
}
```


### Alarm Screen Robot

```kotlin
// File: app/src/androidTest/kotlin/robots/AlarmScreenRobot.kt

class AlarmScreenRobot : BaseRobot() {
    
    fun shouldBeVisible() {
        onView(withId(R.id.alarmRootLayout))
            .check(matches(isDisplayed()))
    }
    
    fun shouldShowTimerName(name: String) {
        onView(withId(R.id.alarmTimerName))
            .check(matches(withText(name)))
    }
    
    fun tapDismiss() {
        tapView(R.id.dismissButton)
    }
    
    fun tapSnooze(duration: SnoozeDuration) {
        when (duration) {
            ShortSnooze -> tapView(R.id.snoozeShortButton)
            LongSnooze -> tapView(R.id.snoozeLongButton)
        }
    }
    
    fun shouldPlaySound() {
        // Verify sound manager is playing
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val soundManager = (context.applicationContext as TimerApplication)
            .getSoundManager()
        
        assertTrue("Alarm sound should be playing", soundManager.isPlaying)
    }
    
    fun verifySoundLooping() {
        // Check that sound is set to loop mode
        val soundManager = getSoundManagerInstance()
        assertTrue("Alarm should be looping", soundManager.isLooping)
    }
}
```


***

## DSL Helpers and Extensions

### Time Extensions

```kotlin
// File: app/src/androidTest/kotlin/dsl/TimeHelpers.kt

val Int.seconds: Duration get() = Duration.ofSeconds(this.toLong())
val Int.minutes: Duration get() = Duration.ofMinutes(this.toLong())
val Int.hours: Duration get() = Duration.ofHours(this.toLong())

fun Duration.toMinutesSeconds(): String {
    val minutes = this.toMinutes()
    val seconds = this.seconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

fun parseTimeToMillis(timeString: String): Long {
    val parts = timeString.split(":")
    val minutes = parts[^0].toLong()
    val seconds = parts[^1].toLong()
    return (minutes * 60 + seconds) * 1000
}

fun Long.shouldBeCloseTo(expected: Long, tolerance: Long) {
    val diff = abs(this - expected)
    assertTrue(
        "Expected $this to be within $tolerance of $expected, but diff was $diff",
        diff <= tolerance
    )
}
```


### Custom Matchers

```kotlin
// File: app/src/androidTest/kotlin/dsl/TimerMatchers.kt

fun hasChildCount(expectedCount: Int): Matcher<View> {
    return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("RecyclerView with item count: $expectedCount")
        }
        
        override fun matchesSafely(view: RecyclerView): Boolean {
            return view.adapter?.itemCount == expectedCount
        }
    }
}

fun withRecyclerView(@IdRes recyclerViewId: Int) = 
    RecyclerViewMatcher(recyclerViewId)

class RecyclerViewMatcher(private val recyclerViewId: Int) {
    fun atPositionWithText(text: String): Matcher<View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("RecyclerView item with text: $text")
            }
            
            override fun matchesSafely(view: View): Boolean {
                val recyclerView = view.rootView.findViewById<RecyclerView>(recyclerViewId)
                for (i in 0 until recyclerView.adapter!!.itemCount) {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
                    val itemView = viewHolder?.itemView
                    val nameView = itemView?.findViewById<TextView>(R.id.timerName)
                    if (nameView?.text == text) return true
                }
                return false
            }
        }
    }
}
```


### Idling Resources for Async Operations

```kotlin
// File: app/src/androidTest/kotlin/dsl/TimerIdlingResource.kt

class TimerIdlingResource(
    private val viewModel: TimerViewModel
) : IdlingResource {
    
    private var callback: IdlingResource.ResourceCallback? = null
    
    override fun getName() = "TimerIdlingResource"
    
    override fun isIdleNow(): Boolean {
        val idle = viewModel.timers.value.none { it.isRunning }
        if (idle) callback?.onTransitionToIdle()
        return idle
    }
    
    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }
}

// Usage in tests
fun TimerTestContext.waitForAllTimersComplete() {
    val idlingResource = TimerIdlingResource(getViewModel())
    IdlingRegistry.getInstance().register(idlingResource)
    
    // Espresso will wait until idle
    onView(isRoot()).check(matches(isDisplayed()))
    
    IdlingRegistry.getInstance().unregister(idlingResource)
}
```


***

## Test Fixtures

### Predefined Test Data

```kotlin
// File: app/src/androidTest/kotlin/fixtures/TimerFixtures.kt

object TimerFixtures {
    
    fun quickTimer() = TimerItem(
        id = UUID.randomUUID().toString(),
        name = "Quick Task",
        durationSeconds = 300, // 5 minutes
        remainingSeconds = 300,
        isRunning = false,
        absoluteEndTimeMillis = null
    )
    
    fun longTimer() = TimerItem(
        id = UUID.randomUUID().toString(),
        name = "Long Task",
        durationSeconds = 3600, // 1 hour
        remainingSeconds = 3600,
        isRunning = false,
        absoluteEndTimeMillis = null
    )
    
    fun runningTimer(remainingSeconds: Int = 300) = TimerItem(
        id = UUID.randomUUID().toString(),
        name = "Active Timer",
        durationSeconds = 600,
        remainingSeconds = remainingSeconds,
        isRunning = true,
        absoluteEndTimeMillis = System.currentTimeMillis() + (remainingSeconds * 1000L)
    )
}

// Extension for test context
fun TimerTestContext.createTimerFromFixture(fixture: TimerItem) {
    // Directly inject into repository for faster setup
    val repository = getRepository()
    repository.addTimer(fixture)
}
```


***

## Integration with ViewModel Testing

### Unit Test Example with DSL

```kotlin
// File: app/src/test/kotlin/viewmodel/TimerViewModelTests.kt

@RunWith(MockitoJUnitRunner::class)
class TimerViewModelTests {
    
    @Mock
    private lateinit var repository: TimerRepository
    
    private lateinit var viewModel: TimerViewModel
    
    @Before
    fun setup() {
        viewModel = TimerViewModel(repository)
    }
    
    @Test
    fun `starting timer should calculate absolute end time`() = runTest {
        // Arrange
        val timer = TimerFixtures.quickTimer()
        `when`(repository.loadTimers()).thenReturn(listOf(timer))
        viewModel.loadTimers()
        
        val beforeStart = System.currentTimeMillis()
        
        // Act
        viewModel.startTimer(timer.id)
        
        // Assert
        val updatedTimer = viewModel.timers.value.first()
        updatedTimer.apply {
            isRunning shouldBe true
            absoluteEndTimeMillis.shouldNotBeNull()
            absoluteEndTimeMillis!! shouldBeGreaterThan beforeStart
            absoluteEndTimeMillis!! shouldBeLessThan (beforeStart + 400_000)
        }
    }
    
    @Test
    fun `multiple timers should maintain independent state`() = runTest {
        // Arrange
        val timer1 = TimerFixtures.quickTimer().copy(id = "1")
        val timer2 = TimerFixtures.quickTimer().copy(id = "2")
        `when`(repository.loadTimers()).thenReturn(listOf(timer1, timer2))
        viewModel.loadTimers()
        
        // Act
        viewModel.startTimer("1")
        advanceTimeBy(1000)
        viewModel.pauseTimer("1")
        
        // Assert
        viewModel.timers.value.apply {
            first { it.id == "1" }.isRunning shouldBe false
            first { it.id == "2" }.isRunning shouldBe false
        }
    }
}

// Semantic assertion extensions
infix fun <T> T.shouldBe(expected: T) = assertEquals(expected, this)
infix fun <T : Comparable<T>> T.shouldBeGreaterThan(expected: T) = 
    assertTrue("$this should be > $expected", this > expected)
```


***

## AI Usage Instructions

### For AI Agents: Test Generation Workflow

1. **Identify the Feature** from issue/plan description
2. **Extract Key Scenarios**:

```
Example from Issue #21:
- Timer completes → Alarm appears
- Alarm loops → Until dismissed
- Dismiss button → Stops alarm
```

3. **Generate Test Structure**:

```kotlin
@Test
fun `[semantic name from scenario]`() = timerTest {
    // Arrange (setup)
    createTimer(...)
    
    // Act (user action)
    mainScreen.timerWithName(...).tapPlayPause()
    
    // Assert (verification)
    alarmScreen {
        shouldBeVisible()
        tapDismiss()
    }
}
```

4. **Use Existing Robots** - Check `robots/` directory before creating new methods
5. **Follow Naming Patterns**:
    - Methods: `tap*`, `enter*`, `set*`, `shouldBe*`, `shouldShow*`
    - No `get*` unless returning value for assertion
    - Use infix functions for readability: `value shouldBe expected`

### AI Code Generation Template

```kotlin
/**
 * Test for: [GitHub Issue #XX or Feature Description]
 * 
 * Scenario:
 * - [Step 1 from requirements]
 * - [Step 2 from requirements]
 * - [Expected outcome]
 */
@Test
fun `[semantic name derived from scenario]`() = timerTest {
    // Arrange: Set up initial state
    [create necessary test data using fixtures or DSL]
    
    // Act: Perform user actions
    [use Robot methods to interact with UI]
    
    // Assert: Verify expected behavior
    [use shouldBe*, shouldShow*, verify blocks]
}
```


***

## Anti-Patterns to Avoid

### ❌ Don't: Direct View Interactions in Tests

```kotlin
// BAD
@Test
fun test1() {
    onView(withId(R.id.addTimerButton)).perform(click())
    onView(withId(R.id.timerName)).perform(typeText("Test"))
}
```


### ✅ Do: Use Robots

```kotlin
// GOOD
@Test
fun `should create timer`() = timerTest {
    mainScreen.tapAddTimer()
    addTimerDialog.enterName("Test")
}
```


### ❌ Don't: Hard-Coded Sleeps

```kotlin
// BAD
Thread.sleep(5000)
```


### ✅ Do: Use IdlingResources or waitFor DSL

```kotlin
// GOOD
waitForCompletion()
// or
waitFor(5.seconds)
```


### ❌ Don't: Generic Test Names

```kotlin
@Test
fun test1() { }
```


### ✅ Do: Semantic Backtick Names

```kotlin
@Test
fun `timer should show notification when elapsed`() { }
```


***

## CI/CD Integration

### Gradle Test Execution

```gradle
// app/build.gradle.kts

android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        
        animationsDisabled = true
        
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

dependencies {
    androidTestUtil("androidx.test:orchestrator:1.4.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("org.mockito:mockito-android:5.8.0")
}
```


### Run Tests

```bash
# All instrumented tests
./gradlew connectedAndroidTest

# Specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pneumasoft.multitimer.tests.TimerExecutionTests

# With coverage
./gradlew connectedAndroidTest jacocoTestReport
```


***

## Version History

| Version | Date | Changes |
| :-- | :-- | :-- |
| 1.0 | 2026-01-01 | Initial testing guide with DSL and Page Object pattern |
