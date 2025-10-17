package com.kengine.input.controller

import com.kengine.hooks.context.Context
import com.kengine.log.getLogger
import kotlinx.cinterop.toKString
import sdl3.SDL_GetError
import sdl3.SDL_INIT_GAMEPAD
import sdl3.SDL_INIT_JOYSTICK
import sdl3.SDL_Init

class ControllerContext private constructor(
    val controller: ControllerInputEventSubscriber,
    private val mode: ControllerMode
) : Context() {

    init {
        // Init appropriate SDL subsystem based on mode
        // Note: SDL_INIT_GAMEPAD automatically initializes SDL_INIT_JOYSTICK as well
        val subsystem = when (mode) {
            ControllerMode.GAMEPAD -> SDL_INIT_GAMEPAD
            ControllerMode.JOYSTICK -> SDL_INIT_JOYSTICK
        }
        
        if (!SDL_Init(subsystem)) {
            logger.error { "SDL_Init Error: ${SDL_GetError()!!.toKString()}" }
        } else {
            logger.info { "Controller subsystem initialized: $mode" }
        }
    }

    companion object {
        private val logger = getLogger(ControllerContext::class)
        private var currentContext: ControllerContext? = null


        fun get(): ControllerContext {
            return currentContext ?: run {
                val mode = ControllerConfig.mode
                ControllerContext(
                    controller = ControllerInputEventSubscriber(mode),
                    mode = mode
                ).also {
                    currentContext = it
                }
            }
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up ControllerContext"}
        controller.cleanup()
        currentContext = null
    }

    fun init() {
        controller.init()
    }
}
