package com.kengine.sound

import com.kengine.assets.PortableAssetCatalog
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioCommandBuffer
import com.kengine.audio.AudioCommandType
import com.kengine.audio.AudioContext
import com.kengine.audio.PortableAudioSink
import com.kengine.log.Logging

class PortableAudioSdlRenderer(
    assets: PortableAssetCatalog,
    resolvePath: (String) -> String = { it }
) : PortableAudioSink, Logging {
    private val musicById = mutableMapOf<Int, Sound>()
    private val soundsById = mutableMapOf<Int, Sound>()
    private var currentMusicAssetId = 0
    private var currentMusicVolume = -1

    init {
        assets.music.forEach { music ->
            musicById[AudioAssetId.music(music.id)] = registerPortableSound(
                name = "music:${music.id}",
                filePath = resolvePath(music.source)
            )
        }
        assets.sounds.forEach { sound ->
            soundsById[AudioAssetId.sound(sound.id)] = registerPortableSound(
                name = "sound:${sound.id}",
                filePath = resolvePath(sound.source)
            )
        }
    }

    override fun render(audio: AudioContext) {
        var commandIndex = 0
        while (commandIndex < audio.commandCount) {
            when (audio.commandField(commandIndex, AudioCommandBuffer.FIELD_TYPE)) {
                AudioCommandType.LOOP_MUSIC -> loopMusic(
                    assetId = audio.commandField(commandIndex, AudioCommandBuffer.FIELD_ASSET_ID),
                    volume = audio.commandField(commandIndex, AudioCommandBuffer.FIELD_VOLUME)
                )
                AudioCommandType.STOP_MUSIC -> stopMusic(
                    assetId = audio.commandField(commandIndex, AudioCommandBuffer.FIELD_ASSET_ID)
                )
                AudioCommandType.PLAY_SOUND -> playSound(
                    assetId = audio.commandField(commandIndex, AudioCommandBuffer.FIELD_ASSET_ID),
                    volume = audio.commandField(commandIndex, AudioCommandBuffer.FIELD_VOLUME)
                )
            }
            commandIndex += 1
        }
    }

    private fun loopMusic(assetId: Int, volume: Int) {
        val music = musicById[assetId] ?: return missingAsset("music", assetId)
        val sdlVolume = toSdlVolume(volume)

        if (currentMusicAssetId == assetId) {
            if (currentMusicVolume != sdlVolume) {
                music.setVolume(sdlVolume)
                currentMusicVolume = sdlVolume
            }
            return
        }

        if (currentMusicAssetId != 0) {
            musicById[currentMusicAssetId]?.stop()
        }

        music.setVolume(sdlVolume)
        music.loop()
        currentMusicAssetId = assetId
        currentMusicVolume = sdlVolume
    }

    private fun stopMusic(assetId: Int) {
        val targetAssetId = if (assetId == 0) currentMusicAssetId else assetId
        if (targetAssetId == 0 || currentMusicAssetId != targetAssetId) {
            return
        }

        musicById[targetAssetId]?.stop()
        currentMusicAssetId = 0
        currentMusicVolume = -1
    }

    private fun playSound(assetId: Int, volume: Int) {
        val sound = soundsById[assetId] ?: return missingAsset("sound", assetId)
        sound.setVolume(toSdlVolume(volume))
        sound.play()
    }

    private fun registerPortableSound(name: String, filePath: String): Sound {
        val sound = Sound(filePath)
        getSoundContext().addSound("portable:$name", sound)
        return sound
    }

    private fun toSdlVolume(volume: Int): Int {
        return (volume.coerceIn(0, AudioContext.MAX_VOLUME) * 100) / AudioContext.MAX_VOLUME
    }

    private fun missingAsset(type: String, assetId: Int) {
        logger.warn { "Portable $type asset not registered: $assetId" }
    }
}
