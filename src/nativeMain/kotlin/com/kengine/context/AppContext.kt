package com.kengine.context

class AppContext private constructor(
    val sdl: SDLContext,
    val keyboard: KeyboardContext
) : Context() {

    companion object {
        private var currentContext: AppContext? = null

        fun create(
            title: String,
            width: Int,
            height: Int
        ): AppContext {
            if (currentContext != null) {
                throw IllegalStateException("SDLContext has already been created. Call cleanup() before creating a new context.")
            }

            currentContext = AppContext(
                sdl = SDLContext.create(title, width, height),
                keyboard = KeyboardContext.get()
            )
            return currentContext!!
        }

        fun get(): AppContext {
            return currentContext ?: throw IllegalStateException("AppContext has not been created. Call create() first.")
        }

    }

    override fun cleanup() {
        sdl.cleanup()
        keyboard.cleanup()
    }
}