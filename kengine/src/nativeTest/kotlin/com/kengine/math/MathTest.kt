package com.kengine.math

import com.kengine.test.expectThat
import kotlin.test.Test

class MathTest {
    @Test
    fun `radians to degree`() {
        expectThat(Math.toRadians(45.0)).isEqualTo(0.7853981633974483)
        expectThat(Math.toDegrees(Math.PI / 4)).isEqualTo(45.0)
    }
}
