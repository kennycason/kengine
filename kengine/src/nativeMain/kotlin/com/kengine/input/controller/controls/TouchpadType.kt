package com.kengine.input.controller.controls

/**
 * Extended types of inputs a controller can support
 */
enum class TouchpadType {
    DPAD,        // Touchpad emulating a D-pad
    STICK,       // Touchpad emulating an analog stick
    BUTTONS,     // Touchpad emulating buttons (like mouse clicks)
    SCROLL,      // Touchpad being used as scroll wheel
    MOUSE        // Touchpad being used as mouse/trackpad
}
