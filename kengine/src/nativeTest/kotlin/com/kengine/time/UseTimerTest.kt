package com.kengine.time

import com.kengine.action.ActionContext
import com.kengine.hooks.context.ContextRegistry
import com.kengine.test.expectThat
import platform.posix.sleep
import kotlin.test.Ignore

class UseTimerTest {
    init {
        ContextRegistry.register(ActionContext.get())
        ContextRegistry.register(ClockContext.get())
    }

    @Ignore
    fun `useTimer triggers onComplete after delay`() {
        var timerTriggered = false

        useTimer(200) { timerTriggered = true }

        sleep(100u) // half delay, timer should not trigger
        expectThat(timerTriggered).isFalse()

        sleep(101u)  // full delay, timer should trigger
        expectThat(timerTriggered).isTrue()
    }
}