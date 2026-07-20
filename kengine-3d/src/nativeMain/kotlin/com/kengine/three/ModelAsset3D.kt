package com.kengine.three

import com.kengine.file.File

data class ModelAsset3D(
    val relativePath: String,
    val options: ModelLoadOptions3D = ModelLoadOptions3D()
) {
    val format: ModelFormat3D
        get() = ModelLoader3D.detectFormat(relativePath)
}

data class AnimatedModelAsset3D(
    val relativePath: String,
    val type: AnimatedModelType3D,
    val options: ModelLoadOptions3D = ModelLoadOptions3D()
) {
    val format: ModelFormat3D
        get() = ModelLoader3D.detectFormat(relativePath)

    companion object {
        fun nodeAnimatedLit(
            relativePath: String,
            options: ModelLoadOptions3D = ModelLoadOptions3D()
        ): AnimatedModelAsset3D {
            return AnimatedModelAsset3D(
                relativePath = relativePath,
                type = AnimatedModelType3D.NODE_ANIMATED_LIT,
                options = options
            )
        }

        fun skinnedTexturedLit(
            relativePath: String,
            options: ModelLoadOptions3D = ModelLoadOptions3D()
        ): AnimatedModelAsset3D {
            return AnimatedModelAsset3D(
                relativePath = relativePath,
                type = AnimatedModelType3D.SKINNED_TEXTURED_LIT,
                options = options
            )
        }
    }
}

data class ResolvedModelAsset3D(
    val asset: ModelAsset3D,
    val assetPath: String,
    val format: ModelFormat3D = ModelLoader3D.detectFormat(assetPath)
)

data class ResolvedAnimatedModelAsset3D(
    val asset: AnimatedModelAsset3D,
    val assetPath: String,
    val format: ModelFormat3D = ModelLoader3D.detectFormat(assetPath)
)

class ModelAssetPathResolver3D(
    val packagedAssetRoot: String = "assets",
    val sourceAssetRoot: String? = null,
    private val resolveAssetPath: (String) -> String = File::resolveAssetPath,
    private val assetExists: (String) -> Boolean = File::isExist
) {
    fun resolve(relativePath: String): String {
        if (isAbsolutePath(relativePath)) {
            return resolveAssetPath(relativePath)
        }

        val packagedPath = resolveAssetPath(joinRoot(packagedAssetRoot, relativePath))
        if (assetExists(packagedPath)) {
            return packagedPath
        }

        val sourcePath = sourceAssetRoot?.let { resolveAssetPath(joinRoot(it, relativePath)) }
        if (sourcePath != null && assetExists(sourcePath)) {
            return sourcePath
        }

        return packagedPath
    }

    fun resolve(asset: ModelAsset3D): ResolvedModelAsset3D {
        return ResolvedModelAsset3D(
            asset = asset,
            assetPath = resolve(asset.relativePath)
        )
    }

    fun resolve(asset: AnimatedModelAsset3D): ResolvedAnimatedModelAsset3D {
        return ResolvedAnimatedModelAsset3D(
            asset = asset,
            assetPath = resolve(asset.relativePath)
        )
    }

    private fun joinRoot(
        root: String,
        relativePath: String
    ): String {
        val normalizedRoot = root.trim().trimEnd('/', '\\')
        val normalizedPath = relativePath.trimStart('/', '\\')
        if (normalizedRoot.isEmpty()) {
            return normalizedPath
        }

        if (normalizedPath == normalizedRoot ||
            normalizedPath.startsWith("$normalizedRoot/") ||
            normalizedPath.startsWith("$normalizedRoot\\")
        ) {
            return normalizedPath
        }

        return "$normalizedRoot/$normalizedPath"
    }

    private fun isAbsolutePath(path: String): Boolean {
        return path.startsWith("/") ||
            (path.length > 2 && path[1] == ':' && (path[2] == '\\' || path[2] == '/'))
    }
}

