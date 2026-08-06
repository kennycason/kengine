package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntRectTest {
    @Test
    fun intRectSupportsGeometryQueries() {
        val rect = IntRect(1, 1, 3, 3)

        assertEquals(9, rect.area())
        assertEquals(12, rect.perimeter())
        assertTrue(rect.contains(Vec2(2.0, 2.0)))
        assertTrue(rect.contains(IntVec2(2, 2)))
        assertFalse(rect.contains(IntVec2(0, 0)))
        assertTrue(rect.overlaps(IntRect(3, 3, 2, 2)))
        assertTrue(rect.overlaps(Rect(3.0, 3.0, 2.0, 2.0)))
        assertFalse(rect.overlaps(IntRect(5, 5, 1, 1)))
    }

    @Test
    fun intRectSupportsTransformsAndSetOperations() {
        val rect = IntRect(0, 0, 3, 3)

        assertEquals(IntRect(2, 3, 3, 3), rect.translate(2, 3))
        assertEquals(IntRect(0, 0, 6, 9), rect.scale(2, 3))
        assertEquals(IntRect(2, 2, 1, 1), assertNotNull(rect.intersection(IntRect(2, 2, 3, 3))))
        assertNull(rect.intersection(IntRect(4, 4, 1, 1)))
        assertEquals(IntRect(0, 0, 5, 5), rect.union(IntRect(2, 2, 3, 3)))

        rect.translateAssign(1, 2)
        rect.scaleAssign(2, 3)
        assertEquals(IntRect(1, 2, 6, 9), rect)
    }
}
