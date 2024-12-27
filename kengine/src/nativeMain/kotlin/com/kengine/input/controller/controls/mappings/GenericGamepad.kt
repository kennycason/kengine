package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping
import com.kengine.log.Logging

/**
 * Generic fallback mapping for unknown controllers
 * Assumes a basic layout with 4 face buttons, shoulders, and d-pad
 * Should be the last mapping checked as it will match any controller
 */
object GenericGamepad : ControllerMapping, Logging {
    override val name = "Generic Gamepad"

    override fun isMatches(controllerName: String): Boolean {
        logger.info { "Falling back to generic gamepad for controller name: $controllerName"}
        // This should be the last controller checked, as a fallback
        return true
    }

    // Generic numbered buttons instead of specific labels
    const val BUTTON_1 = 0  // Usually primary face button
    const val BUTTON_2 = 1  // Usually secondary face button
    const val BUTTON_3 = 2  // Usually third face button
    const val BUTTON_4 = 3  // Usually fourth face button
    const val L1 = 4        // Left shoulder
    const val R1 = 5        // Right shoulder
    const val SELECT = 6    // Select/Back/Share equivalent
    const val START = 7     // Start/Options/Menu equivalent
    const val L3 = 8        // Left stick click if present
    const val R3 = 9        // Right stick click if present

    // D-Pad using standard logical IDs
    const val DPAD_UP = 100
    const val DPAD_DOWN = 101
    const val DPAD_LEFT = 102
    const val DPAD_RIGHT = 103

    override val buttonMappings = mapOf(
        BUTTON_1 to ButtonType.REGULAR,
        BUTTON_2 to ButtonType.REGULAR,
        BUTTON_3 to ButtonType.REGULAR,
        BUTTON_4 to ButtonType.REGULAR,
        L1 to ButtonType.REGULAR,
        R1 to ButtonType.REGULAR,
        SELECT to ButtonType.REGULAR,
        START to ButtonType.REGULAR,
        L3 to ButtonType.REGULAR,
        R3 to ButtonType.REGULAR,
        DPAD_UP to ButtonType.HAT_UP,
        DPAD_DOWN to ButtonType.HAT_DOWN,
        DPAD_LEFT to ButtonType.HAT_LEFT,
        DPAD_RIGHT to ButtonType.HAT_RIGHT
    )

    override val gamepadMappings = mapOf(
        Buttons.B to BUTTON_1,
        Buttons.A to BUTTON_2,
        Buttons.Y to BUTTON_3,
        Buttons.X to BUTTON_4,
        Buttons.L1 to L1,
        Buttons.R1 to R1,
        Buttons.SELECT to SELECT,
        Buttons.START to START,
        Buttons.L3 to L3,
        Buttons.R3 to R3,
        Buttons.DPAD_UP to DPAD_UP,
        Buttons.DPAD_DOWN to DPAD_DOWN,
        Buttons.DPAD_LEFT to DPAD_LEFT,
        Buttons.DPAD_RIGHT to DPAD_RIGHT
    )
}
