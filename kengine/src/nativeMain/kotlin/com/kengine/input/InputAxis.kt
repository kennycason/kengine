package com.kengine.input

import kotlin.math.abs

fun digitalAxis(
    positivePressed: Boolean,
    negativePressed: Boolean
): Double {
    return when {
        positivePressed && !negativePressed -> 1.0
        negativePressed && !positivePressed -> -1.0
        else -> 0.0
    }
}

fun snapAxis(
    value: Double,
    epsilon: Double
): Double {
    val clamped = value.coerceIn(-1.0, 1.0)
    return if (abs(clamped) < epsilon) 0.0 else clamped
}