class ModelAssetSourceLoader3D(
    private val resolver: ModelAssetPathResolver3D = ModelAssetPathResolver3D(),
    private val sourceCache: ModelSourceCache3D? = null,
    private val animatedSourceCache: AnimatedModelSourceCache3D? = null
) {
    fun resolve(asset: ModelAsset3D): ResolvedModelAsset3D {
        return resolver.resolve(asset)
    }

    fun resolve(asset: AnimatedModelAsset3D): ResolvedAnimatedModelAsset3D {
        return resolver.resolve(asset)
    }

    fun inspect(asset: ModelAsset3D): ModelInfo3D {
        val resolved = resolve(asset)
        return sourceCache
            ?.get(resolved.assetPath, asset.options)
            ?.info
            ?: ModelLoader3D.inspect(resolved.assetPath)
    }

    fun inspect(asset: AnimatedModelAsset3D): ModelInfo3D {
        val resolved = resolve(asset)
        return animatedSourceCache
            ?.get(resolved.assetPath, asset.type, asset.options)
            ?.info
            ?: AnimatedModelLoader3D.inspect(resolved.assetPath, asset.type)
    }

    fun loadSource(asset: ModelAsset3D): ParsedModel3D {
        val resolved = resolve(asset)
        sourceCache?.let { cache ->
            return cache.load(
                assetPath = resolved.assetPath,
                options = asset.options
            )
        }
        return ModelLoader3D.loadSource(
            assetPath = resolved.assetPath,
            options = asset.options
        )
    }

    fun loadParsed(asset: ModelAsset3D): ParsedModel3D {
        return loadSource(asset)
    }

    fun loadAnimatedSource(asset: AnimatedModelAsset3D): AnimatedModelSource3D {
        val resolved = resolve(asset)
        animatedSourceCache?.let { cache ->
            return cache.load(
                assetPath = resolved.assetPath,
                type = asset.type,
                options = asset.options
            )
        }
        return AnimatedModelLoader3D.loadSource(
            assetPath = resolved.assetPath,
            type = asset.type,
            options = asset.options
        )
    }
}

data class ModelAssetBundle3D(
    val models: List<ModelAsset3D> = emptyList(),
    val animatedModels: List<AnimatedModelAsset3D> = emptyList()
) {
    val size: Int
        get() = models.size + animatedModels.size

    val isEmpty: Boolean
        get() = size == 0

    init {
        require(models.distinct().size == models.size) {
            "ModelAssetBundle3D contains duplicate static model assets."
        }
        require(animatedModels.distinct().size == animatedModels.size) {
            "ModelAssetBundle3D contains duplicate animated model assets."
        }
    }

    fun preloadSources(loader: ModelAssetSourceLoader3D): ModelAssetSourceBundle3D {
        return ModelAssetSourceBundle3D(
            modelSources = models.associateWithTo(linkedMapOf()) { asset ->
                loader.loadSource(asset)
            },
            animatedModelSources = animatedModels.associateWithTo(linkedMapOf()) { asset ->
                loader.loadAnimatedSource(asset)
            }
        )
    }

    fun preloadSources(loader: ModelAssetLoader3D): ModelAssetSourceBundle3D {
        return ModelAssetSourceBundle3D(
            modelSources = models.associateWithTo(linkedMapOf()) { asset ->
                loader.loadSource(asset)
            },
            animatedModelSources = animatedModels.associateWithTo(linkedMapOf()) { asset ->
                loader.loadAnimatedSource(asset)
            }
        )
    }

    fun load(loader: ModelAssetLoader3D): LoadedModelAssetBundle3D {
        return preloadSources(loader).uploadModels(loader)
    }
}

class ModelAssetSourceBundle3D internal constructor(
    val modelSources: Map<ModelAsset3D, ParsedModel3D>,
    val animatedModelSources: Map<AnimatedModelAsset3D, AnimatedModelSource3D>
) {
    val size: Int
        get() = modelSources.size + animatedModelSources.size

    fun modelSource(asset: ModelAsset3D): ParsedModel3D {
        return modelSources[asset]
            ?: throw IllegalArgumentException("Model asset source was not preloaded: ${asset.relativePath}")
    }

    fun animatedModelSource(asset: AnimatedModelAsset3D): AnimatedModelSource3D {
        return animatedModelSources[asset]
            ?: throw IllegalArgumentException("Animated model asset source was not preloaded: ${asset.relativePath}")
    }

    fun uploadModels(loader: ModelAssetLoader3D): LoadedModelAssetBundle3D {
        return LoadedModelAssetBundle3D(
            sources = this,
            models = modelSources.mapValuesTo(linkedMapOf()) { (_, source) ->
                loader.uploadModel(source)
            },
            animatedModels = animatedModelSources.mapValuesTo(linkedMapOf()) { (_, source) ->
                loader.uploadAnimatedModel(source)
            }
        )
    }
}

