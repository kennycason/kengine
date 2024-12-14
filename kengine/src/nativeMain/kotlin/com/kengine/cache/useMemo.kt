package com.kengine.cache

import com.kengine.state.State

fun <T> useMemo(compute: () -> T, vararg dependencies: State<*>): MemoizedValue<T> {
    return MemoizedValue(compute, dependencies.toList())
}