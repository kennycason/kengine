package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping
import com.kengine.log.Logging

/**
 * SDL3 Gamepad API standardized mapping.
 * 
 * This mapping uses SDL3's built-in gamepad layer which normalizes all controllers
 * to a consistent button/axis layout (Xbox-style). SDL handles the hardware-specific
 * mapping internally, so we just need to map Kengine's button enum to SDL's constants.
 * 
 * This mapping is only used when ControllerMode.GAMEPAD is active.
 */
object SDLGamepad : ControllerMapping, Logging {
    override val name = "SDL Gamepad (Standardized)"

    override fun isMatches(controllerName: String): Boolean {
        // In gamepad mode, all controllers use this mapping
        // SDL handles hardware-specific mappings internally
        return true
    }

    // SDL3 Gamepad button indices (from SDL_GamepadButton enum)
    // These are the standardized button IDs SDL uses internally
    // Values verified via ControllerMappingIT test with SNES controller
    const val BUTTON_SOUTH = 1          // A on Xbox, Cross on PlayStation (A button - bottom face)
    const val BUTTON_EAST = 0           // B on Xbox, Circle on PlayStation (B button - right face)
    const val BUTTON_WEST = 3           // X on Xbox, Square on PlayStation (X button - left face)
    const val BUTTON_NORTH = 2          // Y on Xbox, Triangle on PlayStation (Y button - top face)
    const val BUTTON_BACK = 4           // Back/Select/Share
    const val BUTTON_GUIDE = 5          // Home/Guide/PS button
    const val BUTTON_START = 6          // Start/Options
    const val BUTTON_LEFT_STICK = 7     // L3
    const val BUTTON_RIGHT_STICK = 8    // R3
    const val BUTTON_LEFT_SHOULDER = 9  // L1/LB
    const val BUTTON_RIGHT_SHOULDER = 10 // R1/RB
    const val BUTTON_DPAD_UP = 11
    const val BUTTON_DPAD_DOWN = 12
    const val BUTTON_DPAD_LEFT = 13
    const val BUTTON_DPAD_RIGHT = 14
    const val BUTTON_MISC1 = 15         // Share/Capture button on modern controllers

    // SDL3 Gamepad axis indices (from SDL_GamepadAxis enum)
    const val AXIS_LEFTX = 0
    const val AXIS_LEFTY = 1
    const val AXIS_RIGHTX = 2
    const val AXIS_RIGHTY = 3
    const val AXIS_LEFT_TRIGGER = 4     // L2/LT
    const val AXIS_RIGHT_TRIGGER = 5    // R2/RT

    override val buttonMappings = mapOf(
        BUTTON_SOUTH to ButtonType.REGULAR,
        BUTTON_EAST to ButtonType.REGULAR,
        BUTTON_WEST to ButtonType.REGULAR,
        BUTTON_NORTH to ButtonType.REGULAR,
        BUTTON_BACK to ButtonType.REGULAR,
        BUTTON_GUIDE to ButtonType.REGULAR,
        BUTTON_START to ButtonType.REGULAR,
        BUTTON_LEFT_STICK to ButtonType.REGULAR,
        BUTTON_RIGHT_STICK to ButtonType.REGULAR,
        BUTTON_LEFT_SHOULDER to ButtonType.REGULAR,
        BUTTON_RIGHT_SHOULDER to ButtonType.REGULAR,
        BUTTON_DPAD_UP to ButtonType.REGULAR,
        BUTTON_DPAD_DOWN to ButtonType.REGULAR,
        BUTTON_DPAD_LEFT to ButtonType.REGULAR,
        BUTTON_DPAD_RIGHT to ButtonType.REGULAR,
        BUTTON_MISC1 to ButtonType.REGULAR
    )

    override val gamepadMappings: Map<Buttons, Int> = mapOf(
        // Map Kengine's generic button names to SDL's standardized layout
        Buttons.A to BUTTON_SOUTH,
        Buttons.B to BUTTON_EAST,
        Buttons.X to BUTTON_WEST,
        Buttons.Y to BUTTON_NORTH,
        Buttons.L1 to BUTTON_LEFT_SHOULDER,
        Buttons.R1 to BUTTON_RIGHT_SHOULDER,
        Buttons.L3 to BUTTON_LEFT_STICK,
        Buttons.R3 to BUTTON_RIGHT_STICK,
        Buttons.SELECT to BUTTON_BACK,
        Buttons.START to BUTTON_START,
        Buttons.DPAD_UP to BUTTON_DPAD_UP,
        Buttons.DPAD_DOWN to BUTTON_DPAD_DOWN,
        Buttons.DPAD_LEFT to BUTTON_DPAD_LEFT,
        Buttons.DPAD_RIGHT to BUTTON_DPAD_RIGHT
    )

    override val axisMappings: Map<Int, AxisType> = mapOf(
        AXIS_LEFTX to AxisType.STICK_X,
        AXIS_LEFTY to AxisType.STICK_Y,
        AXIS_RIGHTX to AxisType.STICK_X,
        AXIS_RIGHTY to AxisType.STICK_Y,
        AXIS_LEFT_TRIGGER to AxisType.TRIGGER,
        AXIS_RIGHT_TRIGGER to AxisType.TRIGGER
    )
}

