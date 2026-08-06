package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RectTest {
    @Test
    fun rectSupportsGeometryQueries() {
        val rect = Rect(1.0, 1.0, 3.0, 3.0)

        assertEquals(9.0, rect.area())
        assertEquals(12.0, rect.perimeter())
        assertTrue(rect.contains(Vec2(2.0, 2.0)))
        assertTrue(rect.contains(IntVec2(2, 2)))
        assertFalse(rect.contains(Vec2(0.0, 0.0)))
        assertTrue(rect.overlaps(Rect(3.0, 3.0, 2.0, 2.0)))
        assertTrue(rect.overlaps(IntRect(3, 3, 2, 2)))
        assertFalse(rect.overlaps(Rect(5.0, 5.0, 1.0, 1.0)))
    }

    @Test
    fun rectSupportsTransformsAndSetOperations() {
        val rect = Rect(0.0, 0.0, 3.0, 3.0)

        assertEquals(Rect(2.0, 3.0, 3.0, 3.0), rect.translate(2.0, 3.0))
        assertEquals(Rect(0.0, 0.0, 6.0, 9.0), rect.scale(2.0, 3.0))
        assertEquals(Rect(2.0, 2.0, 1.0, 1.0), assertNotNull(rect.intersection(Rect(2.0, 2.0, 3.0, 3.0))))
        assertNull(rect.intersection(Rect(4.0, 4.0, 1.0, 1.0)))
        assertEquals(Rect(0.0, 0.0, 5.0, 5.0), rect.union(Rect(2.0, 2.0, 3.0, 3.0)))

        rect.translateAssign(1.0, 2.0)
        rect.scaleAssign(2.0, 3.0)
        assertEquals(Rect(1.0, 2.0, 6.0, 9.0), rect)
    }
}
