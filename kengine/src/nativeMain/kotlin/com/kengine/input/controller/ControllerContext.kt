package com.kengine.input.controller

import com.kengine.hooks.context.Context
import com.kengine.log.getLogger
import kotlinx.cinterop.toKString
import sdl3.SDL_GetError
import sdl3.SDL_INIT_JOYSTICK
import sdl3.SDL_Init

class ControllerContext private constructor(
    val controller: ControllerInputEventSubscriber
) : Context() {

    init {
        if (!SDL_Init(SDL_INIT_JOYSTICK)) {
            logger.error { "SDL_Init Error: ${SDL_GetError()!!.toKString()}" }
        }
    }

    companion object {
        private val logger = getLogger(ControllerContext::class)
        private var currentContext: ControllerContext? = null


        fun get(): ControllerContext {
            return currentContext ?:  ControllerContext(
                controller = ControllerInputEventSubscriber()
            ).also {
                currentContext = it
            }
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up ControllerContext"}
        currentContext = null
    }

    fun init() {
        controller.init()
    }
}
