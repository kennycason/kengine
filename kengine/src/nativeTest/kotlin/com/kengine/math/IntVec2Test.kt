package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals

class IntVec2Test {

    @Test
    fun `test default constructor`() {
        val vec = IntVec2()
        assertEquals(0, vec.x)
        assertEquals(0, vec.y)
    }

    @Test
    fun `test parameterized constructor`() {
        val vec = IntVec2(3, 4)
        assertEquals(3, vec.x)
        assertEquals(4, vec.y)
    }

    @Test
    fun `test set with x and y`() {
        val vec = IntVec2()
        vec.set(2, 3)
        assertEquals(2, vec.x)
        assertEquals(3, vec.y)
    }

    @Test
    fun `test set with single value`() {
        val vec = IntVec2()
        vec.set(5)
        assertEquals(5, vec.x)
        assertEquals(5, vec.y)
    }

    @Test
    fun `test set with another IntVec2`() {
        val vec1 = IntVec2(1, 2)
        val vec2 = IntVec2()
        vec2.set(vec1)
        assertEquals(1, vec2.x)
        assertEquals(2, vec2.y)
    }

    @Test
    fun `test magnitude`() {
        val vec = IntVec2(3, 4)
        assertEquals(5.0, vec.magnitude())
    }

    @Test
    fun `test normalized`() {
        val vec = IntVec2(3, 4)
        val normalized = vec.normalized()
        assertEquals(0.6, normalized.x)
        assertEquals(0.8, normalized.y)
    }

    @Test
    fun `test normalized for zero vector`() {
        val vec = IntVec2()
        val normalized = vec.normalized()
        assertEquals(0.0, normalized.x)
        assertEquals(0.0, normalized.y)
    }

    @Test
    fun `test plus operator`() {
        val vec1 = IntVec2(1, 2)
        val vec2 = IntVec2(3, 4)
        val result = vec1 + vec2
        assertEquals(4, result.x)
        assertEquals(6, result.y)
    }

    @Test
    fun `test minus operator`() {
        val vec1 = IntVec2(5, 6)
        val vec2 = IntVec2(3, 4)
        val result = vec1 - vec2
        assertEquals(2, result.x)
        assertEquals(2, result.y)
    }

    @Test
    fun `test times operator`() {
        val vec1 = IntVec2(2, 3)
        val vec2 = IntVec2(4, 5)
        val result = vec1 * vec2
        assertEquals(8, result.x)
        assertEquals(15, result.y)
    }

    @Test
    fun `test div operator`() {
        val vec1 = IntVec2(8, 9)
        val vec2 = IntVec2(2, 3)
        val result = vec1 / vec2
        assertEquals(4, result.x)
        assertEquals(3, result.y)
    }

    @Test
    fun `test scalar plus operator`() {
        val vec = IntVec2(1, 2)
        val result = vec + 5
        assertEquals(6, result.x)
        assertEquals(7, result.y)
    }

    @Test
    fun `test scalar minus operator`() {
        val vec = IntVec2(10, 20)
        val result = vec - 5
        assertEquals(5, result.x)
        assertEquals(15, result.y)
    }

    @Test
    fun `test scalar times operator`() {
        val vec = IntVec2(2, 3)
        val result = vec * 4
        assertEquals(8, result.x)
        assertEquals(12, result.y)
    }

    @Test
    fun `test scalar div operator`() {
        val vec = IntVec2(8, 16)
        val result = vec / 2
        assertEquals(4, result.x)
        assertEquals(8, result.y)
    }

    @Test
    fun `test plusAssign operator`() {
        val vec = IntVec2(1, 2)
        vec += IntVec2(3, 4)
        assertEquals(4, vec.x)
        assertEquals(6, vec.y)
    }

    @Test
    fun `test minusAssign operator`() {
        val vec = IntVec2(5, 6)
        vec -= IntVec2(3, 4)
        assertEquals(2, vec.x)
        assertEquals(2, vec.y)
    }

    @Test
    fun `test timesAssign operator`() {
        val vec = IntVec2(2, 3)
        vec *= IntVec2(4, 5)
        assertEquals(8, vec.x)
        assertEquals(15, vec.y)
    }

    @Test
    fun `test divAssign operator`() {
        val vec = IntVec2(8, 9)
        vec /= IntVec2(2, 3)
        assertEquals(4, vec.x)
        assertEquals(3, vec.y)
    }
}