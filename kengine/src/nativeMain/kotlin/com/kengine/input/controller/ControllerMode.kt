package com.kengine.input.controller

/**
 * Defines which SDL subsystem to use for controller input.
 * 
 * JOYSTICK: Uses SDL_INIT_JOYSTICK with custom per-controller mappings (SNES, PS5, Xbox, etc.)
 * GAMEPAD: Uses SDL_INIT_GAMEPAD with SDL's standardized button/axis layout
 */
enum class ControllerMode {
    JOYSTICK,
    GAMEPAD
}

