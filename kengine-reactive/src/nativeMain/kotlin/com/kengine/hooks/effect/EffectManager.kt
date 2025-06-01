package com.kengine.hooks.effect

class EffectManager {
    private val activeEffects = mutableListOf<Effect>()

    fun addEffect(effect: Effect) {
        activeEffects.add(effect)
        effect.execute() // Execute immediately on add
    }

    fun update() {
        activeEffects.forEach { it.checkDependencies() } // Check dependency updates
    }

    fun cleanup() {
        activeEffects.forEach { it.cleanup() }
        activeEffects.clear()
    }
}
