package com.kengine.sound

import com.kengine.file.File
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
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
    private val fullFilePath = "${File.pwd()}/$filePath"

    init {
        val mixer = getSoundContext().mixer
            ?: error("SoundContext mixer not initialized")
        audio = MIX_LoadAudio(mixer, fullFilePath, true)
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
        prepareTrack(loops = 0)
        track?.let {
            MIX_SetTrackGain(it, volume / 100.0f)
            MIX_PlayTrack(it, 0u)
        } ?: error("Failed to play sound: $fullFilePath")
    }

    /**
     * Loops the sound indefinitely.
     */
    fun loop() {
        prepareTrack(loops = -1)
        track?.let {
            MIX_SetTrackGain(it, volume / 100.0f)
            MIX_PlayTrack(it, 0u)
        } ?: error("Failed to loop sound: $fullFilePath")
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

    private fun prepareTrack(loops: Int) {
        if (track == null) {
            val mixer = getSoundContext().mixer
                ?: error("SoundContext mixer not initialized")
            track = MIX_CreateTrack(mixer)
            requireNotNull(track) { "Failed to create track for: $fullFilePath" }
        }
        track?.let {
            MIX_SetTrackAudio(it, audio)
            MIX_SetTrackLoops(it, loops)
        }
    }
}