class LoadedModelAssetBundle3D internal constructor(
    val sources: ModelAssetSourceBundle3D,
    val models: Map<ModelAsset3D, Model3D>,
    val animatedModels: Map<AnimatedModelAsset3D, AnimatedModel3D>
) {
    val size: Int
        get() = models.size + animatedModels.size

    fun model(asset: ModelAsset3D): Model3D {
        return models[asset]
            ?: throw IllegalArgumentException("Model asset was not loaded: ${asset.relativePath}")
    }

    fun animatedModel(asset: AnimatedModelAsset3D): AnimatedModel3D {
        return animatedModels[asset]
            ?: throw IllegalArgumentException("Animated model asset was not loaded: ${asset.relativePath}")
    }

    fun modelSource(asset: ModelAsset3D): ParsedModel3D {
        return sources.modelSource(asset)
    }

    fun animatedModelSource(asset: AnimatedModelAsset3D): AnimatedModelSource3D {
        return sources.animatedModelSource(asset)
    }
}

class ModelAssetLoader3D(
    private val gpu: GpuContext,
    private val resources: GpuResourceScope3D? = null,
    resolver: ModelAssetPathResolver3D = ModelAssetPathResolver3D(),
    private val textureCache: GpuTextureCache3D? = null,
    sourceCache: ModelSourceCache3D? = null,
    animatedSourceCache: AnimatedModelSourceCache3D? = null,
    private val sourceLoader: ModelAssetSourceLoader3D = ModelAssetSourceLoader3D(
        resolver = resolver,
        sourceCache = sourceCache,
        animatedSourceCache = animatedSourceCache
    )
) {
    fun resolve(asset: ModelAsset3D): ResolvedModelAsset3D {
        return sourceLoader.resolve(asset)
    }

    fun resolve(asset: AnimatedModelAsset3D): ResolvedAnimatedModelAsset3D {
        return sourceLoader.resolve(asset)
    }

    fun inspect(asset: ModelAsset3D): ModelInfo3D {
        return sourceLoader.inspect(asset)
    }

    fun inspect(asset: AnimatedModelAsset3D): ModelInfo3D {
        return sourceLoader.inspect(asset)
    }

    fun loadSource(asset: ModelAsset3D): ParsedModel3D {
        return sourceLoader.loadSource(asset)
    }

    fun loadParsed(asset: ModelAsset3D): ParsedModel3D {
        return sourceLoader.loadParsed(asset)
    }

    fun loadModel(asset: ModelAsset3D): Model3D {
        return uploadModel(loadSource(asset))
    }

    fun uploadModel(source: ParsedModel3D): Model3D {
        return track(
            source.upload(
                gpu = gpu,
                textureCache = textureCache
            )
        )
    }

    fun loadAnimatedSource(asset: AnimatedModelAsset3D): AnimatedModelSource3D {
        return sourceLoader.loadAnimatedSource(asset)
    }

    fun uploadAnimatedModel(source: AnimatedModelSource3D): AnimatedModel3D {
        return track(
            source.upload(
                gpu = gpu,
                textureCache = textureCache
            )
        )
    }

    fun loadAnimatedModel(asset: AnimatedModelAsset3D): AnimatedModel3D {
        return uploadAnimatedModel(loadAnimatedSource(asset))
    }

    fun preloadSources(bundle: ModelAssetBundle3D): ModelAssetSourceBundle3D {
        return bundle.preloadSources(this)
    }

    fun loadBundle(bundle: ModelAssetBundle3D): LoadedModelAssetBundle3D {
        return bundle.load(this)
    }

    fun loadTexture(asset: GpuTextureAsset3D): GpuTexture {
        return textureCache?.load(asset) ?: track(gpu.loadTexture3D(asset))
    }

    fun loadMaterial(descriptor: MaterialDescriptor3D): Material3D {
        return track(descriptor.upload(gpu, textureCache))
    }

    private fun <T : GpuResource3D> track(resource: T): T {
        return resources?.track(resource) ?: resource
    }
}
