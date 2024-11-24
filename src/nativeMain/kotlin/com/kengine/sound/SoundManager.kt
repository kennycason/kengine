package com.kengine.sound

class SoundManager {
    private val sounds = mutableMapOf<String, Sound>()

    fun setSound(name: String, sound: Sound) {
        sounds[name] = sound
    }

    fun getSound(name: String): Sound {
        return sounds[name] ?: throw IllegalArgumentException("Sound not found: $name")
    }

    fun cleanup() {
        sounds.values.forEach { it.cleanup() }
        sounds.clear()
    }
}