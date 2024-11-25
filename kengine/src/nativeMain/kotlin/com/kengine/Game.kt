package com.kengine

interface Game {
    fun update(elapsedSeconds: Double)
    fun draw(elapsedSeconds: Double)
    fun cleanup()
}