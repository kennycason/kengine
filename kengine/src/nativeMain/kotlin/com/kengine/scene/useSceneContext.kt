package com.kengine.scene

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useSceneContext(cleanup: Boolean = false, block: SceneContext.() -> R): R {
    return useContextWithReturn<SceneContext, R>(cleanup, block)
}
