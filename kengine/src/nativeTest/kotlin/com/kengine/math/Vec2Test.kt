package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals

class Vec2Test {

    @Test
    fun `test default constructor`() {
        val vec = Vec2()
        assertEquals(0.0, vec.x)
        assertEquals(0.0, vec.y)
    }

    @Test
    fun `test parameterized constructor`() {
        val vec = Vec2(3.0, 4.0)
        assertEquals(3.0, vec.x)
        assertEquals(4.0, vec.y)
    }

    @Test
    fun `test set with x and y`() {
        val vec = Vec2()
        vec.set(2.0, 3.0)
        assertEquals(2.0, vec.x)
        assertEquals(3.0, vec.y)
    }

    @Test
    fun `test set with single value`() {
        val vec = Vec2()
        vec.set(5.0)
        assertEquals(5.0, vec.x)
        assertEquals(5.0, vec.y)
    }

    @Test
    fun `test set with another Vec2`() {
        val vec1 = Vec2(1.0, 2.0)
        val vec2 = Vec2()
        vec2.set(vec1)
        assertEquals(1.0, vec2.x)
        assertEquals(2.0, vec2.y)
    }

    @Test
    fun `test magnitude`() {
        val vec = Vec2(3.0, 4.0)
        assertEquals(5.0, vec.magnitude(), 0.0001)
    }

    @Test
    fun `test normalized`() {
        val vec = Vec2(3.0, 4.0)
        val normalized = vec.normalized()
        assertEquals(0.6, normalized.x, 0.0001)
        assertEquals(0.8, normalized.y, 0.0001)
    }

    @Test
    fun `test normalized for zero vector`() {
        val vec = Vec2()
        val normalized = vec.normalized()
        assertEquals(0.0, normalized.x)
        assertEquals(0.0, normalized.y)
    }

    @Test
    fun `test plus operator`() {
        val vec1 = Vec2(1.0, 2.0)
        val vec2 = Vec2(3.0, 4.0)
        val result = vec1 + vec2
        assertEquals(4.0, result.x)
        assertEquals(6.0, result.y)
    }

    @Test
    fun `test minus operator`() {
        val vec1 = Vec2(5.0, 6.0)
        val vec2 = Vec2(3.0, 4.0)
        val result = vec1 - vec2
        assertEquals(2.0, result.x)
        assertEquals(2.0, result.y)
    }

    @Test
    fun `test times operator`() {
        val vec1 = Vec2(2.0, 3.0)
        val vec2 = Vec2(4.0, 5.0)
        val result = vec1 * vec2
        assertEquals(8.0, result.x)
        assertEquals(15.0, result.y)
    }

    @Test
    fun `test div operator`() {
        val vec1 = Vec2(8.0, 9.0)
        val vec2 = Vec2(2.0, 3.0)
        val result = vec1 / vec2
        assertEquals(4.0, result.x)
        assertEquals(3.0, result.y)
    }

    @Test
    fun `test scalar plus operator`() {
        val vec = Vec2(1.0, 2.0)
        val result = vec + 5.0
        assertEquals(6.0, result.x)
        assertEquals(7.0, result.y)
    }

    @Test
    fun `test scalar minus operator`() {
        val vec = Vec2(10.0, 20.0)
        val result = vec - 5.0
        assertEquals(5.0, result.x)
        assertEquals(15.0, result.y)
    }

    @Test
    fun `test scalar times operator`() {
        val vec = Vec2(2.0, 3.0)
        val result = vec * 4.0
        assertEquals(8.0, result.x)
        assertEquals(12.0, result.y)
    }

    @Test
    fun `test scalar div operator`() {
        val vec = Vec2(8.0, 16.0)
        val result = vec / 2.0
        assertEquals(4.0, result.x)
        assertEquals(8.0, result.y)
    }

    @Test
    fun `test plusAssign operator`() {
        val vec = Vec2(1.0, 2.0)
        vec += Vec2(3.0, 4.0)
        assertEquals(4.0, vec.x)
        assertEquals(6.0, vec.y)
    }

    @Test
    fun `test minusAssign operator`() {
        val vec = Vec2(5.0, 6.0)
        vec -= Vec2(3.0, 4.0)
        assertEquals(2.0, vec.x)
        assertEquals(2.0, vec.y)
    }

    @Test
    fun `test timesAssign operator`() {
        val vec = Vec2(2.0, 3.0)
        vec *= Vec2(4.0, 5.0)
        assertEquals(8.0, vec.x)
        assertEquals(15.0, vec.y)
    }

    @Test
    fun `test divAssign operator`() {
        val vec = Vec2(8.0, 9.0)
        vec /= Vec2(2.0, 3.0)
        assertEquals(4.0, vec.x)
        assertEquals(3.0, vec.y)
    }
}