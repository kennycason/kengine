package com.kengine.time

import com.kengine.action.ActionContext
import com.kengine.hooks.context.ContextRegistry
import com.kengine.log.Logging
import kotlin.test.Test

class UseIntervalTest : Logging {
    init {
        ContextRegistry.register(ActionContext.get())
        ContextRegistry.register(ClockContext.get())
    }

    @Test
    fun `useInterval triggers onTick at intervals`() {
        var tickCount = 0

        // use interval with a 100ms interval
        useInterval(100) {
            tickCount++
            logger.info { "tick count: $tickCount" }
        }
    }
}