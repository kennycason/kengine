package com.kengine.entity

import com.kengine.Vec2

interface Entity {
    val p: Vec2
    val v: Vec2
    val width: Int
    val height: Int

    fun update(elapsedSeconds: Double)
    fun draw(elapsedSeconds: Double)
    fun cleanup()
}
