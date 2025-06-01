package com.kengine.hooks.effect

import com.kengine.hooks.context.getContext
import com.kengine.hooks.state.State

fun useEffect(effect: () -> Unit, vararg dependencies: State<*>) {
    val effectContext = getContext<EffectContext>()
    val newEffect = Effect(effect, dependencies.toList())
    effectContext.useEffect(newEffect)
    newEffect.execute()
}
