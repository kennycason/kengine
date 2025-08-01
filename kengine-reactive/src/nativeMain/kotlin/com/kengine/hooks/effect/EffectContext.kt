package com.kengine.hooks.effect

import com.kengine.hooks.context.Context

class EffectContext : Context() {
    private val effectManager = EffectManager()

    override fun cleanup() {
        effectManager.cleanup()
    }

    fun useEffect(effect: Effect) {
        effectManager.addEffect(effect)
    }

    fun update() {
        effectManager.update() // Ensure effects are checked for updates
    }
}
