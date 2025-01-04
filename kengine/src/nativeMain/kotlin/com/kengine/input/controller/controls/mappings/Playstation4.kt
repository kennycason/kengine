package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping

object Playstation4 : ControllerMapping {
    override val name = "PS4 Controller"

    override fun isMatches(controllerName: String): Boolean {
        return controllerName.contains("PS4", ignoreCase = true) ||
            controllerName.contains("DualShock", ignoreCase = true) ||
            controllerName.contains("PlayStation 4", ignoreCase = true)
    }

    // Face buttons
    const val X = 0
    const val O = 1
    const val SQUARE = 2
    const val TRIANGLE = 3

    // Special buttons
    const val SHARE = 4
    const val OPTIONS = 6

    // Stick buttons
    const val L3 = 7
    const val R3 = 8

    const val L2 = 4 // axis
    const val R2 = 5

    // Shoulder buttons
    const val L1 = 9
    const val R1 = 10

    // D-Pad (using same logical IDs as before)
    const val DPAD_UP = 100
    const val DPAD_DOWN = 101
    const val DPAD_LEFT = 102
    const val DPAD_RIGHT = 103

    // Axes
    const val L_STICK_HORIZONTAL_AXIS = 0
    const val L_STICK_VERTICAL_AXIS = 1
    const val R_STICK_HORIZONTAL_AXIS = 2
    const val R_STICK_VERTICAL_AXIS = 3
    private const val L2_AXIS = L2
    private const val R2_AXIS = R2

    override val buttonMappings = mapOf(
        // Face buttons
        X to ButtonType.REGULAR,
        O to ButtonType.REGULAR,
        SQUARE to ButtonType.REGULAR,
        TRIANGLE to ButtonType.REGULAR,

        // Special buttons
        SHARE to ButtonType.REGULAR,
        OPTIONS to ButtonType.REGULAR,

        // Stick buttons
        L3 to ButtonType.REGULAR,
        R3 to ButtonType.REGULAR,

        // Shoulder buttons
        L1 to ButtonType.REGULAR,
        R1 to ButtonType.REGULAR,

        L2 to ButtonType.REGULAR,
        R2 to ButtonType.REGULAR,

        // D-Pad
        DPAD_UP to ButtonType.HAT_UP,
        DPAD_DOWN to ButtonType.HAT_DOWN,
        DPAD_LEFT to ButtonType.HAT_LEFT,
        DPAD_RIGHT to ButtonType.HAT_RIGHT
    )

    override val gamepadMappings = mapOf(
        // Map both generic and PS-specific buttons
        Buttons.B to X,             // X button
        Buttons.A to O,             // O button
        Buttons.Y to SQUARE,        // Square button
        Buttons.X to TRIANGLE,      // Triangle button

        // PS-specific mappings
        Buttons.SQUARE to SQUARE,
        Buttons.TRIANGLE to TRIANGLE,
        Buttons.CIRCLE to O,

        Buttons.SELECT to SHARE,
        Buttons.START to OPTIONS,

        Buttons.L1 to L1,
        Buttons.R1 to R1,
        Buttons.L2 to L2,
        Buttons.R2 to R2,
        Buttons.L3 to L3,
        Buttons.R3 to R3,

        Buttons.DPAD_UP to DPAD_UP,
        Buttons.DPAD_DOWN to DPAD_DOWN,
        Buttons.DPAD_LEFT to DPAD_LEFT,
        Buttons.DPAD_RIGHT to DPAD_RIGHT
    )

    // Optional: Add axis mappings if you want to normalize/configure axis handling
    override val axisMappings = mapOf(
        L_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        L_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        R_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        R_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        L2_AXIS to AxisType.TRIGGER,
        R2_AXIS to AxisType.TRIGGER
    )
}
