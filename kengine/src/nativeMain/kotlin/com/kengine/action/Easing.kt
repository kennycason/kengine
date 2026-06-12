package com.kengine.action

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

typealias EasingFunction = (Double) -> Double

object Easing {
    val linear: EasingFunction = { t -> t }

    // Quadratic
    val easeInQuad: EasingFunction = { t -> t * t }
    val easeOutQuad: EasingFunction = { t -> t * (2 - t) }
    val easeInOutQuad: EasingFunction = { t ->
        if (t < 0.5) 2 * t * t
        else -1 + (4 - 2 * t) * t
    }

    // Cubic
    val easeInCubic: EasingFunction = { t -> t * t * t }
    val easeOutCubic: EasingFunction = { t -> (t - 1).let { it * it * it + 1 } }
    val easeInOutCubic: EasingFunction = { t ->
        if (t < 0.5) 4 * t * t * t
        else (t - 1).let { 1 + 4 * it * it * it }
    }

    // Quartic
    val easeInQuart: EasingFunction = { t -> t * t * t * t }
    val easeOutQuart: EasingFunction = { t -> 1 - (t - 1).let { it * it * it * it } }
    val easeInOutQuart: EasingFunction = { t ->
        if (t < 0.5) 8 * t * t * t * t
        else 1 - (t - 1).let { 8 * it * it * it * it }
    }

    // Sine
    val easeInSine: EasingFunction = { t -> 1 - cos(t * PI / 2) }
    val easeOutSine: EasingFunction = { t -> sin(t * PI / 2) }
    val easeInOutSine: EasingFunction = { t -> -(cos(PI * t) - 1) / 2 }

    // Exponential
    val easeInExpo: EasingFunction = { t -> if (t == 0.0) 0.0 else 2.0.pow(10 * (t - 1)) }
    val easeOutExpo: EasingFunction = { t -> if (t == 1.0) 1.0 else 1 - 2.0.pow(-10 * t) }
    val easeInOutExpo: EasingFunction = { t ->
        when {
            t == 0.0 -> 0.0
            t == 1.0 -> 1.0
            t < 0.5 -> 2.0.pow(20 * t - 10) / 2
            else -> (2 - 2.0.pow(-20 * t + 10)) / 2
        }
    }

    // Circular
    val easeInCirc: EasingFunction = { t -> 1 - sqrt(1 - t * t) }
    val easeOutCirc: EasingFunction = { t -> sqrt(1 - (t - 1).let { it * it }) }
    val easeInOutCirc: EasingFunction = { t ->
        if (t < 0.5) (1 - sqrt(1 - (2 * t).let { it * it })) / 2
        else (sqrt(1 - (-2 * t + 2).let { it * it }) + 1) / 2
    }

    // Back (overshoots then returns)
    val easeInBack: EasingFunction = { t ->
        val c = 1.70158
        (c + 1) * t * t * t - c * t * t
    }
    val easeOutBack: EasingFunction = { t ->
        val c = 1.70158
        val t1 = t - 1
        1 + (c + 1) * t1 * t1 * t1 + c * t1 * t1
    }
    val easeInOutBack: EasingFunction = { t ->
        val c = 1.70158 * 1.525
        if (t < 0.5) ((2 * t).let { it * it } * ((c + 1) * 2 * t - c)) / 2
        else ((2 * t - 2).let { it * it } * ((c + 1) * (t * 2 - 2) + c) + 2) / 2
    }

    // Elastic
    val easeInElastic: EasingFunction = { t ->
        when {
            t == 0.0 -> 0.0
            t == 1.0 -> 1.0
            else -> -(2.0.pow(10 * t - 10)) * sin((t * 10 - 10.75) * (2 * PI / 3))
        }
    }
    val easeOutElastic: EasingFunction = { t ->
        when {
            t == 0.0 -> 0.0
            t == 1.0 -> 1.0
            else -> 2.0.pow(-10 * t) * sin((t * 10 - 0.75) * (2 * PI / 3)) + 1
        }
    }
    val easeInOutElastic: EasingFunction = { t ->
        when {
            t == 0.0 -> 0.0
            t == 1.0 -> 1.0
            t < 0.5 -> -(2.0.pow(20 * t - 10) * sin((20 * t - 11.125) * (2 * PI / 4.5))) / 2
            else -> (2.0.pow(-20 * t + 10) * sin((20 * t - 11.125) * (2 * PI / 4.5))) / 2 + 1
        }
    }

    // Bounce
    val easeOutBounce: EasingFunction = { t ->
        when {
            t < 1.0 / 2.75 -> 7.5625 * t * t
            t < 2.0 / 2.75 -> (t - 1.5 / 2.75).let { 7.5625 * it * it + 0.75 }
            t < 2.5 / 2.75 -> (t - 2.25 / 2.75).let { 7.5625 * it * it + 0.9375 }
            else -> (t - 2.625 / 2.75).let { 7.5625 * it * it + 0.984375 }
        }
    }
    val easeInBounce: EasingFunction = { t -> 1 - easeOutBounce(1 - t) }
    val easeInOutBounce: EasingFunction = { t ->
        if (t < 0.5) (1 - easeOutBounce(1 - 2 * t)) / 2
        else (1 + easeOutBounce(2 * t - 1)) / 2
    }
}
