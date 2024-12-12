package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RectTest {

    @Test
    fun `test default constructor`() {
        val rect = Rect()
        assertEquals(0.0, rect.x)
        assertEquals(0.0, rect.y)
        assertEquals(0.0, rect.w)
        assertEquals(0.0, rect.h)
    }

    @Test
    fun `test parameterized constructor`() {
        val rect = Rect(1.0, 2.0, 3.0, 4.0)
        assertEquals(1.0, rect.x)
        assertEquals(2.0, rect.y)
        assertEquals(3.0, rect.w)
        assertEquals(4.0, rect.h)
    }

    @Test
    fun `test area`() {
        val rect = Rect(0.0, 0.0, 3.0, 4.0)
        assertEquals(12.0, rect.area())
    }

    @Test
    fun `test perimeter`() {
        val rect = Rect(0.0, 0.0, 3.0, 4.0)
        assertEquals(14.0, rect.perimeter())
    }

    @Test
    fun `test translate`() {
        val rect = Rect(1.0, 2.0, 3.0, 4.0)
        val translated = rect.translate(2.0, 3.0)
        assertEquals(3.0, translated.x)
        assertEquals(5.0, translated.y)
        assertEquals(3.0, translated.w)
        assertEquals(4.0, translated.h)
    }

    @Test
    fun `test translateAssign`() {
        val rect = Rect(1.0, 2.0, 3.0, 4.0)
        rect.translateAssign(2.0, 3.0)
        assertEquals(3.0, rect.x)
        assertEquals(5.0, rect.y)
    }

    @Test
    fun `test scale`() {
        val rect = Rect(0.0, 0.0, 3.0, 4.0)
        val scaled = rect.scale(2.0, 3.0)
        assertEquals(6.0, scaled.w)
        assertEquals(12.0, scaled.h)
    }

    @Test
    fun `test scaleAssign`() {
        val rect = Rect(0.0, 0.0, 3.0, 4.0)
        rect.scaleAssign(2.0, 3.0)
        assertEquals(6.0, rect.w)
        assertEquals(12.0, rect.h)
    }

    @Test
    fun `test contains point`() {
        val rect = Rect(1.0, 1.0, 3.0, 3.0)
        assertTrue(rect.contains(Vec2(2.0, 2.0)))
        assertFalse(rect.contains(Vec2(0.0, 0.0)))
    }

    @Test
    fun `test contains point - intvec2`() {
        val rect = Rect(1.0, 1.0, 3.0, 3.0)
        assertTrue(rect.contains(IntVec2(2, 2)))
        assertFalse(rect.contains(IntVec2(0, 0)))
    }

    @Test
    fun `test overlaps`() {
        val rect1 = Rect(0.0, 0.0, 3.0, 3.0)
        val rect2 = Rect(2.0, 2.0, 3.0, 3.0)
        val rect3 = Rect(4.0, 4.0, 2.0, 2.0)
        assertTrue(rect1.overlaps(rect2))
        assertFalse(rect1.overlaps(rect3))
    }

    @Test
    fun `test overlaps - intrect2`() {
        val rect1 = Rect(0.0, 0.0, 3.0, 3.0)
        val rect2 = IntRect(2, 2, 3, 3)
        val rect3 = IntRect(4, 4, 2, 2)
        assertTrue(rect1.overlaps(rect2))
        assertFalse(rect1.overlaps(rect3))
    }

    @Test
    fun `test intersection`() {
        val rect1 = Rect(0.0, 0.0, 3.0, 3.0)
        val rect2 = Rect(2.0, 2.0, 3.0, 3.0)
        val intersection = rect1.intersection(rect2)
        assertNotNull(intersection)
        assertEquals(Rect(2.0, 2.0, 1.0, 1.0), intersection)
    }

    @Test
    fun `test no intersection`() {
        val rect1 = Rect(0.0, 0.0, 2.0, 2.0)
        val rect2 = Rect(3.0, 3.0, 2.0, 2.0)
        val intersection = rect1.intersection(rect2)
        assertNull(intersection)
    }

    @Test
    fun `test union`() {
        val rect1 = Rect(0.0, 0.0, 3.0, 3.0)
        val rect2 = Rect(2.0, 2.0, 3.0, 3.0)
        val union = rect1.union(rect2)
        assertEquals(Rect(0.0, 0.0, 5.0, 5.0), union)
    }
}