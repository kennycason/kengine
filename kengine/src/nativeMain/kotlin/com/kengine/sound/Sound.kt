package com.kengine.sound

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.mixer.MIX_MAX_VOLUME
import sdl2.mixer.Mix_Chunk
import sdl2.mixer.Mix_FreeChunk
import sdl2.mixer.Mix_HaltChannel
import sdl2.mixer.Mix_LoadWAV
import sdl2.mixer.Mix_Pause
import sdl2.mixer.Mix_PlayChannel
import sdl2.mixer.Mix_Resume
import sdl2.mixer.Mix_Volume
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
class Sound(private val filePath: String) {
    private var sound: CPointer<Mix_Chunk>? = null
    private var channel: Int = -1

    // 0 (silent) to 100 (maximum)
    private var volume: Int = 100

    init {
        sound = Mix_LoadWAV(filePath)
        requireNotNull(sound) { "Failed to load sound: $filePath" }
    }

    /**
     * Sets the volume for this sound.
     * @param volume An integer between 0 (silent) and 100 (maximum).
     */
    fun setVolume(volume: Int) {
        this.volume = max(0, min(volume, 100))
        Mix_Volume(channel, scaleVolume(this.volume))
    }

    /**
     * Retrieves the current volume of this sound.
     * @return The volume level as an integer between 0 and 100.
     */
    fun getVolume(): Int = volume

    /**
     * Plays the sound once.
     */
    fun play() {
        channel = Mix_PlayChannel(-1, sound, 0)
        require(channel != -1) { "Failed to play sound: $filePath" }
        Mix_Volume(channel, scaleVolume(volume))
    }

    /**
     * Loops the sound indefinitely.
     */
    fun loop() {
        channel = Mix_PlayChannel(-1, sound, -1)
        require(channel != -1) { "Failed to loop sound: $filePath" }
        Mix_Volume(channel, scaleVolume(volume))
    }

    /**
     * Pauses the sound if it's currently playing.
     */
    fun pause() {
        if (channel != -1) {
            Mix_Pause(channel)
        }
    }

    /**
     * Resumes the sound if it's paused.
     */
    fun resume() {
        if (channel != -1) {
            Mix_Resume(channel)
        }
    }
    
    /**
     * Stops the sound if it's currently playing.
     */
    fun stop() {
        if (channel != -1) {
            Mix_HaltChannel(channel)
            channel = -1
        }
    }

    /**
     * Cleans up the sound resources when no longer needed
     */
    fun cleanup() {
        sound?.let {
            Mix_FreeChunk(it)
        }
        sound = null
    }

    /**
     * Linearly map the 0-100 volume range to SDL_mixer's 0-128 range.
     * @param volume The volume level on a 0-100 scale.
     * @return The scaled volume on a 0-128 scale.
     */
    private fun scaleVolume(volume: Int): Int {
        return ((volume / 100.0) * MIX_MAX_VOLUME).toInt().coerceIn(0, MIX_MAX_VOLUME)
    }

}