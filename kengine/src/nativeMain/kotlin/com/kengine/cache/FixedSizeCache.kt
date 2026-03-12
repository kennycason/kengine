package com.kengine.cache

class FixedSizeCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>(maxSize)

    fun getOrPut(key: K, defaultValue: () -> V): V {
        map[key]?.let { return it }
        if (map.size >= maxSize) {
            val oldest = map.keys.first()
            map.remove(oldest)
        }
        val value = defaultValue()
        map[key] = value
        return value
    }
}
