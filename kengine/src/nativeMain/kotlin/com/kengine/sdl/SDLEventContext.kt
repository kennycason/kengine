package com.kengine.sdl

import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl3.SDL_EVENT_GAMEPAD_ADDED
import sdl3.SDL_EVENT_GAMEPAD_AXIS_MOTION
import sdl3.SDL_EVENT_GAMEPAD_BUTTON_DOWN
import sdl3.SDL_EVENT_GAMEPAD_BUTTON_UP
import sdl3.SDL_EVENT_GAMEPAD_REMOVED
import sdl3.SDL_EVENT_KEY_DOWN
import sdl3.SDL_EVENT_KEY_UP
import sdl3.SDL_EVENT_MOUSE_BUTTON_DOWN
import sdl3.SDL_EVENT_MOUSE_BUTTON_UP
import sdl3.SDL_EVENT_MOUSE_MOTION
import sdl3.SDL_EVENT_QUIT
import sdl3.SDL_Event
import sdl3.SDL_PollEvent

@OptIn(ExperimentalForeignApi::class)
class SDLEventContext private constructor() : Context(), Logging {
    private val events = mutableListOf<SDL_Event>()
    private val subscribers = mutableMapOf<EventType, MutableList<(SDL_Event) -> Unit>>()

    enum class EventType {
        QUIT, KEYBOARD, MOUSE, CONTROLLER
    }

    companion object {
        private var currentContext: SDLEventContext? = null

        fun get(): SDLEventContext {
            return currentContext ?: SDLEventContext().also {
                currentContext = it
            }
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up SDLEventContext"}
        events.clear()
        subscribers.clear()
        currentContext = null
    }

    /**
     * Poll SDL events and notify subscribers
     */
    fun pollEvents() {
        memScoped {
            val event = alloc<SDL_Event>()
            while (SDL_PollEvent(event.ptr)) {
                events.add(event)
                notifySubscribers(event)
            }
        }
    }

    /**
     * Subscribe to a specific event type
     */
    fun subscribe(eventType: EventType, handler: (SDL_Event) -> Unit) {
        val handlers = subscribers.getOrPut(eventType) { mutableListOf() }
        handlers.add(handler)
    }

    /**
     * Notify all subscribers of a specific event type
     */
    private fun notifySubscribers(event: SDL_Event) {
        val eventType = when (event.type) {
            SDL_EVENT_KEY_DOWN,
            SDL_EVENT_KEY_UP -> EventType.KEYBOARD

            SDL_EVENT_MOUSE_BUTTON_DOWN,
            SDL_EVENT_MOUSE_BUTTON_UP,
            SDL_EVENT_MOUSE_MOTION -> EventType.MOUSE

            SDL_EVENT_GAMEPAD_BUTTON_DOWN,
            SDL_EVENT_GAMEPAD_BUTTON_UP,
            SDL_EVENT_GAMEPAD_AXIS_MOTION,
            SDL_EVENT_GAMEPAD_ADDED,
            SDL_EVENT_GAMEPAD_REMOVED -> EventType.CONTROLLER

            SDL_EVENT_QUIT -> EventType.QUIT
            else -> null
        }

        if (eventType != null) {
            val handlers = subscribers[eventType]
            if (!handlers.isNullOrEmpty()) {
                handlers.forEach { handler ->
                    try {
                        handler(event)
                    } catch (e: Exception) {
                        logger.error { "Error handling event: ${e.message}" }
                    }
                }
            } else {
                logger.warn { "No subscribers for event type: $eventType" }
            }
        } else {
            logger.debug { "Unsupported event type: ${event.type}" }
        }
    }

    /**
     * Clear all stored events
     */
    fun clearEvents() {
        events.clear()
    }

}
