package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping

/**
 * Ouya controller mapping for the Android-based console
 * Notable for its touchpad in the center and unique O-U-Y-A button layout
 */
object Ouya : ControllerMapping {
    override val name = "OUYA"

    override fun isMatches(controllerName: String): Boolean {
        return controllerName.contains("OUYA", ignoreCase = true) ||
               controllerName.contains("Ouya", ignoreCase = true)
    }

    // Face buttons - follows OUYA's unique O-U-Y-A layout
    const val O = 0      // Bottom button
    const val U = 1      // Right button
    const val Y = 2      // Top button
    const val A = 3      // Left button

    // Shoulder buttons (L1/R1 only, no L2/R2)
    const val L1 = 4
    const val R1 = 5

    // System button (center touchpad click)
    const val SYSTEM = 6

    // Stick clicks
    const val L3 = 7
    const val R3 = 8

    // D-Pad using standard logical IDs
    const val DPAD_UP = 100
    const val DPAD_DOWN = 101
    const val DPAD_LEFT = 102
    const val DPAD_RIGHT = 103

    // Standard axes for sticks
    const val L_STICK_HORIZONTAL_AXIS = 0
    const val L_STICK_VERTICAL_AXIS = 1
    const val R_STICK_HORIZONTAL_AXIS = 2
    const val R_STICK_VERTICAL_AXIS = 3

    override val buttonMappings = mapOf(
        O to ButtonType.REGULAR,
        U to ButtonType.REGULAR,
        Y to ButtonType.REGULAR,
        A to ButtonType.REGULAR,
        L1 to ButtonType.REGULAR,
        R1 to ButtonType.REGULAR,
        SYSTEM to ButtonType.REGULAR,
        L3 to ButtonType.REGULAR,
        R3 to ButtonType.REGULAR,
        DPAD_UP to ButtonType.HAT_UP,
        DPAD_DOWN to ButtonType.HAT_DOWN,
        DPAD_LEFT to ButtonType.HAT_LEFT,
        DPAD_RIGHT to ButtonType.HAT_RIGHT
    )

    override val gamepadMappings = mapOf(
        Buttons.B to O,       // Maps OUYA's O to our B
        Buttons.A to A,       // Maps OUYA's A to our A
        Buttons.X to U,       // Maps OUYA's U to our X
        Buttons.Y to Y,       // Maps OUYA's Y to our Y
        Buttons.L1 to L1,
        Buttons.R1 to R1,
        Buttons.L3 to L3,
        Buttons.R3 to R3,
        Buttons.START to SYSTEM,  // OUYA uses center button as menu
        Buttons.DPAD_UP to DPAD_UP,
        Buttons.DPAD_DOWN to DPAD_DOWN,
        Buttons.DPAD_LEFT to DPAD_LEFT,
        Buttons.DPAD_RIGHT to DPAD_RIGHT
    )

    override val axisMappings: Map<Int, AxisType> = linkedMapOf()
}
