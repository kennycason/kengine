package com.kengine.context

import com.kengine.state.useState
import com.kengine.test.expectThat
import kotlin.test.Test


class ContextTest {
    @Test
    fun `context simple test EXPECT success`() {
        class SimpleContext : Context() {
            var count: Int = 1
        }

        val simpleContext = SimpleContext()
        ContextRegistry.register(simpleContext)
        useContext<SimpleContext> {
            expectThat(count).isEqualTo(1)
            count = 5
            expectThat(count).isEqualTo(5)
        }
    }

    @Test
    fun `context WITH state variables EXPECT success`() {
        class StatefulContext : Context() {
            val count = useState(0)
        }

        val statefulContext = StatefulContext()
        ContextRegistry.register(statefulContext)
        useContext<StatefulContext> {
            expectThat(count.get()).isEqualTo(0)
            count.set(42)
            expectThat(count.get()).isEqualTo(42)

            var countUpdated = false
            var countReceived = 0
            count.subscribe {
                countUpdated = true
                countReceived = it
            }
            count.set(64)
            expectThat(countUpdated).isTrue()
            expectThat(count.get()).isEqualTo(64)
            expectThat(countReceived).isEqualTo(64)
        }
    }

    @Test
    fun `context WITH private state variables and limited access EXPECT success`() {
        class StatefulContext : Context() {
            private val count = useState(0)
            fun getCount(): Int = count.get()
            fun incrementCount() {
                count.set(count.get() + 1)
            }
            fun subscribeCount(callback: (Int) -> Unit) {
                count.subscribe(callback)
            }
        }

        val statefulContext = StatefulContext()
        ContextRegistry.register(statefulContext)
        useContext<StatefulContext> {
            expectThat(getCount()).isEqualTo(0)
            incrementCount()
            expectThat(getCount()).isEqualTo(1)

            var countUpdated = false
            var countReceived = 0
            subscribeCount {
                countUpdated = true
                countReceived = it
            }
            incrementCount()
            expectThat(countUpdated).isTrue()
            expectThat(countReceived).isEqualTo(2)
        }
    }
}