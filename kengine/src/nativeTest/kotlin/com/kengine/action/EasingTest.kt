package com.kengine.action

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class EasingTest {
    private val eps = 0.0001

    private fun assertClose(expected: Double, actual: Double, msg: String = "") {
        assertTrue(abs(expected - actual) < eps, "$msg expected $expected but was $actual")
    }

    @Test
    fun `all easing functions return 0 at t=0`() {
        val easings = listOf(
            "linear" to Easing.linear,
            "easeInQuad" to Easing.easeInQuad,
            "easeOutQuad" to Easing.easeOutQuad,
            "easeInOutQuad" to Easing.easeInOutQuad,
            "easeInCubic" to Easing.easeInCubic,
            "easeOutCubic" to Easing.easeOutCubic,
            "easeInOutCubic" to Easing.easeInOutCubic,
            "easeInQuart" to Easing.easeInQuart,
            "easeOutQuart" to Easing.easeOutQuart,
            "easeInOutQuart" to Easing.easeInOutQuart,
            "easeInSine" to Easing.easeInSine,
            "easeOutSine" to Easing.easeOutSine,
            "easeInOutSine" to Easing.easeInOutSine,
            "easeInExpo" to Easing.easeInExpo,
            "easeOutExpo" to Easing.easeOutExpo,
            "easeInOutExpo" to Easing.easeInOutExpo,
            "easeInCirc" to Easing.easeInCirc,
            "easeOutCirc" to Easing.easeOutCirc,
            "easeInOutCirc" to Easing.easeInOutCirc,
            "easeInBack" to Easing.easeInBack,
            "easeOutBack" to Easing.easeOutBack,
            "easeInOutBack" to Easing.easeInOutBack,
            "easeInElastic" to Easing.easeInElastic,
            "easeOutElastic" to Easing.easeOutElastic,
            "easeInOutElastic" to Easing.easeInOutElastic,
            "easeInBounce" to Easing.easeInBounce,
            "easeOutBounce" to Easing.easeOutBounce,
            "easeInOutBounce" to Easing.easeInOutBounce,
        )
        easings.forEach { (name, fn) ->
            assertClose(0.0, fn(0.0), "$name at t=0:")
        }
    }

    @Test
    fun `all easing functions return 1 at t=1`() {
        val easings = listOf(
            "linear" to Easing.linear,
            "easeInQuad" to Easing.easeInQuad,
            "easeOutQuad" to Easing.easeOutQuad,
            "easeInOutQuad" to Easing.easeInOutQuad,
            "easeInCubic" to Easing.easeInCubic,
            "easeOutCubic" to Easing.easeOutCubic,
            "easeInOutCubic" to Easing.easeInOutCubic,
            "easeInSine" to Easing.easeInSine,
            "easeOutSine" to Easing.easeOutSine,
            "easeInOutSine" to Easing.easeInOutSine,
            "easeInExpo" to Easing.easeInExpo,
            "easeOutExpo" to Easing.easeOutExpo,
            "easeInOutExpo" to Easing.easeInOutExpo,
            "easeInCirc" to Easing.easeInCirc,
            "easeOutCirc" to Easing.easeOutCirc,
            "easeInOutCirc" to Easing.easeInOutCirc,
            "easeInBack" to Easing.easeInBack,
            "easeOutBack" to Easing.easeOutBack,
            "easeInOutBack" to Easing.easeInOutBack,
            "easeInElastic" to Easing.easeInElastic,
            "easeOutElastic" to Easing.easeOutElastic,
            "easeInOutElastic" to Easing.easeInOutElastic,
            "easeInBounce" to Easing.easeInBounce,
            "easeOutBounce" to Easing.easeOutBounce,
            "easeInOutBounce" to Easing.easeInOutBounce,
        )
        easings.forEach { (name, fn) ->
            assertClose(1.0, fn(1.0), "$name at t=1:")
        }
    }

    @Test
    fun `linear returns input unchanged`() {
        assertClose(0.0, Easing.linear(0.0))
        assertClose(0.25, Easing.linear(0.25))
        assertClose(0.5, Easing.linear(0.5))
        assertClose(0.75, Easing.linear(0.75))
        assertClose(1.0, Easing.linear(1.0))
    }

    @Test
    fun `easeIn functions are below linear at midpoint`() {
        assertTrue(Easing.easeInQuad(0.5) < 0.5, "easeInQuad at 0.5 should be < 0.5")
        assertTrue(Easing.easeInCubic(0.5) < 0.5, "easeInCubic at 0.5 should be < 0.5")
        assertTrue(Easing.easeInQuart(0.5) < 0.5, "easeInQuart at 0.5 should be < 0.5")
        assertTrue(Easing.easeInSine(0.5) < 0.5, "easeInSine at 0.5 should be < 0.5")
        assertTrue(Easing.easeInCirc(0.5) < 0.5, "easeInCirc at 0.5 should be < 0.5")
    }

    @Test
    fun `easeOut functions are above linear at midpoint`() {
        assertTrue(Easing.easeOutQuad(0.5) > 0.5, "easeOutQuad at 0.5 should be > 0.5")
        assertTrue(Easing.easeOutCubic(0.5) > 0.5, "easeOutCubic at 0.5 should be > 0.5")
        assertTrue(Easing.easeOutQuart(0.5) > 0.5, "easeOutQuart at 0.5 should be > 0.5")
        assertTrue(Easing.easeOutSine(0.5) > 0.5, "easeOutSine at 0.5 should be > 0.5")
        assertTrue(Easing.easeOutCirc(0.5) > 0.5, "easeOutCirc at 0.5 should be > 0.5")
    }

    @Test
    fun `easeInOut functions pass through midpoint near 0_5`() {
        assertClose(0.5, Easing.easeInOutQuad(0.5), "easeInOutQuad")
        assertClose(0.5, Easing.easeInOutCubic(0.5), "easeInOutCubic")
        assertClose(0.5, Easing.easeInOutQuart(0.5), "easeInOutQuart")
        assertClose(0.5, Easing.easeInOutSine(0.5), "easeInOutSine")
        assertClose(0.5, Easing.easeInOutExpo(0.5), "easeInOutExpo")
        assertClose(0.5, Easing.easeInOutCirc(0.5), "easeInOutCirc")
    }

    @Test
    fun `easeBack overshoots beyond 0-1 range`() {
        assertTrue(Easing.easeInBack(0.3) < 0.0, "easeInBack should go negative")
        assertTrue(Easing.easeOutBack(0.7) > 1.0, "easeOutBack should exceed 1")
    }

    @Test
    fun `bounce has multiple bounces`() {
        val v1 = Easing.easeOutBounce(0.2)
        val v2 = Easing.easeOutBounce(0.4)
        val v3 = Easing.easeOutBounce(0.6)
        assertTrue(v1 > 0.0 && v1 < 1.0)
        assertTrue(v2 > 0.0 && v2 < 1.0)
        assertTrue(v3 > 0.0 && v3 < 1.0)
    }

    @Test
    fun `higher order easing is slower at start`() {
        val t = 0.25
        val quad = Easing.easeInQuad(t)
        val cubic = Easing.easeInCubic(t)
        val quart = Easing.easeInQuart(t)
        assertTrue(quad > cubic, "quad should be > cubic at low t")
        assertTrue(cubic > quart, "cubic should be > quart at low t")
    }
}
