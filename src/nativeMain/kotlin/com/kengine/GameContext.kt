package com.kengine

import com.kengine.action.ActionsContext
import com.kengine.context.Context
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.TextureContext
import com.kengine.input.KeyboardContext
import com.kengine.input.MouseContext
import com.kengine.sdl.SDLContext
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.useSDLQuitEventSubscriber
import com.kengine.sound.SoundContext

class GameContext private constructor(
    val sdl: SDLContext,
    val events: SDLEventContext,
    val keyboard: KeyboardContext,
    val mouse: MouseContext,
    val textures: TextureContext,
    val sprites: SpriteContext,
    val sounds: SoundContext,
    val actions: ActionsContext
) : Context() {

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
                events = SDLEventContext.get(),
                keyboard = KeyboardContext.get(),
                mouse = MouseContext.get(),
                textures = TextureContext.get(),
                sprites = SpriteContext.get(),
                sounds = SoundContext.get(),
                actions = ActionsContext.get()
            )
            return currentContext!!
        }

        fun get(): GameContext {
            return currentContext ?: throw IllegalStateException("AppContext has not been created. Call create() first.")
        }

    }

    override fun cleanup() {
        sdl.cleanup()
        keyboard.cleanup()
        mouse.cleanup()
        events.cleanup()
        textures.cleanup()
        sprites.cleanup()
        sounds.cleanup()
        actions.cleanup()
    }

}