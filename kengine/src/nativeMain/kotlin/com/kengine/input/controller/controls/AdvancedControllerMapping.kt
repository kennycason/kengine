package com.kengine.input.controller.controls

/**
 * Extended controller mapping interface that defines additional input types
 * beyond standard buttons and axes
 */
interface AdvancedControllerMapping : ControllerMapping {
    // Maps touchpad IDs to their types
    val touchpadMappings: Map<Int, TouchpadType>

    // Maps trigger IDs to their corresponding axis indices
    val triggerMappings: Map<Int, Int>

    // Maps touchpad IDs to their corresponding axis indices (X and Y)
    val touchpadAxisMappings: Map<Int, Pair<Int, Int>>

    // Define which sides support haptic feedback
    val hapticSupportedSides: Set<Side>

    enum class Side {
        LEFT, RIGHT, BOTH
    }
}
