package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.AdvancedControllerMapping
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.TouchpadType

/**
 * Steam Controller mapping - Basic implementation
 * Note: Many advanced features (touchpads, haptics, mode-shifting)
 * are not supported by current interface
 */
object Steam : AdvancedControllerMapping {
    override val name = "Steam Controller"

    override fun isMatches(controllerName: String): Boolean {
        return controllerName.contains("Steam", ignoreCase = true) &&
            controllerName.contains("Controller", ignoreCase = true)
    }

    // Face Buttons (ABXY in Xbox layout)
    const val A = 0
    const val B = 1
    const val X = 2
    const val Y = 3

    // Special buttons
    const val BACK = 4
    const val START = 5
    const val STEAM = 6

    // Shoulder buttons
    const val LB = 7
    const val RB = 8

    // Grip buttons
    const val LEFT_GRIP = 9
    const val RIGHT_GRIP = 10

    // D-Pad (emulated by left touchpad)
    const val DPAD_UP = 100
    const val DPAD_DOWN = 101
    const val DPAD_LEFT = 102
    const val DPAD_RIGHT = 103

    // Touchpad identifiers
    const val LEFT_TOUCHPAD = 20
    const val RIGHT_TOUCHPAD = 21

    // Trigger identifiers
    const val LEFT_TRIGGER = 30
    const val RIGHT_TRIGGER = 31

    // Axes
    const val L_STICK_HORIZONTAL_AXIS = 0
    const val L_STICK_VERTICAL_AXIS = 1
    const val R_STICK_HORIZONTAL_AXIS = 2
    const val R_STICK_VERTICAL_AXIS = 3
    const val LEFT_TRIGGER_AXIS = 4
    const val RIGHT_TRIGGER_AXIS = 5

    // Touchpad axes (each touchpad has X and Y)
    const val LEFT_TOUCHPAD_X = 6
    const val LEFT_TOUCHPAD_Y = 7
    const val RIGHT_TOUCHPAD_X = 8
    const val RIGHT_TOUCHPAD_Y = 9

    override val buttonMappings = mapOf(
        A to ButtonType.REGULAR,
        B to ButtonType.REGULAR,
        X to ButtonType.REGULAR,
        Y to ButtonType.REGULAR,
        BACK to ButtonType.REGULAR,
        START to ButtonType.REGULAR,
        STEAM to ButtonType.REGULAR,
        LB to ButtonType.REGULAR,
        RB to ButtonType.REGULAR,
        LEFT_GRIP to ButtonType.REGULAR,
        RIGHT_GRIP to ButtonType.REGULAR,
        DPAD_UP to ButtonType.HAT_UP,
        DPAD_DOWN to ButtonType.HAT_DOWN,
        DPAD_LEFT to ButtonType.HAT_LEFT,
        DPAD_RIGHT to ButtonType.HAT_RIGHT
    )

    override val gamepadMappings = mapOf(
        Buttons.B to A,    // Xbox layout mapping
        Buttons.A to B,
        Buttons.Y to X,
        Buttons.X to Y,
        Buttons.SELECT to BACK,
        Buttons.START to START,
        Buttons.L1 to LB,
        Buttons.R1 to RB,
        Buttons.DPAD_UP to DPAD_UP,
        Buttons.DPAD_DOWN to DPAD_DOWN,
        Buttons.DPAD_LEFT to DPAD_LEFT,
        Buttons.DPAD_RIGHT to DPAD_RIGHT
    )

    override val touchpadMappings = mapOf(
        LEFT_TOUCHPAD to TouchpadType.DPAD,
        RIGHT_TOUCHPAD to TouchpadType.MOUSE
    )

    override val triggerMappings = mapOf(
        LEFT_TRIGGER to LEFT_TRIGGER_AXIS,
        RIGHT_TRIGGER to RIGHT_TRIGGER_AXIS
    )

    override val touchpadAxisMappings = mapOf(
        LEFT_TOUCHPAD to Pair(LEFT_TOUCHPAD_X, LEFT_TOUCHPAD_Y),
        RIGHT_TOUCHPAD to Pair(RIGHT_TOUCHPAD_X, RIGHT_TOUCHPAD_Y)
    )

    override val hapticSupportedSides = setOf(
        AdvancedControllerMapping.Side.LEFT,
        AdvancedControllerMapping.Side.RIGHT,
        AdvancedControllerMapping.Side.BOTH
    )
}
