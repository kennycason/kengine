package com.kengine

data class Vec2(
    var x: Double = 0.0,
    var y: Double = 0.0
) {
    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }
    fun set(v: Double) {
        this.x = v
        this.y = v
    }
    fun set(v: Vec2) {
        this.x = v.x
        this.y = v.y
    }
}