
import com.kengine.context.Context
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl2.SDL_Event
import sdl2.SDL_KEYDOWN
import sdl2.SDL_KEYUP
import sdl2.SDL_MOUSEBUTTONDOWN
import sdl2.SDL_MOUSEBUTTONUP
import sdl2.SDL_MOUSEMOTION
import sdl2.SDL_PollEvent
import sdl2.SDL_QUIT

class EventContext private constructor() : Context() {
    private val events = mutableListOf<SDL_Event>()
    private val subscribers = mutableMapOf<EventType, MutableList<(SDL_Event) -> Unit>>()

    enum class EventType {
        KEYBOARD, QUIT, MOUSE
    }

    companion object {
        private var currentContext: EventContext? = null

        fun get(): EventContext {
            if (currentContext == null) {
                currentContext = EventContext()
            }
            return currentContext ?: throw IllegalStateException("Failed to create event context")
        }
    }

    override fun cleanup() {
        events.clear()
        subscribers.clear()
    }

    /**
     * Poll SDL events and notify subscribers
     */
    fun pollEvents() {
        memScoped {
            val event = alloc<SDL_Event>()
            while (SDL_PollEvent(event.ptr) != 0) {
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
            SDL_KEYDOWN,
            SDL_KEYUP -> EventType.KEYBOARD
            SDL_MOUSEBUTTONDOWN,
            SDL_MOUSEBUTTONUP,
            SDL_MOUSEMOTION -> EventType.MOUSE
            SDL_QUIT -> EventType.QUIT
            else -> null
        }

        if (eventType != null) {
            val handlers = subscribers[eventType]
            if (!handlers.isNullOrEmpty()) {
                handlers.forEach { handler ->
                    try {
                        handler(event)
                    } catch (e: Exception) {
                        println("Error handling event: ${e.message}")
                    }
                }
            } else {
                println("No subscribers for event type: $eventType")
            }
        } else {
            println("Unsupported event type: ${event.type}")
        }
    }

    /**
     * Clear all stored events
     */
    fun clearEvents() {
        events.clear()
    }

}