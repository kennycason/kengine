//package com.kengine.sdl
//import com.kengine.hooks.context.Context
//import com.kengine.log.Logging
//import kotlinx.cinterop.ExperimentalForeignApi
//import kotlinx.cinterop.alloc
//import kotlinx.cinterop.memScoped
//import kotlinx.cinterop.ptr
//import sdl2.SDL_Event
//import sdl2.SDL_JOYAXISMOTION
//import sdl2.SDL_JOYBALLMOTION
//import sdl2.SDL_JOYBUTTONDOWN
//import sdl2.SDL_JOYBUTTONUP
//import sdl2.SDL_JOYDEVICEADDED
//import sdl2.SDL_JOYDEVICEREMOVED
//import sdl2.SDL_JOYHATMOTION
//import sdl2.SDL_KEYDOWN
//import sdl2.SDL_KEYUP
//import sdl2.SDL_MOUSEBUTTONDOWN
//import sdl2.SDL_MOUSEBUTTONUP
//import sdl2.SDL_MOUSEMOTION
//import sdl2.SDL_PollEvent
//import sdl2.SDL_QUIT
//
//@OptIn(ExperimentalForeignApi::class)
//class SDLEventContext private constructor() : Context(), Logging {
//    private val events = mutableListOf<SDL_Event>()
//    private val subscribers = mutableMapOf<EventType, MutableList<(SDL_Event) -> Unit>>()
//
//    enum class EventType {
//        QUIT, KEYBOARD, MOUSE, CONTROLLER
//    }
//
//    companion object {
//        private var currentContext: SDLEventContext? = null
//
//        fun get(): SDLEventContext {
//            if (currentContext == null) {
//                currentContext = SDLEventContext()
//            }
//            return currentContext ?: throw IllegalStateException("Failed to create event context")
//        }
//    }
//
//    override fun cleanup() {
//        events.clear()
//        subscribers.clear()
//    }
//
//    /**
//     * Poll SDL events and notify subscribers
//     */
//    fun pollEvents() {
//        memScoped {
//            val event = alloc<SDL_Event>()
//            while (SDL_PollEvent(event.ptr) != 0) {
//                events.add(event)
//                notifySubscribers(event)
//            }
//        }
//    }
//
//    /**
//     * Subscribe to a specific event type
//     */
//    fun subscribe(eventType: EventType, handler: (SDL_Event) -> Unit) {
//        val handlers = subscribers.getOrPut(eventType) { mutableListOf() }
//        handlers.add(handler)
//    }
//
//    /**
//     * Notify all subscribers of a specific event type
//     */
//    private fun notifySubscribers(event: SDL_Event) {
//        val eventType = when (event.type) {
//            SDL_KEYDOWN,
//            SDL_KEYUP -> EventType.KEYBOARD
//            SDL_MOUSEBUTTONDOWN,
//            SDL_MOUSEBUTTONUP,
//            SDL_MOUSEMOTION -> EventType.MOUSE
//            SDL_JOYBUTTONDOWN,
//            SDL_JOYBUTTONUP,
//            SDL_JOYAXISMOTION,
//            SDL_JOYBALLMOTION,
//            SDL_JOYHATMOTION,
//            SDL_JOYDEVICEADDED,
//            SDL_JOYDEVICEREMOVED -> EventType.CONTROLLER
//            SDL_QUIT -> EventType.QUIT
//            else -> null
//        }
//
//        if (eventType != null) {
//            val handlers = subscribers[eventType]
//            if (!handlers.isNullOrEmpty()) {
//                handlers.forEach { handler ->
//                    try {
//                        handler(event)
//                    } catch (e: Exception) {
//                        logger.error { "Error handling event: ${e.message}" }
//                    }
//                }
//            } else {
//                logger.warn { "No subscribers for event type: $eventType" }
//            }
//        } else {
//            logger.debug { "Unsupported event type: ${event.type}" }
//        }
//    }
//
//    /**
//     * Clear all stored events
//     */
//    fun clearEvents() {
//        events.clear()
//    }
//
//}
