package com.kengine.sound

import com.kengine.hooks.context.Context
import com.kengine.log.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.exit
import sdl3.SDL_GetError
import sdl3.SDL_INIT_AUDIO
import sdl3.SDL_Init
import sdl3.mixer.Mix_CloseAudio
import sdl3.mixer.Mix_OpenAudio
import sdl3.mixer.SDL_AudioSpec

@OptIn(ExperimentalForeignApi::class)
class SoundContext private constructor(
    private val manager: SoundManager
) : Context() {

    fun addSound(name: String, sound: Sound) {
        manager.addSound(name, sound)
    }

    fun getSound(name: String): Sound {
        return manager.getSound(name)
    }

    override fun cleanup() {
        manager.cleanup()
        Mix_CloseAudio()
    }

    companion object {
        private val logger = Logger.get(SoundContext::class)
        private var currentContext: SoundContext? = null

        fun get(): SoundContext {
            if (currentContext == null) {
                currentContext = SoundContext(
                    manager = SoundManager()
                )

                require(SDL_Init(SDL_INIT_AUDIO)) {
                    logger.error("Error initializing SDL Audio: ${SDL_GetError()?.toKString()}")
                    exit(1)
                }

                memScoped {
                    val audioSpec = alloc<SDL_AudioSpec>().apply {
                        freq = 44100     // Sample rate
                        format = 0x8010u // 16-bit signed little-endian
                        channels = 2     // Stereo
                    }

                    logger.info { "freq=${audioSpec.freq}, format=${audioSpec.format}, channels=${audioSpec.channels}" }

                    val result = Mix_OpenAudio(0u, audioSpec.ptr)
                    require(result) {
                        "Failed to initialize SDL_mixer: ${SDL_GetError()?.toKString()}"
                    }
                }
            }
            return currentContext ?: throw IllegalStateException("Failed to create SoundContext")
        }
    }
}
