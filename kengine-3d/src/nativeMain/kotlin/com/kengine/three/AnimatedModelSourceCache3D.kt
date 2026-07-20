package com.kengine.three

data class AnimatedModelSourceCacheKey3D(
    val assetPath: String,
    val type: AnimatedModelType3D,
    val options: ModelLoadOptions3D = ModelLoadOptions3D()
) {
    init {
        require(assetPath.isNotBlank()) {
            "Animated model source cache asset path must not be blank."
        }
    }
}

class AnimatedModelSourceCache3D internal constructor(
    private val loadSource: (
        assetPath: String,
        type: AnimatedModelType3D,
        options: ModelLoadOptions3D
    ) -> AnimatedModelSource3D
) {
    constructor() : this({ assetPath, type, options ->
        AnimatedModelLoader3D.loadSource(assetPath, type, options)
    })

    private val sources = linkedMapOf<AnimatedModelSourceCacheKey3D, AnimatedModelSource3D>()

    val size: Int
        get() = sources.size

    fun load(
        assetPath: String,
        type: AnimatedModelType3D,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): AnimatedModelSource3D {
        return load(AnimatedModelSourceCacheKey3D(assetPath, type, options))
    }

    fun load(key: AnimatedModelSourceCacheKey3D): AnimatedModelSource3D {
        return sources.getOrPut(key) {
            loadSource(key.assetPath, key.type, key.options)
        }
    }

    fun contains(
        assetPath: String,
        type: AnimatedModelType3D,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): Boolean {
        return containsKey(AnimatedModelSourceCacheKey3D(assetPath, type, options))
    }

    fun containsKey(key: AnimatedModelSourceCacheKey3D): Boolean {
        return sources.containsKey(key)
    }

    fun get(
        assetPath: String,
        type: AnimatedModelType3D,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): AnimatedModelSource3D? {
        return get(AnimatedModelSourceCacheKey3D(assetPath, type, options))
    }

    fun get(key: AnimatedModelSourceCacheKey3D): AnimatedModelSource3D? {
        return sources[key]
    }

    fun clear() {
        sources.clear()
    }
}
