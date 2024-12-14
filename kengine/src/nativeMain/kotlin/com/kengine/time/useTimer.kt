package com.kengine.time

import com.kengine.action.getActionContext

fun useTimer(delayMs: Long, onComplete: () -> Unit) {
    getActionContext().timer(delayMs, onComplete)
}