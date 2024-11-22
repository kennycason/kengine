package com.kengine.entity

import com.kengine.Vec2D

interface Entity {
    val p: Vec2D
    val v: Vec2D
    val width: Int
    val height: Int

    fun update(elapsedSeconds: Double)
    fun draw(elapsedSeconds: Double)
    fun cleanup()
}
