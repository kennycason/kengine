package com.kengine.event

import com.kengine.hooks.context.Context
import com.kengine.log.Logging


class EventContext : Context(), Logging {
    private val subscribers = mutableMapOf<String, MutableList<(Any) -> Unit>>()

    fun <M> subscribe(eventType: String, handler: (M) -> Unit) {
        val handlers = subscribers.getOrPut(eventType) { mutableListOf() }
        handlers.add { message ->
            try {
                @Suppress("UNCHECKED_CAST")
                handler(message as M)
            } catch (e: ClassCastException) {
                logger.error { "Failed to cast message: ${e.message}" }
                logger.debug(e)
            }
        }
    }

    fun <M : Any> publish(eventType: String, message: M) {
        subscribers[eventType]?.forEach { handler ->
            handler(message)
        }
    }

    fun clearSubscribers(eventType: String) {
        subscribers.remove(eventType)
    }

    fun clearAll() {
        subscribers.clear()
    }

    override fun cleanup() {
        logger.info { "Cleaning up EventContext"}
        clearAll()
        currentContext = null
    }

    companion object {
        private var currentContext: EventContext? = null

        fun get(): EventContext {
            if (currentContext == null) {
                currentContext = EventContext()

            }
            return currentContext ?: throw IllegalStateException("Failed to create EventContext")
        }
    }

}
