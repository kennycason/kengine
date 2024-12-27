package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping

/**
 * Mappings for Logitech F310
 */
object Logitech : ControllerMapping {
    override val name = "Logitech"

    override fun isMatches(controllerName: String): Boolean {
        return controllerName.contains("Logitech", ignoreCase = true)
    }

    // Face buttons - follows Xbox labeling but Nintendo positioning
    const val A = 0
    const val B = 1
    const val X = 2
    const val Y = 3

    // Shoulder buttons and special buttons
    const val LB = 4
    const val RB = 5
    const val BACK = 6
    const val START = 7
    const val GUIDE = 8
    const val L3 = 9
    const val R3 = 10

    // D-Pad using standard logical IDs
    const val DPAD_UP = 100
    const val DPAD_DOWN = 101
    const val DPAD_LEFT = 102
    const val DPAD_RIGHT = 103

    // Standard axis layout matching Xbox style
    const val L_STICK_HORIZONTAL_AXIS = 0
    const val L_STICK_VERTICAL_AXIS = 1
    const val R_STICK_HORIZONTAL_AXIS = 2
    const val R_STICK_VERTICAL_AXIS = 3
    const val LT_AXIS = 4
    const val RT_AXIS = 5

    override val buttonMappings = mapOf(
        A to ButtonType.REGULAR,
        B to ButtonType.REGULAR,
        X to ButtonType.REGULAR,
        Y to ButtonType.REGULAR,
        BACK to ButtonType.REGULAR,
        START to ButtonType.REGULAR,
        GUIDE to ButtonType.REGULAR,
        L3 to ButtonType.REGULAR,
        R3 to ButtonType.REGULAR,
        LB to ButtonType.REGULAR,
        RB to ButtonType.REGULAR,
        DPAD_UP to ButtonType.HAT_UP,
        DPAD_DOWN to ButtonType.HAT_DOWN,
        DPAD_LEFT to ButtonType.HAT_LEFT,
        DPAD_RIGHT to ButtonType.HAT_RIGHT
    )

    override val gamepadMappings = mapOf(
        Buttons.B to A,
        Buttons.A to B,
        Buttons.Y to X,
        Buttons.X to Y,
        Buttons.SELECT to BACK,
        Buttons.START to START,
        Buttons.L1 to LB,
        Buttons.R1 to RB,
        Buttons.L3 to L3,
        Buttons.R3 to R3,
        Buttons.DPAD_UP to DPAD_UP,
        Buttons.DPAD_DOWN to DPAD_DOWN,
        Buttons.DPAD_LEFT to DPAD_LEFT,
        Buttons.DPAD_RIGHT to DPAD_RIGHT
    )
}
