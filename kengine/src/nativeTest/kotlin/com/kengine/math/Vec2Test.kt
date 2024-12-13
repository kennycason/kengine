package com.kengine.math

import com.kengine.test.expectThat
import kotlin.test.Test

class Vec2Test {

    @Test
    fun `test default constructor`() {
        val vec = Vec2()
        expectThat(vec.x).isEqualTo(0.0)
        expectThat(vec.y).isEqualTo(0.0)
    }

    @Test
    fun `test parameterized constructor`() {
        val vec = Vec2(3.0, 4.0)
        expectThat(vec.x).isEqualTo(3.0)
        expectThat(vec.y).isEqualTo(4.0)
    }

    @Test
    fun `test set with x and y`() {
        val vec = Vec2()
        vec.set(2.0, 3.0)
        expectThat(vec.x).isEqualTo(2.0)
        expectThat(vec.y).isEqualTo(3.0)
    }

    @Test
    fun `test set with single value`() {
        val vec = Vec2()
        vec.set(5.0)
        expectThat(vec.x).isEqualTo(5.0)
        expectThat(vec.y).isEqualTo(5.0)
    }

    @Test
    fun `test set with another Vec2`() {
        val vec1 = Vec2(1.0, 2.0)
        val vec2 = Vec2()
        vec2.set(vec1)
        expectThat(vec2.x).isEqualTo(1.0)
        expectThat(vec2.y).isEqualTo(2.0)
    }

    @Test
    fun `test magnitude`() {
        val vec = Vec2(3.0, 4.0)
        expectThat(vec.magnitude()).isEqualTo(5.0)
    }

    @Test
    fun `test normalized`() {
        val vec = Vec2(3.0, 4.0)
        val normalized = vec.normalized()
        expectThat(normalized.x).isEqualTo(0.6)
        expectThat(normalized.y).isEqualTo(0.8)
    }

    @Test
    fun `test normalized for zero vector`() {
        val vec = Vec2()
        val normalized = vec.normalized()
        expectThat(normalized.x).isEqualTo(0.0)
        expectThat(normalized.y).isEqualTo(0.0)
    }

    @Test
    fun `test plus operator`() {
        val vec1 = Vec2(1.0, 2.0)
        val vec2 = Vec2(3.0, 4.0)
        val result = vec1 + vec2
        expectThat(result.x).isEqualTo(4.0)
        expectThat(result.y).isEqualTo(6.0)
    }

    @Test
    fun `test minus operator`() {
        val vec1 = Vec2(5.0, 6.0)
        val vec2 = Vec2(3.0, 4.0)
        val result = vec1 - vec2
        expectThat(result.x).isEqualTo(2.0)
        expectThat(result.y).isEqualTo(2.0)
    }

    @Test
    fun `test times operator`() {
        val vec1 = Vec2(2.0, 3.0)
        val vec2 = Vec2(4.0, 5.0)
        val result = vec1 * vec2
        expectThat(result.x).isEqualTo(8.0)
        expectThat(result.y).isEqualTo(15.0)
    }

    @Test
    fun `test div operator`() {
        val vec1 = Vec2(8.0, 9.0)
        val vec2 = Vec2(2.0, 3.0)
        val result = vec1 / vec2
        expectThat(result.x).isEqualTo(4.0)
        expectThat(result.y).isEqualTo(3.0)
    }

    @Test
    fun `test scalar plus operator`() {
        val vec = Vec2(1.0, 2.0)
        val result = vec + 5.0
        expectThat(result.x).isEqualTo(6.0)
        expectThat(result.y).isEqualTo(7.0)
    }

    @Test
    fun `test scalar minus operator`() {
        val vec = Vec2(10.0, 20.0)
        val result = vec - 5.0
        expectThat(result.x).isEqualTo(5.0)
        expectThat(result.y).isEqualTo(15.0)
    }

    @Test
    fun `test scalar times operator`() {
        val vec = Vec2(2.0, 3.0)
        val result = vec * 4.0
        expectThat(result.x).isEqualTo(8.0)
        expectThat(result.y).isEqualTo(12.0)
    }

    @Test
    fun `test scalar div operator`() {
        val vec = Vec2(8.0, 16.0)
        val result = vec / 2.0
        expectThat(result.x).isEqualTo(4.0)
        expectThat(result.y).isEqualTo(8.0)
    }

    @Test
    fun `test plusAssign operator`() {
        val vec = Vec2(1.0, 2.0)
        vec += Vec2(3.0, 4.0)
        expectThat(vec.x).isEqualTo(4.0)
        expectThat(vec.y).isEqualTo(6.0)
    }

    @Test
    fun `test minusAssign operator`() {
        val vec = Vec2(5.0, 6.0)
        vec -= Vec2(3.0, 4.0)
        expectThat(vec.x).isEqualTo(2.0)
        expectThat(vec.y).isEqualTo(2.0)
    }

    @Test
    fun `test timesAssign operator`() {
        val vec = Vec2(2.0, 3.0)
        vec *= Vec2(4.0, 5.0)
        expectThat(vec.x).isEqualTo(8.0)
        expectThat(vec.y).isEqualTo(15.0)
    }

    @Test
    fun `test divAssign operator`() {
        val vec = Vec2(8.0, 9.0)
        vec /= Vec2(2.0, 3.0)
        expectThat(vec.x).isEqualTo(4.0)
        expectThat(vec.y).isEqualTo(3.0)
    }
}