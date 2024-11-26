package com.kengine.sound

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.mixer.Mix_Chunk
import sdl2.mixer.Mix_FreeChunk
import sdl2.mixer.Mix_HaltChannel
import sdl2.mixer.Mix_LoadWAV
import sdl2.mixer.Mix_Pause
import sdl2.mixer.Mix_PlayChannel

@OptIn(ExperimentalForeignApi::class)
class Sound(private val filePath: String) {
    private var sound: CPointer<Mix_Chunk>? = null
    private var channel: Int = -1

    init {
        sound = Mix_LoadWAV(filePath)
        requireNotNull(sound) { "Failed to load sound: $filePath" }
    }

    fun play() {
        channel = Mix_PlayChannel(-1, sound, 0)
        require(channel != -1) { "Failed to play sound: $filePath" }
    }

    fun loop() {
        channel = Mix_PlayChannel(-1, sound, -1)
        require(channel != -1) { "Failed to loop sound: $filePath" }
    }

    fun pause() {
        if (channel != -1) {
            Mix_Pause(channel)
        }
    }

    fun stop() {
        if (channel != -1) {
            Mix_HaltChannel(channel)
            channel = -1
        }
    }

    fun cleanup() {
        sound?.let {
            Mix_FreeChunk(it)
        }
        sound = null
    }
}