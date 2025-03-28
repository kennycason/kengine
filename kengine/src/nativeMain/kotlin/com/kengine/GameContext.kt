package com.kengine

import com.kengine.action.ActionContext
import com.kengine.event.EventContext
import com.kengine.font.FontContext
import com.kengine.geometry.GeometryContext
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.TextureContext
import com.kengine.hooks.context.Context
import com.kengine.hooks.context.ContextRegistry
import com.kengine.hooks.effect.EffectContext
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
import com.kengine.ui.ViewContext

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
    val view: ViewContext,
    val sound: SoundContext,
    val network: NetworkContext,
    val action: ActionContext,
    val effect: EffectContext,
    val physics: PhysicsContext,
    val clock: ClockContext,
) : Context(), Logging {
    var isRunning = true

    init {
        initContexts()
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
                throw IllegalStateException("GameContext has already been created. Call cleanup() before creating a new context.")
            }

            // TODO order matters, i.e. there are dependencies
            return GameContext(
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
                view = ViewContext.get(),
                sound = SoundContext.get(),
                network = NetworkContext.get(),
                geometry = GeometryContext.get(),
                action = ActionContext.get(),
                effect = EffectContext(),
                physics = PhysicsContext.get(),
                clock = ClockContext.get()
            )
                .also { it.initContexts() }
                .also { currentContext = it }
        }

        fun get(): GameContext {
            return currentContext ?: throw IllegalStateException("AppContext has not been created. Call create() first.")
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up GameContext" }
        log.cleanup()
        action.cleanup()
        effect.cleanup()
        sdlEvent.cleanup()
        events.cleanup()
        keyboard.cleanup()
        mouse.cleanup()
        controller.cleanup()
        texture.cleanup()
        sprite.cleanup()
        geometry.cleanup()
        font.cleanup()
        view.cleanup()
        sound.cleanup()
        network.cleanup()
        sdl.cleanup()
        clock.cleanup()
       // physics.cleanup() TODO track error with cleanup during ITests (consecutive start/cleanups()

        ContextRegistry.clearAll()
        currentContext = null
    }

    internal fun initContexts() {
        ContextRegistry.register(this)
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
        ContextRegistry.register(view)
        ContextRegistry.register(sound)
        ContextRegistry.register(network)
        ContextRegistry.register(action)
        ContextRegistry.register(effect)
        ContextRegistry.register(physics)
        ContextRegistry.register(clock)
        getKeyboardContext().init()
        getMouseContext().init()
        getControllerContext().init()
        registerSDLQuitHandler()
    }
}
