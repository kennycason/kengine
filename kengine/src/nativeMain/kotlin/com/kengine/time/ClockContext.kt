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

    private var elapsedTimeSec = 0.0
    private var frameCount = 0
    private var logCounter = 0
    private var logInterval = 60 // Log every 60 frames (about once per second at 60 FPS)

    // moving average FPS
    private var avgFps = 60.0 // initial approximation
    private var frameWeight = 0.1 // Recent frames have 10% weight

    // frame timing
    private var targetFrameTime = 16.67 // Default ~60 FPS
    private var frameTimeErrorMs = 0.0 // Drift compensation

    /**
     * Sets the target FPS and calculates ideal frame time.
     */
    fun setFrameRate(fps: Int) {
        targetFrameTime = if (fps > 0) 1000.0 / fps else 0.0
        logger.info { "Target frame rate set: $fps FPS (${targetFrameTime.toInt()}ms)" }
    }

    /**
     * Updates time deltas and calculates FPS with a simple moving average.
     */
    fun update(deltaTimeMs: Long) {
        // update deltas
        this.deltaTimeMs = deltaTimeMs
        this.deltaTimeSec = deltaTimeMs / 1000.0
        totalTimeMs += deltaTimeMs
        totalTimeSec = totalTimeMs / 1000.0

        // calculate FPS
        frameCount++
        elapsedTimeSec += deltaTimeSec
        logCounter++

        if (elapsedTimeSec >= 1.0) {
            val currentFps = frameCount / elapsedTimeSec

            // simple moving average: weight recent frames more
            avgFps = avgFps * (1.0 - frameWeight) + currentFps * frameWeight
            fps = avgFps // store smoothed FPS

            if (logger.isDebugEnabled()) {
                logger.debug { "FPS: $currentFps, Avg: $fps" }
            }

            frameCount = 0
            elapsedTimeSec = 0.0
        }

        // Log detailed metrics periodically
        if (logCounter >= logInterval) {
            logCounter = 0

            // Calculate frame time statistics
            val frameTimeMs = getCurrentMilliseconds() - lastFrameTimeMs

            if (logger.isInfoEnabled()) {
                logger.info {
                    "Performance Metrics - " +
                    "FPS: ${fps.toInt()}, " +
                    "Frame Time: ${frameTimeMs}ms, " +
                    "Delta Time: ${deltaTimeMs}ms, " +
                    "Total Time: ${totalTimeMs}ms, " +
                    "Target Frame Time: ${targetFrameTime}ms"
                }
            }
        }
    }

    /**
     * Computes the time to delay for next frame, correcting drift.
     */
    fun calculateFrameDelay(): Double {
        if (targetFrameTime <= 0.0) return 0.0 // Uncapped FPS

        val currentTimeMs = getCurrentMilliseconds()
        val frameTimeMs = currentTimeMs - lastFrameTimeMs
        val adjustedDelay = targetFrameTime - frameTimeMs + frameTimeErrorMs

        // Update lastFrameTimeMs for next calculation
        lastFrameTimeMs = currentTimeMs

        // sub-millisecond drift correction
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
        elapsedTimeSec = 0.0
        frameCount = 0
        avgFps = 60.0
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
