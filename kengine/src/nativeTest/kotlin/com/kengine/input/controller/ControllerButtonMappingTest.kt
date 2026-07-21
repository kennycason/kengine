package com.kengine.input.controller

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping
import com.kengine.input.controller.controls.mappings.NintendoSwitch
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControllerButtonMappingTest {
    @Test
    fun usesPrimaryButtonMapping() {
        val state = controllerState(buttons = booleanArrayOf(true, false))

        assertTrue(isMappedButtonPressed(Buttons.B, state, TestMapping))
    }

    @Test
    fun usesAlternateButtonMapping() {
        val state = controllerState(
            buttons = BooleanArray(8).also { it[7] = true }
        )

        assertTrue(isMappedButtonPressed(Buttons.L2, state, TestMapping))
    }

    @Test
    fun ignoresAlternateButtonMappingWhenButtonIsReleased() {
        val state = controllerState(buttons = BooleanArray(8))

        assertFalse(isMappedButtonPressed(Buttons.L2, state, TestMapping))
    }

    @Test
    fun usesPositiveTriggerAxisAsButtonMapping() {
        val state = controllerState(
            axes = FloatArray(5).also { it[4] = 0.72f },
            buttons = BooleanArray(8)
        )

        assertTrue(isMappedButtonPressed(Buttons.L2, state, TestMapping))
    }

    @Test
    fun ignoresNegativeRestingTriggerAxisAsButtonMapping() {
        val state = controllerState(
            axes = FloatArray(5).also { it[4] = -1.0f },
            buttons = BooleanArray(8)
        )

        assertFalse(isMappedButtonPressed(Buttons.L2, state, TestMapping))
    }

    @Test
    fun nintendoSwitchMapsZlAlternateToButtonSeven() {
        val state = controllerState(
            buttons = BooleanArray(8).also { it[NintendoSwitch.ZL_BUTTON] = true }
        )

        assertTrue(isMappedButtonPressed(Buttons.L2, state, NintendoSwitch))
    }

    private fun controllerState(
        axes: FloatArray = FloatArray(0),
        buttons: BooleanArray
    ): ControllerInputEventSubscriber.ControllerState {
        return ControllerInputEventSubscriber.ControllerState(
            axes = axes,
            buttons = buttons,
            hatStates = IntArray(0)
        )
    }

    private object TestMapping : ControllerMapping {
        override val name: String = "Test"
        override fun isMatches(controllerName: String): Boolean = false
        override val buttonMappings: Map<Int, ButtonType> = mapOf(
            0 to ButtonType.REGULAR,
            4 to ButtonType.REGULAR,
            7 to ButtonType.REGULAR
        )
        override val gamepadMappings: Map<Buttons, Int> = mapOf(
            Buttons.B to 0,
            Buttons.L2 to 4
        )
        override val alternateGamepadMappings: Map<Buttons, List<Int>> = mapOf(
            Buttons.L2 to listOf(7)
        )
        override val buttonAxisMappings: Map<Buttons, List<Int>> = mapOf(
            Buttons.L2 to listOf(4)
        )
        override val axisMappings: Map<Int, AxisType> = mapOf(
            4 to AxisType.TRIGGER
        )
    }
}
