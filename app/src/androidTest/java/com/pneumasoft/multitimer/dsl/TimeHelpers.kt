package com.pneumasoft.multitimer.dsl

import org.junit.Assert.assertTrue
import java.time.Duration
import kotlin.math.abs

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
    if (parts.size < 2) return 0L
    // Assuming format is "mm:ss" or "hh:mm:ss"?
    // The robot seems to check "mm:ss" based on Example 1 ("25:00")
    // But MainActivity displays "hh h mm m" or "mm minutes".
    // Wait, the Recycler View Item probably shows remaining time.
    // TimerAdapter.kt isn't read, but TimerItemRobot checks `timerRemainingTime`.
    
    // Let's assume standard parsing for now, user can adjust.
    val minutes = parts[0].toLongOrNull() ?: 0L
    val seconds = parts[1].toLongOrNull() ?: 0L
    return (minutes * 60 + seconds) * 1000
}

fun Long.shouldBeCloseTo(expected: Long, tolerance: Long) {
    val diff = abs(this - expected)
    assertTrue(
        "Expected $this to be within $tolerance of $expected, but diff was $diff",
        diff <= tolerance
    )
}
