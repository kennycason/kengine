package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals

class IntVec2Test {
    @Test
    fun intVec2SupportsMutableApi() {
        val vec = IntVec2()
        vec.set(2)
        vec += IntVec2(1, 2)
        vec *= 2

        assertEquals(IntVec2(6, 8), vec)
        assertEquals(10.0, vec.magnitude())
        assertEquals(Vec2(0.6, 0.8), vec.normalized())
        assertEquals(IntVec2(7, 9), vec + 1)
        assertEquals(IntVec2(5, 7), vec - 1)
        assertEquals(IntVec2(3, 4), vec / 2)
    }

    @Test
    fun intVec2SupportsComponentOperations() {
        val vec = IntVec2(8, 9)

        assertEquals(IntVec2(10, 12), vec + IntVec2(2, 3))
        assertEquals(IntVec2(6, 6), vec - IntVec2(2, 3))
        assertEquals(IntVec2(16, 27), vec * IntVec2(2, 3))
        assertEquals(IntVec2(4, 3), vec / IntVec2(2, 3))
    }
}
