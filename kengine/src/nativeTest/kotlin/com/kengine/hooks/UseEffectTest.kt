package com.kengine.hooks

import com.kengine.hooks.effect.Effect
import com.kengine.hooks.state.State
import com.kengine.test.expectThat
import kotlin.test.Test

class UseEffectTest {
    @Test
    fun `effect cleanup unsubscribes callbacks`() {
        val count = State(0)
        var effectCalls = 0

        val effect = Effect(
            effect = { effectCalls++ },
            dependencies = listOf(count)
        )

        expectThat(effectCalls).isEqualTo(0)

        effect.execute() // Should run initially
        expectThat(effectCalls).isEqualTo(1)

        count.set(1) // Trigger dependency change
        expectThat(effectCalls).isEqualTo(2)

        effect.cleanup() // Remove callbacks
        count.set(2) // Should not trigger again
        expectThat(effectCalls).isEqualTo(2)
    }
}
