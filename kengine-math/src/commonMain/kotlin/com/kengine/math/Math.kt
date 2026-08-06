package com.kengine.math

object Math {
    const val PI: Double = kotlin.math.PI
    const val E: Double = kotlin.math.E
    const val GOLDEN_RATIO: Double = 1.618033988749895
    const val SQRT_2: Double = 1.4142135623730951
    const val SQRT_3: Double = 1.7320508075688772
    const val SQRT_5: Double = 2.23606797749979
    const val LN_2: Double = 0.6931471805599453
    const val LN_10: Double = 2.302585092994046
    const val LOG2_E: Double = 1.4426950408889634
    const val LOG10_E: Double = 0.4342944819032518
    const val TAU: Double = 2 * PI
    const val PI_HALF: Double = PI / 2
    const val PI_THIRD: Double = PI / 3
    const val PI_QUARTER: Double = PI / 4
    const val ONE_OVER_PI: Double = 1.0 / PI
    const val TWO_OVER_PI: Double = 2.0 / PI
    const val TWO_OVER_SQRT_PI: Double = 1.128379167095513
    const val EULER_MASCHERONI: Double = 0.57721566490153286
    const val SILVER_RATIO: Double = 2.414213562373095
    const val APERY: Double = 1.2020569031595943
    const val CATALAN: Double = 0.915965594177219

    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI

    fun toRadians(degrees: Double): Double = degrees * DEG_TO_RAD

    fun toDegrees(radians: Double): Double = radians * RAD_TO_DEG
}
