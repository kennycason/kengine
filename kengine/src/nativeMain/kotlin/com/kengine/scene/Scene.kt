package com.kengine.scene

interface Scene {
    fun enter() {}
    fun exit() {}
    fun pause() {}
    fun resume() {}
    fun update()
    fun draw()
    fun cleanup() {}
}
