package com.kengine.input.controller

import com.kengine.log.Logging
import platform.posix.getenv
import kotlinx.cinterop.toKString

/**
 * Configuration for controller input system.
 * 
 * Reads from environment variables:
 * - KENGINE_CONTROLLER_MODE: "joystick" (default) or "gamepad"
 */
object ControllerConfig : Logging {
    
    /**
     * The active controller mode.
     * Defaults to JOYSTICK for backward compatibility with existing custom mappings.
     */
    val mode: ControllerMode by lazy {
        val envValue = getenv("KENGINE_CONTROLLER_MODE")?.toKString()?.lowercase()
        
        val selectedMode = when (envValue) {
            "gamepad" -> ControllerMode.GAMEPAD
            "joystick", null -> ControllerMode.JOYSTICK
            else -> {
                logger.warn { "Unknown KENGINE_CONTROLLER_MODE value '$envValue', defaulting to JOYSTICK" }
                ControllerMode.JOYSTICK
            }
        }
        
        logger.info { "Controller mode: $selectedMode" }
        selectedMode
    }
}

