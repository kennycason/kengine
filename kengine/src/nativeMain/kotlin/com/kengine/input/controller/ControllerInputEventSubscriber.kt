package com.kengine.input.controller

import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapper
import com.kengine.input.controller.controls.ControllerMapping
import com.kengine.input.controller.controls.HatDirection
import com.kengine.log.Logging
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.useSDLEventContext
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import sdl3.SDL_EVENT_JOYSTICK_ADDED
import sdl3.SDL_EVENT_JOYSTICK_AXIS_MOTION
import sdl3.SDL_EVENT_JOYSTICK_BUTTON_DOWN
import sdl3.SDL_EVENT_JOYSTICK_BUTTON_UP
import sdl3.SDL_EVENT_JOYSTICK_HAT_MOTION
import sdl3.SDL_EVENT_JOYSTICK_REMOVED
import sdl3.SDL_Event
import sdl3.SDL_GetError
import sdl3.SDL_GetJoystickName
import sdl3.SDL_GetJoysticks
import sdl3.SDL_GetNumJoystickAxes
import sdl3.SDL_GetNumJoystickButtons
import sdl3.SDL_GetNumJoystickHats
import sdl3.SDL_OpenJoystick
import kotlin.math.abs

/**
 * Handles controller input events and maintains state for all connected controllers.
 * Provides both specific controller access and convenience methods for accessing
 * the first active controller.
 */
