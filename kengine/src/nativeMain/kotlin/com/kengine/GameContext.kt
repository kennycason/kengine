package com.kengine

import com.kengine.action.ActionContext
import com.kengine.context.Context
import com.kengine.context.ContextRegistry
import com.kengine.event.EventContext
import com.kengine.font.FontContext
import com.kengine.geometry.GeometryContext
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.TextureContext
import com.kengine.input.controller.ControllerContext
import com.kengine.input.controller.getControllerContext
import com.kengine.input.keyboard.KeyboardContext
import com.kengine.input.keyboard.getKeyboardContext
import com.kengine.input.mouse.MouseContext
import com.kengine.input.mouse.getMouseContext
import com.kengine.log.Logging
import com.kengine.sdl.SDLContext
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.registerSDLQuitHandler
import com.kengine.sound.SoundContext
import com.kengine.time.ClockContext

class GameContext private constructor(
    val sdl: SDLContext,
    val sdlEvent: SDLEventContext,
    val events: EventContext,
    val keyboard: KeyboardContext,
    val mouse: MouseContext,
    val controller: ControllerContext,
    val texture: TextureContext,
    val sprite: SpriteContext,
    val geometry: GeometryContext,
    val font: FontContext,
    val sound: SoundContext,
    val action: ActionContext,
    val clock: ClockContext,
) : Context(), Logging {
    var isRunning = true

    init {
        ContextRegistry.register(sdl)
        ContextRegistry.register(sdlEvent)
        ContextRegistry.register(events)
        ContextRegistry.register(keyboard)
        ContextRegistry.register(mouse)
        ContextRegistry.register(controller)
        ContextRegistry.register(texture)
        ContextRegistry.register(sprite)
        ContextRegistry.register(geometry)
        ContextRegistry.register(font)
        ContextRegistry.register(sound)
        ContextRegistry.register(action)
        ContextRegistry.register(clock)
        getKeyboardContext().init()
        getMouseContext().init()
        getControllerContext().init()
        registerSDLQuitHandler()
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
                sdlEvent = SDLEventContext.get(),
                keyboard = KeyboardContext.get(),
                mouse = MouseContext.get(),
                controller = ControllerContext.get(),
                texture = TextureContext.get(),
                sprite = SpriteContext.get(),
                font = FontContext.get(),
                sound = SoundContext.get(),
                geometry = GeometryContext.get(),
                action = ActionContext.get(),
                clock = ClockContext.get()
            )
            return currentContext!!
        }

        fun get(): GameContext {
            return currentContext ?: throw IllegalStateException("AppContext has not been created. Call create() first.")
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up game resources" }
        action.cleanup()
        sdlEvent.cleanup()
        events.cleanup()
        keyboard.cleanup()
        mouse.cleanup()
        controller.cleanup()
        texture.cleanup()
        sprite.cleanup()
        geometry.cleanup()
        sound.cleanup()
        sdl.cleanup()
        clock.cleanup()
        ContextRegistry.clearAll()
    }

}


