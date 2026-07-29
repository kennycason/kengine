package com.kengine.audio

object AudioAssetId {
    fun music(name: String): Int {
        return stableId("music:$name")
    }

    private fun stableId(value: String): Int {
        var hash = FNV_OFFSET_BASIS
        for (char in value) {
            hash = hash xor char.code
            hash *= FNV_PRIME
        }
        return if (hash == 0) 1 else hash
    }

    private const val FNV_OFFSET_BASIS = -0x7ee3623b
    private const val FNV_PRIME = 0x01000193
}