class ControllerInputEventSubscriber(
    val deadzone: Float = 0.5f
) : Logging {
    private val controllerStates = mutableMapOf<UInt, ControllerState>()
    private val controllerMappings = mutableMapOf<UInt, ControllerMapping>()


    /**
     * Gets the ID of the first connected controller, if any exist.
     * @return The first controller ID, or null if no controllers are connected
     */
    fun getFirstControllerId(): UInt? = controllerStates.keys.firstOrNull()

    data class ControllerState(
        var axes: FloatArray,
        var buttons: BooleanArray,
        var hatStates: IntArray  // Store raw hat values
    ) {
        // Helper function to check if a hat direction is pressed
        fun isHatDirectionPressed(hatIndex: Int, direction: HatDirection): Boolean {
            return if (hatIndex < hatStates.size) {
                val hatValue = hatStates[hatIndex]
                direction.isPressed(hatValue)
            } else false
        }
    }

    fun init() {
        useSDLEventContext {
            logger.info { "Subscribed to controller events" }
            subscribe(SDLEventContext.EventType.CONTROLLER, ::handleControllerEvent)
        }
        initControllers()
    }

    private fun handleControllerEvent(event: SDL_Event) {
        when (event.type) {
            SDL_EVENT_JOYSTICK_BUTTON_DOWN -> handleButtonEvent(event, true)
            SDL_EVENT_JOYSTICK_BUTTON_UP -> handleButtonEvent(event, false)
            SDL_EVENT_JOYSTICK_AXIS_MOTION -> handleAxisEvent(event)
            SDL_EVENT_JOYSTICK_HAT_MOTION -> handleHatEvent(event)
            SDL_EVENT_JOYSTICK_ADDED -> handleControllerAdded()
            SDL_EVENT_JOYSTICK_REMOVED -> handleControllerRemoved(event)
        }
    }

    private fun handleButtonEvent(event: SDL_Event, isPressed: Boolean) {
        val joystickID = event.jbutton.which
        val button = event.jbutton.button.toInt()
        if (logger.isDebugEnabled()) {
            logger.debug { "Button ${if (isPressed) "Down" else "Up"} - JoystickID: $joystickID, Button: $button" }
        }
        controllerStates[joystickID]?.buttons?.getOrNull(button)?.let {
            controllerStates[joystickID]?.buttons?.set(button, isPressed)
        }
    }

    private fun handleAxisEvent(event: SDL_Event) {
        val joystickID = event.jaxis.which
        val axis = event.jaxis.axis.toInt()
        val value = event.jaxis.value.toFloat() / 32767.0f

        // Apply deadzone threshold
        val adjustedValue = if (abs(value) < deadzone) 0f else value

        if (logger.isDebugEnabled()) {
            logger.debug { "Axis Motion - JoystickID: $joystickID, Axis: $axis, Value: $adjustedValue" }
        }

        controllerStates[joystickID]?.axes?.getOrNull(axis)?.let {
            controllerStates[joystickID]?.axes?.set(axis, adjustedValue)
        }
    }

    private fun handleHatEvent(event: SDL_Event) {
        val joystickID = event.jhat.which
        val hatID = event.jhat.hat.toInt()
        val hatValue = event.jhat.value.toInt()

        controllerStates[joystickID]?.let { state ->
            if (hatID < state.hatStates.size) {
                state.hatStates[hatID] = hatValue
                if (logger.isDebugEnabled()) {
                    logger.debug {
                        "Hat[$hatID] State: " +
                            "UP=${HatDirection.UP.isPressed(hatValue)}, " +
                            "RIGHT=${HatDirection.RIGHT.isPressed(hatValue)}, " +
                            "DOWN=${HatDirection.DOWN.isPressed(hatValue)}, " +
                            "LEFT=${HatDirection.LEFT.isPressed(hatValue)}"
                    }
                }
            }
        }
    }

    private fun handleControllerAdded() {
        logger.info { "Controller added" }
        initControllers()
    }

    private fun handleControllerRemoved(event: SDL_Event) {
        logger.info { "Controller removed" }
        val joystickID = event.jdevice.which
        controllerStates.remove(joystickID)
    }

    fun getAxisValue(controllerId: UInt, axisIndex: Int): Float {
        return controllerStates[controllerId]?.axes?.getOrNull(axisIndex) ?: 0.0f
    }

    fun getAxisValue(axisIndex: Int): Float {
        for (state in controllerStates.values) {
            val axisState = state.axes.getOrElse(axisIndex) { 0.0f }
            if (axisState != 0.0f) return axisState
        }
        return 0.0f
    }

    /**
     * Checks if a button is pressed on any connected controller
     */
    fun isButtonPressed(buttonIndex: Int): Boolean {
        for ((id, state) in controllerStates) {
            val mapping = controllerMappings[id]?.buttonMappings?.get(buttonIndex)

            when (mapping) {
                ButtonType.REGULAR -> if (state.buttons.getOrNull(buttonIndex) == true) return true
                ButtonType.HAT_UP -> if (state.isHatDirectionPressed(0, HatDirection.UP)) return true
                ButtonType.HAT_DOWN -> if (state.isHatDirectionPressed(0, HatDirection.DOWN)) return true
                ButtonType.HAT_LEFT -> if (state.isHatDirectionPressed(0, HatDirection.LEFT)) return true
                ButtonType.HAT_RIGHT -> if (state.isHatDirectionPressed(0, HatDirection.RIGHT)) return true
                null -> if (state.buttons.getOrNull(buttonIndex) == true) return true
            }
        }
        return false
    }

    fun isButtonPressed(controllerId: UInt, buttonIndex: Int): Boolean {
        return controllerStates[controllerId]?.buttons?.getOrNull(buttonIndex) ?: false
    }

    fun isButtonPressed(button: Buttons): Boolean {
        for ((id, state) in controllerStates) {
            val mapping = controllerMappings[id] ?: continue
            val buttonIndex = mapping.gamepadMappings[button] ?: continue
            if (isButtonPressed(buttonIndex)) return true
        }
        return false
    }

    /**
     * Checks if a hat direction is pressed on any connected controller
     */
    fun isHatDirectionPressed(
        hatIndex: Int,
        direction: HatDirection
    ): Boolean {
        for (state in controllerStates.values) {
            if (state.isHatDirectionPressed(hatIndex, direction)) return true
        }
        return false
    }

    fun isHatDirectionPressed(
        controllerId: UInt,
        hatIndex: Int,
        direction: HatDirection
    ): Boolean {
        return controllerStates[controllerId]?.isHatDirectionPressed(hatIndex, direction) ?: false
    }

    fun cleanup() {
        logger.info { "Cleaning up ControllerInputEventSubscriber" }
        controllerStates.clear()
    }

    private fun initControllers() {
        memScoped {
            val countPtr = alloc<IntVar>()
            val joysticks = SDL_GetJoysticks(countPtr.ptr)
            val count = countPtr.value
            logger.info { "Controllers found: $count" }

            joysticks?.let { joyArray ->
                for (i in 0 until count) {
                    val instanceId = joyArray[i]
                    SDL_OpenJoystick(instanceId)?.let { joystick ->
                        val name = SDL_GetJoystickName(joystick)?.toKString() ?: "Unknown Controller"
                        val mapping = ControllerMapper.getMapping(name)

                        logger.info { "Controller '$name' connected with mapping: ${mapping?.name ?: "None"}" }

                        controllerStates[instanceId] = ControllerState(
                            axes = FloatArray(SDL_GetNumJoystickAxes(joystick)),
                            buttons = BooleanArray(SDL_GetNumJoystickButtons(joystick)),
                            hatStates = IntArray(SDL_GetNumJoystickHats(joystick))
                        )

                        if (mapping != null) {
                            controllerMappings[instanceId] = mapping
                        }
                    } ?: run {
                        logger.warn { "Failed to open joystick $i: ${SDL_GetError()?.toKString()}" }
                    }
                }
            }
        }
    }
}
