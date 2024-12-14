package com.kengine.effect

import com.kengine.state.State
import com.kengine.test.expectThat
import kotlin.test.Test

class EffectTest {
    @Test
    fun `effect cleanup unsubscribes callbacks`() {
        val count = State(0)
        var effectCalls = 0

        val effect = Effect(
            effect = { effectCalls++ },
            dependencies = listOf(count)
        )

        expectThat(effectCalls).isEqualTo(0)

        effect.execute()
        expectThat(effectCalls).isEqualTo(1)

        count.set(2)
        expectThat(effectCalls).isEqualTo(2) // triggered state change

        effect.cleanup() // unsubscribe all callbacks

        count.set(2)
        expectThat(effectCalls).isEqualTo(2) // no effect triggered after cleanup
    }
}