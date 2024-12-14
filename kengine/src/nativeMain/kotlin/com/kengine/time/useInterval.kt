package com.kengine.time

import com.kengine.action.getActionContext

fun useInterval(intervalMs: Long, onTick: () -> Unit) {
    getActionContext().interval(intervalMs, onTick)
}