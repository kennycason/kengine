package com.kengine.time

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

/**
 * These values are updated each update within GameLoop.
 * Tracks time and frame rates, optimized with circular buffer for moving average.
 */
@ConsistentCopyVisibility
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
    var deltaTimeSec: Double = 0.0,

    var lastFrameTimeMs: Long = getCurrentMilliseconds() // TODO, should this be zero'd?
) : Context(), Logging {


    // FPS Tracking
    var fps: Double = 0.0
        private set

    private var frameCount: Int = 0
    private var elapsedTimeSec: Double = 0.0

    // Circular buffer for moving average FPS
    private val fpsBuffer = DoubleArray(60) { 60.0 }
    private var fpsIndex = 0
    private var fpsSum = 60.0 * 60

    // Target frame rate & drift correction
    private var targetFrameTime = 0.0
    private var frameTimeErrorMs = 0.0 // Error for drift correction

    /**
     * Sets the target frame rate (FPS) and calculates frame time.
     */
    fun setFrameRate(fps: Int) {
        targetFrameTime = if (fps > 0) 1000.0 / fps else 0.0
        logger.info { "Target frame rate set: $fps FPS (Target Frame Time: ${targetFrameTime.toInt()}ms)." }
    }

    /**
     * Updates the clock, tracks FPS, and detects frame drops.
     */
    fun update() {
        val currentTimeMs = getCurrentMilliseconds()

        // Update time deltas
        deltaTimeMs = currentTimeMs - lastFrameTimeMs
        deltaTimeSec = deltaTimeMs / 1000.0
        totalTimeMs += deltaTimeMs
        totalTimeSec = totalTimeMs / 1000.0
        lastFrameTimeMs = currentTimeMs

        // FPS calculation
        frameCount++
        elapsedTimeSec += deltaTimeSec

        if (elapsedTimeSec >= 1.0) {
            fps = frameCount / elapsedTimeSec

            // Update moving average
            fpsSum -= fpsBuffer[fpsIndex]
            fpsBuffer[fpsIndex] = fps
            fpsSum += fps
            fpsIndex = (fpsIndex + 1) % fpsBuffer.size

            val avgFps = fpsSum / fpsBuffer.size

            if (logger.isDebugEnabled()) {
                logger.debug { "FPS: $fps (Avg: $avgFps)" }
            }

            frameCount = 0
            elapsedTimeSec = 0.0
        }

        // Frame drop detection (10% tolerance)
        if (targetFrameTime > 0 && deltaTimeMs > targetFrameTime * 1.1) {
            if (logger.isTraceEnabled()) {
                logger.trace { "Frame drop detected: Took ${deltaTimeMs}ms (Target: ${targetFrameTime.toInt()}ms)" }
            }
        }
    }


    /**
     * Calculates and returns the delay required to match target frame time.
     */
    fun calculateFrameDelay(): Double {
        if (targetFrameTime <= 0) return 0.0 // No frame rate limiting

        val frameTimeMs = getCurrentMilliseconds() - lastFrameTimeMs
        val adjustedDelay = targetFrameTime - frameTimeMs + frameTimeErrorMs

        // Adjust error for sub-millisecond drift correction
        frameTimeErrorMs = adjustedDelay % 1.0
        return adjustedDelay.coerceAtLeast(0.0)
    }


    override fun cleanup() {
        logger.info { "Cleaning up ClockContext" }
        totalTimeMs = 0L
        deltaTimeMs = 0L
        totalTimeSec = 0.0
        deltaTimeSec = 0.0
        fps = 0.0
        frameCount = 0
        elapsedTimeSec = 0.0
        fpsSum = 60.0 * 60
        fpsIndex = 0
        fpsBuffer.fill(60.0)
        frameTimeErrorMs = 0.0
        currentContext = null
    }

    companion object {
        private var currentContext: ClockContext? = null

        fun get(): ClockContext {
            return currentContext ?: ClockContext().also {
                currentContext = it
            }
        }
    }
}
