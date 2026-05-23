package com.kengine.sound

import com.kengine.hooks.context.Context
import com.kengine.log.Logger
import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.exit
import sdl3.SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK
import sdl3.SDL_GetError
import sdl3.SDL_INIT_AUDIO
import sdl3.SDL_Init
import sdl3.mixer.MIX_CreateMixerDevice
import sdl3.mixer.MIX_DestroyMixer
import sdl3.mixer.MIX_Init
import sdl3.mixer.MIX_Quit

@OptIn(ExperimentalForeignApi::class)
class SoundContext private constructor(
    private val manager: SoundManager
) : Context(), Logging {

    var mixer: CPointer<cnames.structs.MIX_Mixer>? = null
        private set

    init {
        require(SDL_Init(SDL_INIT_AUDIO)) {
            Companion.logger.error("Error initializing SDL Audio: ${SDL_GetError()?.toKString()}")
            exit(1)
        }

        require(MIX_Init()) {
            "Failed to initialize SDL_mixer: ${SDL_GetError()?.toKString()}"
        }

        mixer = MIX_CreateMixerDevice(SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK, null)
        requireNotNull(mixer) {
            "Failed to create SDL_mixer device: ${SDL_GetError()?.toKString()}"
        }
    }

    fun addSound(name: String, sound: Sound) {
        manager.addSound(name, sound)
    }

    fun getSound(name: String): Sound {
        return manager.getSound(name)
    }

    override fun cleanup() {
        logger.info { "Cleaning up SoundContext" }
        manager.cleanup()
        mixer?.let { MIX_DestroyMixer(it) }
        mixer = null
        MIX_Quit()
        currentContext = null
    }

    companion object {
        private val logger = Logger.get(SoundContext::class)
        private var currentContext: SoundContext? = null

        fun get(): SoundContext {
            return currentContext ?: SoundContext(
                manager = SoundManager()
            ).also {
                currentContext = it
            }
        }
    }
}
