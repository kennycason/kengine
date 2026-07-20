package com.kengine.input

import kotlin.test.Test
import kotlin.test.assertEquals

class InputAxisTest {
    @Test
    fun digitalAxisUsesPositiveAndNegativeButtons() {
        assertEquals(1.0, digitalAxis(positivePressed = true, negativePressed = false))
        assertEquals(-1.0, digitalAxis(positivePressed = false, negativePressed = true))
        assertEquals(0.0, digitalAxis(positivePressed = false, negativePressed = false))
        assertEquals(0.0, digitalAxis(positivePressed = true, negativePressed = true))
    }

    @Test
    fun snapAxisClampsAndZerosSmallValues() {
        assertEquals(0.0, snapAxis(0.04, epsilon = 0.05))
        assertEquals(0.06, snapAxis(0.06, epsilon = 0.05))
        assertEquals(1.0, snapAxis(1.3, epsilon = 0.05))
        assertEquals(-1.0, snapAxis(-1.3, epsilon = 0.05))
    }
}
