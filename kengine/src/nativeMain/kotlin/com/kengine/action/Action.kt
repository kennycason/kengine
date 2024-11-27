package com.kengine.action

interface Action {
    fun update(): Boolean // Return `true` if action is complete
}
