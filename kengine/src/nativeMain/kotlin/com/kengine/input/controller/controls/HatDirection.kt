package com.kengine.input.controller.controls


/**
 * Handles controller input events and maintains state for all connected controllers.
 * Provides both specific controller access and convenience methods for accessing
 * the first active controller.
 */
enum class HatDirection(private val mask: Int) {
    UP(0x01),
    RIGHT(0x02),
    DOWN(0x04),
    LEFT(0x08);

    fun isPressed(hatValue: Int): Boolean = (hatValue and mask) != 0
}
