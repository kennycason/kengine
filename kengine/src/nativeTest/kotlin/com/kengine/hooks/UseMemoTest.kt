package com.kengine.hooks

import com.kengine.hooks.state.State
import com.kengine.hooks.state.useState
import com.kengine.memo.useMemo
import com.kengine.test.expectThat
import kotlin.test.Test

class UseMemoTest {

    @Test
    fun `useMemo updates on dependency change`() {
        val count = useState(0)
        var computedValue = useMemo({ count.get() * 2 }, count)

        expectThat(computedValue.get()).isEqualTo(0)

        count.set(2) // trigger update
        computedValue = useMemo({ count.get() * 2 }, count) // retrieve updated value

        expectThat(computedValue.get()).isEqualTo(4) // ensure recomputation occurred
    }

    @Test
    fun `useMemo caches value until dependencies change`() {
        val a = State(1)
        val b = State(1)

        var calculationCount = 0

        val memoizedSum = useMemo({
            calculationCount++
            a.get() + b.get()
        }, a, b)

        // initially compute the sum
        expectThat(memoizedSum.get()).isEqualTo(2)
        expectThat(calculationCount).isEqualTo(1)

        // access again without changing dependencies, no recalculation
        expectThat(memoizedSum.get()).isEqualTo(2)
        expectThat(calculationCount).isEqualTo(1)

        // change one dependency and verify recalculation
        a.set(2)
        expectThat(memoizedSum.get()).isEqualTo(3)
        expectThat(calculationCount).isEqualTo(2)

        // change another dependency and verify recalculation
        b.set(2)
        expectThat(memoizedSum.get()).isEqualTo(4)
        expectThat(calculationCount).isEqualTo(3)
    }

    @Test
    fun `useMemo cleanup unsubscribes from dependencies`() {
        val a = State(5)
        val b = State(10)

        var calculationCount = 0

        val memoizedProduct = useMemo({
            calculationCount++
            a.get() * b.get()
        }, a, b)

        // access to trigger subscription
        expectThat(memoizedProduct.get()).isEqualTo(50)
        expectThat(calculationCount).isEqualTo(1)

        // cleanup and verify that changes no longer trigger recalculations
        memoizedProduct.cleanup()
        a.set(10)
        b.set(20)

        // value should remain unchanged as subscriptions are removed
        expectThat(memoizedProduct.get()).isEqualTo(50) // cached value
        expectThat(calculationCount).isEqualTo(1)
    }
}