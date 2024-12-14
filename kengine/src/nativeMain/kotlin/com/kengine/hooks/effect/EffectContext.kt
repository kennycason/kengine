package com.kengine.hooks.effect

import com.kengine.hooks.context.Context
import com.kengine.hooks.state.State

class EffectContext : Context() {
    private val effectManager = EffectManager()

    override fun cleanup() {
        super.cleanup()
        effectManager.cleanup()
    }

    fun useEffect(effect: () -> Unit, vararg dependencies: State<*>) {
        effectManager.addEffect(Effect(effect, dependencies.toList()))
    }
}