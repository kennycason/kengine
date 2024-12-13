package com.kengine.math

import com.kengine.test.expectThat
import kotlin.test.Test

class IntRectTest {

    @Test
    fun `test default constructor`() {
        val rect = IntRect()
        expectThat(rect.x).isEqualTo(0)
        expectThat(rect.y).isEqualTo(0)
        expectThat(rect.w).isEqualTo(0)
        expectThat(rect.h).isEqualTo(0)
    }

    @Test
    fun `test parameterized constructor`() {
        val rect = IntRect(1, 2, 3, 4)
        expectThat(rect.x).isEqualTo(1)
        expectThat(rect.y).isEqualTo(2)
        expectThat(rect.w).isEqualTo(3)
        expectThat(rect.h).isEqualTo(4)
    }

    @Test
    fun `test area`() {
        val rect = IntRect(0, 0, 3, 4)
        expectThat(rect.area()).isEqualTo(12)
    }

    @Test
    fun `test perimeter`() {
        val rect = IntRect(0, 0, 3, 4)
        expectThat(rect.perimeter()).isEqualTo(14)
    }

    @Test
    fun `test translate`() {
        val rect = IntRect(1, 2, 3, 4)
        val translated = rect.translate(2, 3)
        expectThat(translated.x).isEqualTo(3)
        expectThat(translated.y).isEqualTo(5)
        expectThat(translated.w).isEqualTo(3)
        expectThat(translated.h).isEqualTo(4)
    }

    @Test
    fun `test translateAssign`() {
        val rect = IntRect(1, 2, 3, 4)
        rect.translateAssign(2, 3)
        expectThat(rect.x).isEqualTo(3)
        expectThat(rect.y).isEqualTo(5)
    }

    @Test
    fun `test scale`() {
        val rect = IntRect(0, 0, 3, 4)
        val scaled = rect.scale(2, 3)
        expectThat(scaled.w).isEqualTo(6)
        expectThat(scaled.h).isEqualTo(12)
    }

    @Test
    fun `test scaleAssign`() {
        val rect = IntRect(0, 0, 3, 4)
        rect.scaleAssign(2, 3)
        expectThat(rect.w).isEqualTo(6)
        expectThat(rect.h).isEqualTo(12)
    }

    @Test
    fun `test contains point`() {
        val rect = IntRect(1, 1, 3, 3)
        expectThat(rect.contains(IntVec2(2, 2))).isTrue()
        expectThat(rect.contains(IntVec2(0, 0))).isFalse()
    }

    @Test
    fun `test contains point - vec2`() {
        val rect = IntRect(1, 1, 3, 3)
        expectThat(rect.contains(Vec2(2.0, 2.0))).isTrue()
        expectThat(rect.contains(Vec2(0.0, 0.0))).isFalse()
    }

    @Test
    fun `test overlaps`() {
        val rect1 = IntRect(0, 0, 3, 3)
        val rect2 = IntRect(2, 2, 3, 3)
        val rect3 = IntRect(4, 4, 2, 2)
        expectThat(rect1.overlaps(rect2)).isTrue()
        expectThat(rect1.overlaps(rect3)).isFalse()
    }

    @Test
    fun `test overlaps - rect`() {
        val rect1 = IntRect(0, 0, 3, 3)
        val rect2 = Rect(2.0, 2.0, 3.0, 3.0)
        val rect3 = Rect(4.0, 4.0, 2.0, 2.0)
        expectThat(rect1.overlaps(rect2)).isTrue()
        expectThat(rect1.overlaps(rect3)).isFalse()
    }

    @Test
    fun `test intersection`() {
        val rect1 = IntRect(0, 0, 3, 3)
        val rect2 = IntRect(2, 2, 3, 3)
        val intersection = rect1.intersection(rect2)
        expectThat(intersection).isNotNull()
        expectThat(intersection).isEqualTo(IntRect(2, 2, 1, 1))
    }

    @Test
    fun `test no intersection`() {
        val rect1 = IntRect(0, 0, 2, 2)
        val rect2 = IntRect(3, 3, 2, 2)
        val intersection = rect1.intersection(rect2)
        expectThat(intersection).isNull()
    }

    @Test
    fun `test union`() {
        val rect1 = IntRect(0, 0, 3, 3)
        val rect2 = IntRect(2, 2, 3, 3)
        val union = rect1.union(rect2)
        expectThat(union).isEqualTo(IntRect(0, 0, 5, 5))
    }
}