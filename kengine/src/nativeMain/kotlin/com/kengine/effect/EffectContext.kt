package com.kengine.effect

import com.kengine.context.Context
import com.kengine.state.State

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