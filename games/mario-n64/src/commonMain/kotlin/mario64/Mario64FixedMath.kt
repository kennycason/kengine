package mario64

private const val TRIG_SCALE = 4096
private const val ANGLE_FULL = 1024
private const val ANGLE_RIGHT = ANGLE_FULL / 4
private const val ANGLE_HALF = ANGLE_FULL / 2

internal fun sinAngle(angle: Int): Int {
    val wrapped = angle and (ANGLE_FULL - 1)
    val halfAngle = if (wrapped <= ANGLE_HALF) wrapped else ANGLE_FULL - wrapped
    val product = halfAngle * (ANGLE_HALF - halfAngle)
    val denominator = 5 * ANGLE_HALF * ANGLE_HALF / 16 - product / 4
    val magnitude = if (denominator == 0) 0 else (product * TRIG_SCALE + denominator / 2) / denominator
    return if (wrapped <= ANGLE_HALF) magnitude else -magnitude
}

internal fun cosAngle(angle: Int): Int = sinAngle(angle + ANGLE_RIGHT)

internal fun trigMul(value: Int, trig: Int): Int = (value * trig) / TRIG_SCALE

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
