package com.kengine.math

import com.kengine.test.expectThat
import kotlin.test.Test

class IntVec2Test {

    @Test
    fun `test default constructor`() {
        val vec = IntVec2()
        expectThat(vec.x).isEqualTo(0)
        expectThat(vec.y).isEqualTo(0)
    }

    @Test
    fun `test parameterized constructor`() {
        val vec = IntVec2(3, 4)
        expectThat(vec.x).isEqualTo(3)
        expectThat(vec.y).isEqualTo(4)
    }

    @Test
    fun `test set with x and y`() {
        val vec = IntVec2()
        vec.set(2, 3)
        expectThat(vec.x).isEqualTo(2)
        expectThat(vec.y).isEqualTo(3)
    }

    @Test
    fun `test set with single value`() {
        val vec = IntVec2()
        vec.set(5)
        expectThat(vec.x).isEqualTo(5)
        expectThat(vec.y).isEqualTo(5)
    }

    @Test
    fun `test set with another IntVec2`() {
        val vec1 = IntVec2(1, 2)
        val vec2 = IntVec2()
        vec2.set(vec1)
        expectThat(vec2.x).isEqualTo(1)
        expectThat(vec2.y).isEqualTo(2)
    }

    @Test
    fun `test magnitude`() {
        val vec = IntVec2(3, 4)
        expectThat(vec.magnitude()).isEqualTo(5.0)
    }

    @Test
    fun `test normalized`() {
        val vec = IntVec2(3, 4)
        val normalized = vec.normalized()
        expectThat(normalized.x).isEqualTo(0.6)
        expectThat(normalized.y).isEqualTo(0.8)
    }

    @Test
    fun `test normalized for zero vector`() {
        val vec = IntVec2()
        val normalized = vec.normalized()
        expectThat(normalized.x).isEqualTo(0.0)
        expectThat(normalized.y).isEqualTo(0.0)
    }

    @Test
    fun `test plus operator`() {
        val vec1 = IntVec2(1, 2)
        val vec2 = IntVec2(3, 4)
        val result = vec1 + vec2
        expectThat(result.x).isEqualTo(4)
        expectThat(result.y).isEqualTo(6)
    }

    @Test
    fun `test minus operator`() {
        val vec1 = IntVec2(5, 6)
        val vec2 = IntVec2(3, 4)
        val result = vec1 - vec2
        expectThat(result.x).isEqualTo(2)
        expectThat(result.y).isEqualTo(2)
    }

    @Test
    fun `test times operator`() {
        val vec1 = IntVec2(2, 3)
        val vec2 = IntVec2(4, 5)
        val result = vec1 * vec2
        expectThat(result.x).isEqualTo(8)
        expectThat(result.y).isEqualTo(15)
    }

    @Test
    fun `test div operator`() {
        val vec1 = IntVec2(8, 9)
        val vec2 = IntVec2(2, 3)
        val result = vec1 / vec2
        expectThat(result.x).isEqualTo(4)
        expectThat(result.y).isEqualTo(3)
    }

    @Test
    fun `test scalar plus operator`() {
        val vec = IntVec2(1, 2)
        val result = vec + 5
        expectThat(result.x).isEqualTo(6)
        expectThat(result.y).isEqualTo(7)
    }

    @Test
    fun `test scalar minus operator`() {
        val vec = IntVec2(10, 20)
        val result = vec - 5
        expectThat(result.x).isEqualTo(5)
        expectThat(result.y).isEqualTo(15)
    }

    @Test
    fun `test scalar times operator`() {
        val vec = IntVec2(2, 3)
        val result = vec * 4
        expectThat(result.x).isEqualTo(8)
        expectThat(result.y).isEqualTo(12)
    }

    @Test
    fun `test scalar div operator`() {
        val vec = IntVec2(8, 16)
        val result = vec / 2
        expectThat(result.x).isEqualTo(4)
        expectThat(result.y).isEqualTo(8)
    }

    @Test
    fun `test plusAssign operator`() {
        val vec = IntVec2(1, 2)
        vec += IntVec2(3, 4)
        expectThat(vec.x).isEqualTo(4)
        expectThat(vec.y).isEqualTo(6)
    }

    @Test
    fun `test minusAssign operator`() {
        val vec = IntVec2(5, 6)
        vec -= IntVec2(3, 4)
        expectThat(vec.x).isEqualTo(2)
        expectThat(vec.y).isEqualTo(2)
    }

    @Test
    fun `test timesAssign operator`() {
        val vec = IntVec2(2, 3)
        vec *= IntVec2(4, 5)
        expectThat(vec.x).isEqualTo(8)
        expectThat(vec.y).isEqualTo(15)
    }

    @Test
    fun `test divAssign operator`() {
        val vec = IntVec2(8, 9)
        vec /= IntVec2(2, 3)
        expectThat(vec.x).isEqualTo(4)
        expectThat(vec.y).isEqualTo(3)
    }
}