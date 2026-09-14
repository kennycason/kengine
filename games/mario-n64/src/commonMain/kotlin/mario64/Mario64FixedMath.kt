package mario64

private const val TRIG_SCALE = 4096
private const val ANGLE_FULL = 1024
private const val ANGLE_RIGHT = ANGLE_FULL / 4

internal fun sinAngle(angle: Int): Int {
    val wrapped = angle and (ANGLE_FULL - 1)
    return when {
        wrapped < ANGLE_RIGHT -> quarterSin(wrapped)
        wrapped < ANGLE_RIGHT * 2 -> quarterSin(ANGLE_RIGHT * 2 - wrapped)
        wrapped < ANGLE_RIGHT * 3 -> -quarterSin(wrapped - ANGLE_RIGHT * 2)
        else -> -quarterSin(ANGLE_FULL - wrapped)
    }
}

internal fun cosAngle(angle: Int): Int = sinAngle(angle + ANGLE_RIGHT)

internal fun trigMul(value: Int, trig: Int): Int = (value * trig) / TRIG_SCALE

private fun quarterSin(value: Int): Int {
    val bounded = clampInt(value, 0, ANGLE_RIGHT)
    val numerator = bounded * (ANGLE_RIGHT * 2 - bounded)
    return scaleValue(numerator, TRIG_SCALE, ANGLE_RIGHT * ANGLE_RIGHT)
}

private fun scaleValue(value: Int, numerator: Int, denominator: Int): Int {
    if (denominator == 0) return 0
    val scaled = value * numerator
    return if (scaled >= 0) {
        (scaled + denominator / 2) / denominator
    } else {
        (scaled - denominator / 2) / denominator
    }
}

internal fun wrapAngle(angle: Int): Int = angle and (ANGLE_FULL - 1)

internal fun clampInt(value: Int, min: Int, max: Int): Int {
    return when {
        value < min -> min
        value > max -> max
        else -> value
    }
}

internal fun rgba(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
    return clampInt(red, 0, 255) or
        (clampInt(green, 0, 255) shl 8) or
        (clampInt(blue, 0, 255) shl 16) or
        (clampInt(alpha, 0, 255) shl 24)
}
