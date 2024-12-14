package com.kengine.action

import com.kengine.time.getClockContext

data class IntervalAction(
    private val intervalMs: Long,
    private val onTick: () -> Unit
) : Action {
    private val clock = getClockContext()
    private var lastTickMs = clock.totalTimeMs
    private var isRunning = true

    override fun update(): Boolean {
        if (isRunning && clock.totalTimeMs - lastTickMs >= intervalMs) {
            lastTickMs = clock.totalTimeMs
            onTick()
        }
        return false
    }

    fun stop() {
        isRunning = false
    }
}