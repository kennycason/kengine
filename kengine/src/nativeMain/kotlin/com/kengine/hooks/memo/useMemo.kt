package com.kengine.memo

import com.kengine.hooks.state.State

fun <T> useMemo(compute: () -> T, vararg dependencies: State<*>): MemoizedValue<T> {
    return MemoizedValue(compute, dependencies.toList())
}