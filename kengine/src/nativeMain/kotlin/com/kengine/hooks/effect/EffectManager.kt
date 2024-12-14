package com.kengine.hooks.effect

class EffectManager {
    private val activeEffects = mutableListOf<Effect>()

    fun addEffect(effect: Effect) {
        activeEffects.add(effect)
    }

    fun cleanup() {
        activeEffects.forEach { it.cleanup() }
        activeEffects.clear()
    }
}
