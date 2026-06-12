package com.kengine.scene

import com.kengine.Game

class SceneGame(initialScene: Scene) : Game {
    private val sceneContext = SceneContext.get()

    init {
        sceneContext.push(initialScene)
    }

    override fun update() = sceneContext.update()
    override fun draw() = sceneContext.draw()
    override fun cleanup() = sceneContext.cleanup()
}
