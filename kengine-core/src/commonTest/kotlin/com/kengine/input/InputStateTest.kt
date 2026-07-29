package com.kengine.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputStateTest {
    @Test
    fun tracksPressedButtons() {
        val input = InputState()

        input.set(InputButton.LEFT)
        input.set(InputButton.A)

        assertTrue(input.isPressed(InputButton.LEFT))
        assertTrue(input.isPressed(InputButton.A))
        assertFalse(input.isPressed(InputButton.RIGHT))

        input.set(InputButton.LEFT, pressed = false)

        assertFalse(input.isPressed(InputButton.LEFT))
        assertTrue(input.isPressed(InputButton.A))
    }

    @Test
    fun computesDigitalAxis() {
        val input = InputState()

        input.set(InputButton.RIGHT)
        assertEquals(1, input.axis(InputButton.LEFT, InputButton.RIGHT))

        input.set(InputButton.LEFT)
        assertEquals(0, input.axis(InputButton.LEFT, InputButton.RIGHT))

        input.set(InputButton.RIGHT, pressed = false)
        assertEquals(-1, input.axis(InputButton.LEFT, InputButton.RIGHT))
    }

    @Test
    fun canResetSetMaskAndCopy() {
        val source = InputState()
        val target = InputState()

        source.set(InputButton.DOWN)
        source.set(InputButton.B)
        target.copyFrom(source)

        assertEquals(source.mask, target.mask)
        assertTrue(target.isPressed(InputButton.DOWN))
        assertTrue(target.isPressed(InputButton.B))

        target.reset()
        assertEquals(0, target.mask)

        target.setMask(InputState.bitFor(InputButton.START))
        assertTrue(target.isPressed(InputButton.START))
    }
}
