package com.pneumasoft.multitimer.model

import java.util.UUID

data class TimerItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationSeconds: Int,
    var remainingSeconds: Int,
    var isRunning: Boolean = false,
    var completionTimestamp: Long? = null
)
