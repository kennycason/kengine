package com.kengine.cache

class FixedSizeCache<K, V>(private val maxSize: Int) {
    private val map = mutableMapOf<K, V>()
    private val keys = ArrayDeque<K>(maxSize)

    fun getOrPut(key: K, defaultValue: () -> V): V {
        return map[key] ?: run {
            if (keys.size >= maxSize) {
                // Remove oldest entry
                map.remove(keys.removeFirst())
            }
            val value = defaultValue()
            map[key] = value
            keys.addLast(key)
            value
        }
    }
}
