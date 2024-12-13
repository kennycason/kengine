package com.kengine.math

import com.kengine.test.expectThat
import kotlin.test.Test

class RectTest {

    @Test
    fun `test default constructor`() {
        val rect = Rect()
        expectThat(rect.x).isEqualTo(0.0)
        expectThat(rect.y).isEqualTo(0.0)
        expectThat(rect.w).isEqualTo(0.0)
        expectThat(rect.h).isEqualTo(0.0)
    }

    @Test
    fun `test parameterized constructor`() {
        val rect = Rect(1.0, 2.0, 3.0, 4.0)
        expectThat(rect.x).isEqualTo(1.0)
        expectThat(rect.y).isEqualTo(2.0)
        expectThat(rect.w).isEqualTo(3.0)
        expectThat(rect.h).isEqualTo(4.0)
    }

    @Test
    fun `test area`() {
        val rect = Rect(0.0, 0.0, 3.0, 4.0)
        expectThat(rect.area()).isEqualTo(12.0)
    }

    @Test
    fun `test perimeter`() {
        val rect = Rect(0.0, 0.0, 3.0, 4.0)
        expectThat(rect.perimeter()).isEqualTo(14.0)
    }

    @Test
    fun `test translate`() {
        val rect = Rect(1.0, 2.0, 3.0, 4.0)
        val translated = rect.translate(2.0, 3.0)
        expectThat(translated.x).isEqualTo(3.0)
        expectThat(translated.y).isEqualTo(5.0)
        expectThat(translated.w).isEqualTo(3.0)
        expectThat(translated.h).isEqualTo(4.0)
    }

    @Test
    fun `test translateAssign`() {
        val rect = Rect(1.0, 2.0, 3.0, 4.0)
        rect.translateAssign(2.0, 3.0)
        expectThat(rect.x).isEqualTo(3.0)
        expectThat(rect.y).isEqualTo(5.0)
    }

    @Test
    fun `test scale`() {
        val rect = Rect(0.0, 0.0, 3.0, 4.0)
        val scaled = rect.scale(2.0, 3.0)
        expectThat(scaled.w).isEqualTo(6.0)
        expectThat(scaled.h).isEqualTo(12.0)
    }

    @Test
    fun `test scaleAssign`() {
        val rect = Rect(0.0, 0.0, 3.0, 4.0)
        rect.scaleAssign(2.0, 3.0)
        expectThat(rect.w).isEqualTo(6.0)
        expectThat(rect.h).isEqualTo(12.0)
    }

    @Test
    fun `test contains point`() {
        val rect = Rect(1.0, 1.0, 3.0, 3.0)
        expectThat(rect.contains(Vec2(2.0, 2.0))).isTrue()
        expectThat(rect.contains(Vec2(0.0, 0.0))).isFalse()
    }

    @Test
    fun `test contains point - intvec2`() {
        val rect = Rect(1.0, 1.0, 3.0, 3.0)
        expectThat(rect.contains(IntVec2(2, 2))).isTrue()
        expectThat(rect.contains(IntVec2(0, 0))).isFalse()
    }

    @Test
    fun `test overlaps`() {
        val rect1 = Rect(0.0, 0.0, 3.0, 3.0)
        val rect2 = Rect(2.0, 2.0, 3.0, 3.0)
        val rect3 = Rect(4.0, 4.0, 2.0, 2.0)
        expectThat(rect1.overlaps(rect2)).isTrue()
        expectThat(rect1.overlaps(rect3)).isFalse()
    }

    @Test
    fun `test overlaps - intrect2`() {
        val rect1 = Rect(0.0, 0.0, 3.0, 3.0)
        val rect2 = IntRect(2, 2, 3, 3)
        val rect3 = IntRect(4, 4, 2, 2)
        expectThat(rect1.overlaps(rect2)).isTrue()
        expectThat(rect1.overlaps(rect3)).isFalse()
    }

    @Test
    fun `test intersection`() {
        val rect1 = Rect(0.0, 0.0, 3.0, 3.0)
        val rect2 = Rect(2.0, 2.0, 3.0, 3.0)
        val intersection = rect1.intersection(rect2)
        expectThat(intersection).isNotNull()
        expectThat(intersection).isEqualTo(Rect(2.0, 2.0, 1.0, 1.0))
    }

    @Test
    fun `test no intersection`() {
        val rect1 = Rect(0.0, 0.0, 2.0, 2.0)
        val rect2 = Rect(3.0, 3.0, 2.0, 2.0)
        val intersection = rect1.intersection(rect2)
        expectThat(intersection).isNull()
    }

    @Test
    fun `test union`() {
        val rect1 = Rect(0.0, 0.0, 3.0, 3.0)
        val rect2 = Rect(2.0, 2.0, 3.0, 3.0)
        val union = rect1.union(rect2)
        expectThat(union).isEqualTo(Rect(0.0, 0.0, 5.0, 5.0))
    }
}