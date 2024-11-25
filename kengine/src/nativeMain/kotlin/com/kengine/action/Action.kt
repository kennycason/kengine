package com.kengine.action

interface Action {
    fun update(deltaTime: Double): Boolean // Return `true` if action is complete
}
