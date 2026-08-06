package com.kengine.sound

import com.kengine.file.File
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import sdl3.SDL_GetError
import sdl3.mixer.MIX_CreateTrack
import sdl3.mixer.MIX_DestroyAudio
import sdl3.mixer.MIX_DestroyTrack
import sdl3.mixer.MIX_LoadAudio
import sdl3.mixer.MIX_PauseTrack
import sdl3.mixer.MIX_PlayTrack
import sdl3.mixer.MIX_ResumeTrack
import sdl3.mixer.MIX_SetTrackAudio
import sdl3.mixer.MIX_SetTrackGain
import sdl3.mixer.MIX_SetTrackLoops
import sdl3.mixer.MIX_StopTrack
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
class Sound(filePath: String) {
    private var audio: CPointer<cnames.structs.MIX_Audio>? = null
    private var track: CPointer<cnames.structs.MIX_Track>? = null

    // 0 (silent) to 100 (maximum)
    private var volume: Int = 100
    private val fullFilePath = File.resolveAssetPath(filePath)

    init {
        audio = MIX_LoadAudio(getSoundContext().mixer(), fullFilePath, true)
        requireNotNull(audio) { "Failed to load sound: $fullFilePath" }
    }

    /**
     * Sets the volume for this sound.
     * @param volume An integer between 0 (silent) and 100 (maximum).
     */
    fun setVolume(volume: Int) {
        this.volume = max(0, min(volume, 100))
        track?.let { MIX_SetTrackGain(it, this.volume / 100.0f) }
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
        playWithLoopCount(loops = 0, action = "play")
    }

    /**
     * Loops the sound indefinitely.
     */
    fun loop() {
        playWithLoopCount(loops = -1, action = "loop")
    }

    private fun playWithLoopCount(loops: Int, action: String) {
        prepareTrack()
        track?.let {
            require(MIX_SetTrackGain(it, volume / 100.0f)) {
                "Failed to set track gain: $fullFilePath (${SDL_GetError()?.toKString()})"
            }
            require(MIX_PlayTrack(it, 0u)) {
                "Failed to $action sound: $fullFilePath (${SDL_GetError()?.toKString()})"
            }
            if (loops != 0) {
                require(MIX_SetTrackLoops(it, loops)) {
                    "Failed to set track loops: $fullFilePath (${SDL_GetError()?.toKString()})"
                }
            }
        } ?: error("Failed to $action sound: $fullFilePath")
    }

    /**
     * Pauses the sound if it's currently playing.
     */
    fun pause() {
        track?.let { MIX_PauseTrack(it) }
    }

    /**
     * Resumes the sound if it's paused.
     */
    fun resume() {
        track?.let { MIX_ResumeTrack(it) }
    }

    /**
     * Stops the sound if it's currently playing.
     */
    fun stop() {
        track?.let {
            MIX_StopTrack(it, 0)
        }
    }

    /**
     * Cleans up the sound resources when no longer needed.
     */
    fun cleanup() {
        track?.let { MIX_DestroyTrack(it) }
        track = null
        audio?.let { MIX_DestroyAudio(it) }
        audio = null
    }

    private fun prepareTrack() {
        if (track == null) {
            track = MIX_CreateTrack(getSoundContext().mixer())
            requireNotNull(track) { "Failed to create track for: $fullFilePath" }
        }
        track?.let {
            require(MIX_SetTrackAudio(it, audio)) {
                "Failed to set track audio: $fullFilePath (${SDL_GetError()?.toKString()})"
            }
        }
    }
}
