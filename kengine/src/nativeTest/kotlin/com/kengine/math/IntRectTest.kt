package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntRectTest {

    @Test
    fun `test default constructor`() {
        val rect = IntRect()
        assertEquals(0, rect.x)
        assertEquals(0, rect.y)
        assertEquals(0, rect.w)
        assertEquals(0, rect.h)
    }

    @Test
    fun `test parameterized constructor`() {
        val rect = IntRect(1, 2, 3, 4)
        assertEquals(1, rect.x)
        assertEquals(2, rect.y)
        assertEquals(3, rect.w)
        assertEquals(4, rect.h)
    }

    @Test
    fun `test area`() {
        val rect = IntRect(0, 0, 3, 4)
        assertEquals(12, rect.area())
    }

    @Test
    fun `test perimeter`() {
        val rect = IntRect(0, 0, 3, 4)
        assertEquals(14, rect.perimeter())
    }

    @Test
    fun `test translate`() {
        val rect = IntRect(1, 2, 3, 4)
        val translated = rect.translate(2, 3)
        assertEquals(3, translated.x)
        assertEquals(5, translated.y)
        assertEquals(3, translated.w)
        assertEquals(4, translated.h)
    }

    @Test
    fun `test translateAssign`() {
        val rect = IntRect(1, 2, 3, 4)
        rect.translateAssign(2, 3)
        assertEquals(3, rect.x)
        assertEquals(5, rect.y)
    }

    @Test
    fun `test scale`() {
        val rect = IntRect(0, 0, 3, 4)
        val scaled = rect.scale(2, 3)
        assertEquals(6, scaled.w)
        assertEquals(12, scaled.h)
    }

    @Test
    fun `test scaleAssign`() {
        val rect = IntRect(0, 0, 3, 4)
        rect.scaleAssign(2, 3)
        assertEquals(6, rect.w)
        assertEquals(12, rect.h)
    }

    @Test
    fun `test contains point`() {
        val rect = IntRect(1, 1, 3, 3)
        assertTrue(rect.contains(IntVec2(2, 2)))
        assertFalse(rect.contains(IntVec2(0, 0)))
    }

    @Test
    fun `test contains point - vec2`() {
        val rect = IntRect(1, 1, 3, 3)
        assertTrue(rect.contains(Vec2(2.0, 2.0)))
        assertFalse(rect.contains(Vec2(0.0, 0.0)))
    }

    @Test
    fun `test overlaps`() {
        val rect1 = IntRect(0, 0, 3, 3)
        val rect2 = IntRect(2, 2, 3, 3)
        val rect3 = IntRect(4, 4, 2, 2)
        assertTrue(rect1.overlaps(rect2))
        assertFalse(rect1.overlaps(rect3))
    }

    @Test
    fun `test overlaps - rect`() {
        val rect1 = IntRect(0, 0, 3, 3)
        val rect2 = Rect(2.0, 2.0, 3.0, 3.0)
        val rect3 = Rect(4.0, 4.0, 2.0, 2.0)
        assertTrue(rect1.overlaps(rect2))
        assertFalse(rect1.overlaps(rect3))
    }

    @Test
    fun `test intersection`() {
        val rect1 = IntRect(0, 0, 3, 3)
        val rect2 = IntRect(2, 2, 3, 3)
        val intersection = rect1.intersection(rect2)
        assertNotNull(intersection)
        assertEquals(IntRect(2, 2, 1, 1), intersection)
    }

    @Test
    fun `test no intersection`() {
        val rect1 = IntRect(0, 0, 2, 2)
        val rect2 = IntRect(3, 3, 2, 2)
        val intersection = rect1.intersection(rect2)
        assertNull(intersection)
    }

    @Test
    fun `test union`() {
        val rect1 = IntRect(0, 0, 3, 3)
        val rect2 = IntRect(2, 2, 3, 3)
        val union = rect1.union(rect2)
        assertEquals(IntRect(0, 0, 5, 5), union)
    }
}