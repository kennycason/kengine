package com.kengine.time

import com.kengine.context.Context

/**
 * These values are updated each update within GameLoop.
 * It supports both milliseconds(long) and seconds(double) for convenience
 */
data class ClockContext private constructor(
    /**
     * The total time the game has been running in milliseconds.
     */
    var totalTimeMs: Long = 0L,
    /**
     * The total time the game has been running in seconds as a fraction
     */
    var totalTimeSec: Double = 0.0,
    /**
     * The time in milliseconds since the last time update() was called within GameLoop
     */
    var deltaTimeMs: Long = 0L,
    /**
     * The time in seconds since the last time update() was called within GameLoop
     */
    var deltaTimeSec: Double = 0.0
) : Context() {

    override fun cleanup() {
        totalTimeMs = 0L
        deltaTimeMs = 0L
        totalTimeSec = 0.0
        deltaTimeSec = 0.0
    }

    companion object {
        private var currentContext: ClockContext? = null

        fun get(): ClockContext {
            if (currentContext == null) {
                currentContext = ClockContext()
            }
            return currentContext ?: throw IllegalStateException("Failed to create ClockContext")
        }
    }
}