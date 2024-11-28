package com.kengine

import com.kengine.action.ActionsContext
import com.kengine.context.Context
import com.kengine.context.ContextRegistry
import com.kengine.context.getContext
import com.kengine.event.EventContext
import com.kengine.geometry.GeometryContext
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.TextureContext
import com.kengine.input.KeyboardContext
import com.kengine.input.MouseContext
import com.kengine.log.Logger
import com.kengine.sdl.SDLContext
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.registerSDLQuitHandler
import com.kengine.sound.SoundContext
import com.kengine.time.ClockContext

class GameContext private constructor(
    val sdl: SDLContext,
    val sdlEvents: SDLEventContext,
    val events: EventContext,
    val keyboard: KeyboardContext,
    val mouse: MouseContext,
    val textures: TextureContext,
    val sprites: SpriteContext,
    val geometry: GeometryContext,
    val sounds: SoundContext,
    val actions: ActionsContext,
    val clock: ClockContext,
) : Context() {
    var isRunning = true

    init {
        registerSDLQuitHandler()
        ContextRegistry.register(sdl)
        ContextRegistry.register(sdlEvents)
        ContextRegistry.register(events)
        ContextRegistry.register(keyboard)
        ContextRegistry.register(mouse)
        ContextRegistry.register(textures)
        ContextRegistry.register(sprites)
        ContextRegistry.register(geometry)
        ContextRegistry.register(sounds)
        ContextRegistry.register(actions)
        ContextRegistry.register(clock)
        getContext<KeyboardContext>().keyboard.init()
        getContext<MouseContext>().mouse.init()
    }

    fun registerContext(context: Context) {
        ContextRegistry.register(context)
    }

    companion object {
        private var currentContext: GameContext? = null

        fun create(
            title: String,
            width: Int,
            height: Int
        ): GameContext {
            if (currentContext != null) {
                throw IllegalStateException("SDLContext has already been created. Call cleanup() before creating a new context.")
            }
            // TODO order matters, i.e. there are dependencies
            currentContext = GameContext(
                sdl = SDLContext.create(title, width, height),
                events = EventContext.get(),
                sdlEvents = SDLEventContext.get(),
                keyboard = KeyboardContext.get(),
                mouse = MouseContext.get(),
                textures = TextureContext.get(),
                sprites = SpriteContext.get(),
                sounds = SoundContext.get(),
                geometry = GeometryContext.get(),
                actions = ActionsContext.get(),
                clock = ClockContext.get()
            )
            return currentContext!!
        }

        fun get(): GameContext {
            return currentContext ?: throw IllegalStateException("AppContext has not been created. Call create() first.")
        }
    }

    override fun cleanup() {
        Logger.info { "Cleaning up game resources" }
        actions.cleanup()
        sdlEvents.cleanup()
        events.cleanup()
        keyboard.cleanup()
        mouse.cleanup()
        textures.cleanup()
        sprites.cleanup()
        geometry.cleanup()
        sounds.cleanup()
        sdl.cleanup()
        clock.cleanup()
        ContextRegistry.clearAll()
    }

}


