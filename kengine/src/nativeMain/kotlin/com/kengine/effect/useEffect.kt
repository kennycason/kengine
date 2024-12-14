package com.kengine.effect

import com.kengine.context.getContext
import com.kengine.state.State

fun useEffect(effect: () -> Unit, vararg dependencies: State<*>) {
    val effectContext = getContext<EffectContext>() // Automatically fetch EffectContext
    effectContext.useEffect(effect, *dependencies)
}