package com.kengine

import com.kengine.action.ActionContext
import com.kengine.event.EventContext
import com.kengine.font.FontContext
import com.kengine.geometry.GeometryContext
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.TextureContext
import com.kengine.hooks.context.Context
import com.kengine.hooks.context.ContextRegistry
import com.kengine.input.controller.ControllerContext
import com.kengine.input.controller.getControllerContext
import com.kengine.input.keyboard.KeyboardContext
import com.kengine.input.keyboard.getKeyboardContext
import com.kengine.input.mouse.MouseContext
import com.kengine.input.mouse.getMouseContext
import com.kengine.log.Logger
import com.kengine.log.LoggerContext
import com.kengine.log.Logging
import com.kengine.network.NetworkContext
import com.kengine.physics.PhysicsContext
import com.kengine.sdl.SDLContext
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.registerSDLQuitHandler
import com.kengine.sound.SoundContext
import com.kengine.time.ClockContext

class GameContext private constructor(
    val log: LoggerContext,
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
    val network: NetworkContext,
    val action: ActionContext,
    val physics: PhysicsContext,
    val clock: ClockContext,
) : Context(), Logging {
    var isRunning = true

    init {
        ContextRegistry.register(log)
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
        ContextRegistry.register(network)
        ContextRegistry.register(action)
        ContextRegistry.register(physics)
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
            height: Int,
            logLevel: Logger.Level = Logger.Level.INFO
        ): GameContext {
            if (currentContext != null) {
                throw IllegalStateException("SDLContext has already been created. Call cleanup() before creating a new context.")
            }
            // TODO order matters, i.e. there are dependencies
            currentContext = GameContext(
                log = LoggerContext.create(logLevel),
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
                network = NetworkContext.get(),
                geometry = GeometryContext.get(),
                action = ActionContext.get(),
                physics = PhysicsContext.get(),
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
        log.cleanup()
        action.cleanup()
        sdlEvent.cleanup()
        events.cleanup()
        keyboard.cleanup()
        mouse.cleanup()
        controller.cleanup()
        texture.cleanup()
        sprite.cleanup()
        geometry.cleanup()
        font.cleanup()
        sound.cleanup()
        network.cleanup()
        sdl.cleanup()
        clock.cleanup()
        ContextRegistry.clearAll()
    }

}


