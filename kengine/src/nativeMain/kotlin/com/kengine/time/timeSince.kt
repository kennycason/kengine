package com.kengine.time

/**
 * A convenience function for entities to track time since some timestamp.
 * e.g. if (keyboard.isSpacePressed() && timeSince(spaceLastPressedMs) > 200) { }
 */
fun timeSinceMs(timeMs: Long): Long {
    useClockContext {
        return getCurrentMilliseconds() - timeMs
    }
}

/**
 * A convenience function for entities to track time since some timestamp.
 * e.g. if (keyboard.isSpacePressed() && timeSince(spaceLastPressedSec) > 0.2) { }
 */
fun timeSinceSec(timeSec: Double): Double {
    useClockContext {
        return (getCurrentMilliseconds() / 1000.0) - timeSec
    }
}
