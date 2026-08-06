package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals

class MathTest {
    @Test
    fun convertsBetweenRadiansAndDegrees() {
        assertEquals(0.7853981633974483, Math.toRadians(45.0))
        assertEquals(45.0, Math.toDegrees(Math.PI / 4))
    }
}
