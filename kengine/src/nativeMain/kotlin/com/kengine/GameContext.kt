package com.kengine

import com.kengine.action.ActionsContext
import com.kengine.context.Context
import com.kengine.event.EventContext
import com.kengine.geometry.GeometryContext
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.TextureContext
import com.kengine.input.KeyboardContext
import com.kengine.input.MouseContext
import com.kengine.log.Logger
import com.kengine.sdl.SDLContext
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.useSDLQuitEventSubscriber
import com.kengine.sound.SoundContext

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
    val actions: ActionsContext
) : Context() {
    var isRunning = true

    init {
        useSDLQuitEventSubscriber()
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
                actions = ActionsContext.get()
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
        keyboard.cleanup()
        mouse.cleanup()
        textures.cleanup()
        sprites.cleanup()
        geometry.cleanup()
        sounds.cleanup()
        sdl.cleanup()
    }

}