package com.kengine.event

import com.kengine.context.Context
import com.kengine.log.Logger



class EventContext : Context() {
    private data class Event<M>(
        val type: String, // TODO I tried <T : Enum<T>, M>, but clean instantiation of context without a defined T became difficult
        val message: M
    )

    private val subscribers = mutableMapOf<String, MutableList<(Any) -> Unit>>()

    companion object {
        private var currentContext: EventContext? = null

        fun get(): EventContext {
            if (currentContext == null) {
                currentContext = EventContext()

            }
            return currentContext ?: throw IllegalStateException("Failed to create EventContext")
        }
    }

    fun <M> subscribe(eventType: String, handler: (M) -> Unit) {
        val handlers = subscribers.getOrPut(eventType) { mutableListOf() }
        handlers.add { message ->
            try {
                @Suppress("UNCHECKED_CAST")
                handler(message as M)
            } catch (e: ClassCastException) {
                Logger.error { "Failed to cast message: ${e.message}" }
                Logger.debug(e)
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
        clearAll()
    }

}