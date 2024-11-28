package com.kengine.time

import com.kengine.context.useContext

/**
 * A convenience function for entities to track time since some timestamp.
 * e.g. if (keyboard.isSpacePressed() && timeSince(spaceLastPressedMs) > 200) { }
 */
fun timeSinceMs(timeMs: Long): Long {
    useClockContext {
        return totalTimeMs - timeMs
    }
}

/**
 * A convenience function for entities to track time since some timestamp.
 * e.g. if (keyboard.isSpacePressed() && timeSince(spaceLastPressedSec) > 0.2) { }
 */
fun timeSinceSec(timeSec: Double): Double {
    useClockContext {
        return totalTimeSec - timeSec
    }
}
