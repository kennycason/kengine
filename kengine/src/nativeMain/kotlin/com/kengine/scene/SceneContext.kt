package com.kengine.scene

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class SceneContext private constructor(
    val manager: SceneManager = SceneManager()
) : Context(), Logging {

    fun push(scene: Scene, transition: SceneTransition? = null) = manager.push(scene, transition)
    fun pop(transition: SceneTransition? = null) = manager.pop(transition)
    fun replace(scene: Scene, transition: SceneTransition? = null) = manager.replace(scene, transition)
    fun update() = manager.update()
    fun draw() = manager.draw()

    val currentScene: Scene? get() = manager.currentScene
    val depth: Int get() = manager.depth

    override fun cleanup() {
        logger.info { "Cleaning up SceneContext" }
        manager.cleanup()
        currentContext = null
    }

    companion object {
        private var currentContext: SceneContext? = null

        fun get(): SceneContext {
            return currentContext ?: SceneContext().also {
                currentContext = it
            }
        }
    }
}
