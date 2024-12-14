package com.kengine.input.controller

import com.kengine.hooks.context.Context
import com.kengine.log.getLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import sdl2.SDL_GetError
import sdl2.SDL_INIT_JOYSTICK
import sdl2.SDL_Init

class ControllerContext private constructor(
    val controller: ControllerInputEventSubscriber
) : Context() {

    companion object {
        private val logger = getLogger(ControllerContext::class)
        private var currentContext: ControllerContext? = null

        @OptIn(ExperimentalForeignApi::class)
        fun get(): ControllerContext {
            if (currentContext == null) {
                currentContext = ControllerContext(
                    controller = ControllerInputEventSubscriber()
                )

                if (SDL_Init(SDL_INIT_JOYSTICK) != 0) {
                    logger.error { "SDL_Init Error: ${SDL_GetError()!!.toKString()}" }
                }
            }
            return currentContext ?: throw IllegalStateException("Failed to create controller context")
        }
    }

    override fun cleanup() {
    }

    fun init() {
        controller.init()
    }
}