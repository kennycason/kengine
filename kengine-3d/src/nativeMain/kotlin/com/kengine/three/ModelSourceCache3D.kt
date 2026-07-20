package com.kengine.three

data class ModelSourceCacheKey3D(
    val assetPath: String,
    val options: ModelLoadOptions3D = ModelLoadOptions3D()
) {
    init {
        require(assetPath.isNotBlank()) {
            "Model source cache asset path must not be blank."
        }
    }
}

class ModelSourceCache3D internal constructor(
    private val loadSource: (assetPath: String, options: ModelLoadOptions3D) -> ParsedModel3D
) {
    constructor() : this({ assetPath, options ->
        ModelLoader3D.loadSource(assetPath, options)
    })

    private val sources = linkedMapOf<ModelSourceCacheKey3D, ParsedModel3D>()

    val size: Int
        get() = sources.size

    fun load(
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): ParsedModel3D {
        return load(ModelSourceCacheKey3D(assetPath, options))
    }

    fun load(key: ModelSourceCacheKey3D): ParsedModel3D {
        return sources.getOrPut(key) {
            loadSource(key.assetPath, key.options)
        }
    }

    fun contains(
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): Boolean {
        return containsKey(ModelSourceCacheKey3D(assetPath, options))
    }

    fun containsKey(key: ModelSourceCacheKey3D): Boolean {
        return sources.containsKey(key)
    }

    fun get(
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): ParsedModel3D? {
        return get(ModelSourceCacheKey3D(assetPath, options))
    }

    fun get(key: ModelSourceCacheKey3D): ParsedModel3D? {
        return sources[key]
    }

    fun clear() {
        sources.clear()
    }
}
