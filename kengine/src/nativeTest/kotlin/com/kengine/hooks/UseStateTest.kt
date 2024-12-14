package com.kengine.hooks

import com.kengine.hooks.state.useState
import com.kengine.log.Logging
import com.kengine.test.expectThat
import kotlin.test.Test

class UseStateTest : Logging {
    @Test
    fun `useState initializes and updates`() {
        val count = useState(0)

        expectThat(count.get()).isEqualTo(0)
        count.set(42)
        expectThat(count.get()).isEqualTo(42)
    }

    @Test
    fun `useState subscribe and unsubscribe`() {
        val count = useState(0)

        var isToggled = false
        val callback = { _: Int ->
            isToggled = !isToggled
        }
        expectThat(isToggled).isFalse()
        count.subscribe(callback)
        count.set(1)
        expectThat(isToggled).isTrue()
        count.unsubscribe(callback)
        count.set(2)
        expectThat(isToggled).isTrue()
    }
}