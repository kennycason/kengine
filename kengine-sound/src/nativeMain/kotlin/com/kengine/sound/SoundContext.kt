package com.kengine.sound

import com.kengine.hooks.context.Context
import com.kengine.log.Logger
import com.kengine.log.Logging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.exit
import sdl3.SDL_AudioSpec
import sdl3.SDL_GetError
import sdl3.SDL_INIT_AUDIO
import sdl3.SDL_Init
import sdl3.mixer.Mix_CloseAudio
import sdl3.mixer.Mix_OpenAudio

@OptIn(ExperimentalForeignApi::class)
class SoundContext private constructor(
    private val manager: SoundManager
) : Context(), Logging {

    init {
        require(SDL_Init(SDL_INIT_AUDIO)) {
            Companion.logger.error("Error initializing SDL Audio: ${SDL_GetError()?.toKString()}")
            exit(1)
        }

        memScoped {
            val audioSpec = alloc<SDL_AudioSpec>().apply {
                freq = 44100     // Sample rate
                format = 0x8010u // 16-bit signed little-endian
                channels = 2     // Stereo
            }

            Companion.logger.info { "freq=${audioSpec.freq}, format=${audioSpec.format}, channels=${audioSpec.channels}" }

            val result = Mix_OpenAudio(0u, audioSpec.ptr)
            require(result) {
                "Failed to initialize SDL_mixer: ${SDL_GetError()?.toKString()}"
            }
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
        Mix_CloseAudio()
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
