package com.kengine.input.controller

import com.kengine.log.Logging
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import sdl2.SDL_Event
import sdl2.SDL_GetError
import sdl2.SDL_JOYAXISMOTION
import sdl2.SDL_JOYBALLMOTION
import sdl2.SDL_JOYBUTTONDOWN
import sdl2.SDL_JOYBUTTONUP
import sdl2.SDL_JOYDEVICEADDED
import sdl2.SDL_JOYDEVICEREMOVED
import sdl2.SDL_JOYHATMOTION
import sdl2.SDL_JoystickName
import sdl2.SDL_JoystickNumAxes
import sdl2.SDL_JoystickNumButtons
import sdl2.SDL_JoystickOpen
import sdl2.SDL_NumJoysticks

@OptIn(ExperimentalForeignApi::class)
class ControllerInputEventSubscriber : Logging {
    private val buttonStates = mutableMapOf<Int, ControllerState>()

    data class ControllerState(
        var axes: FloatArray,
        var buttons: BooleanArray
    )

    fun init() {
        useSDLContext {
            sdlEvents.subscribe(SDLEventContext.EventType.CONTROLLER, ::handleControllerEvent)
        }
        logger.info { "Controllers found: ${SDL_NumJoysticks()}" }
        for (i in 0 until SDL_NumJoysticks()) {
            val joystick = SDL_JoystickOpen(i)
            if (joystick != null) {
                println("Controller ${SDL_JoystickName(joystick)?.toKString()} connected.")
                buttonStates[i] = ControllerState(
                    axes = FloatArray(SDL_JoystickNumAxes(joystick)),
                    buttons = BooleanArray(SDL_JoystickNumButtons(joystick))
                )
            } else {
                logger.warn { "Failed to open joystick $i: ${SDL_GetError()!!.toKString()}" }
            }
        }
    }

    private fun handleControllerEvent(event: SDL_Event) {
        when (event.type) {
            SDL_JOYBUTTONDOWN -> {
                val joystickID = event.jbutton.which
                val button = event.jbutton.button
                buttonStates[joystickID]?.buttons?.set(button.toInt(), true)
            }
            SDL_JOYBUTTONUP -> {
                val joystickID = event.jbutton.which
                val button = event.jbutton.button
                buttonStates[joystickID]?.buttons?.set(button.toInt(), false)
            }
            SDL_JOYAXISMOTION -> {
                val joystickID = event.jaxis.which
                val axis = event.jaxis.axis
                val value = event.jaxis.value / 32767.0f // Normalize to -1.0 to 1.0
                buttonStates[joystickID]?.axes?.set(axis.toInt(), value)
            }
            SDL_JOYBALLMOTION,
            SDL_JOYHATMOTION,
            SDL_JOYDEVICEADDED,
            SDL_JOYDEVICEREMOVED -> {
                logger.info { "Unhandled controller events ${event.type}" }
            }
        }
    }

    fun getAxisValue(controllerId: Int, axisIndex: Int): Float {
        return buttonStates[controllerId]?.axes?.get(axisIndex) ?: 0.0f
    }

    fun getAxisValue(axisIndex: Int): Float {
        for (controllerButtonStates in buttonStates.entries) {
            val axisState = controllerButtonStates.value.axes.getOrElse(axisIndex) { 0.0f }
            if (axisState != 0.0f) return axisState
        }
        return 0.0f
    }

    fun isButtonPressed(controllerId: Int, buttonIndex: Int): Boolean {
        return buttonStates[controllerId]?.buttons?.get(buttonIndex) ?: false
    }

    fun isButtonPressed(buttonIndex: Int): Boolean {
        for (controllerButtonStates in buttonStates.entries) {
            if (controllerButtonStates.value.buttons.getOrElse(buttonIndex) { false }) return true
        }
        return false
    }

}