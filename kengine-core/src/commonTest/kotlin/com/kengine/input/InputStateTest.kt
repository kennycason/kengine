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

    @Test
    fun preservesStableButtonBitsWhenAddingMoreButtons() {
        assertEquals(1, InputState.bitFor(InputButton.LEFT))
        assertEquals(1 shl 1, InputState.bitFor(InputButton.RIGHT))
        assertEquals(1 shl 2, InputState.bitFor(InputButton.UP))
        assertEquals(1 shl 3, InputState.bitFor(InputButton.DOWN))
        assertEquals(1 shl 4, InputState.bitFor(InputButton.A))
        assertEquals(1 shl 5, InputState.bitFor(InputButton.B))
        assertEquals(1 shl 6, InputState.bitFor(InputButton.START))
        assertEquals(1 shl 7, InputState.bitFor(InputButton.X))
        assertEquals(1 shl 8, InputState.bitFor(InputButton.Y))
        assertEquals(1 shl 9, InputState.bitFor(InputButton.L))
        assertEquals(1 shl 10, InputState.bitFor(InputButton.R))
        assertEquals(1 shl 11, InputState.bitFor(InputButton.SELECT))
    }
}
