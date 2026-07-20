package com.kengine.three

import com.kengine.graphics.Color

enum class ModelFormat3D {
    GLB,
    GLTF,
    OBJ
}

data class ModelLoadOptions3D(
    val normalize: Boolean = true,
    val targetSize: Double = 1.8,
    val placeOnGround: Boolean = true,
    val defaultColor: Color = Color.fromHex("ffffff"),
    val objShadeFaces: Boolean = true,
    val objFlipTextureV: Boolean = true,
    val animatedSkinningMode: AnimatedModelSkinningMode3D = AnimatedModelSkinningMode3D.AUTO
) {
    internal fun toGlbOptions(): GlbMeshLoadOptions {
        return GlbMeshLoadOptions(
            normalize = normalize,
            targetSize = targetSize,
            placeOnGround = placeOnGround,
            defaultColor = defaultColor
        )
    }

    internal fun toObjOptions(): ObjMeshLoadOptions {
        return ObjMeshLoadOptions(
            normalize = normalize,
            targetSize = targetSize,
            defaultColor = defaultColor,
            shadeFaces = objShadeFaces,
            flipTextureV = objFlipTextureV
        )
    }
}

data class ModelInfo3D(
    val assetPath: String,
    val format: ModelFormat3D,
    val vertexCount: Int,
    val meshCount: Int = 0,
    val primitiveCount: Int = 0,
    val materialCount: Int = 0,
    val textureCount: Int = 0,
    val imageCount: Int = 0,
    val skinCount: Int = 0,
    val animationCount: Int = 0,
    val hasTexturedMaterials: Boolean = false,
    val hasSkeleton: Boolean = false,
    val hasAnimations: Boolean = false,
    val animations: List<AnimationClipInfo3D> = emptyList(),
    val skins: List<SkinInfo3D> = emptyList(),
    val skinningSupport: ModelSkinningSupportInfo3D = ModelSkinningSupportInfo3D()
)

data class AnimationClipInfo3D(
    val name: String,
    val durationSeconds: Double,
    val channelCount: Int,
    val channels: List<AnimationChannelInfo3D> = emptyList()
)

data class AnimationChannelInfo3D(
    val nodeIndex: Int,
    val nodeName: String?,
    val path: String,
    val interpolation: String,
    val keyframeCount: Int
)

data class SkinInfo3D(
    val name: String?,
    val jointCount: Int,
    val skeletonRootNodeIndex: Int?,
    val jointNames: List<String?>
)

data class ModelSkinningSupportInfo3D(
    val skinnedNodeCount: Int = 0,
    val supportedPrimitiveCount: Int = 0,
    val nonTrianglePrimitiveCount: Int = 0,
    val missingJointOrWeightPrimitiveCount: Int = 0,
    val missingMeshReferenceCount: Int = 0,
    val missingSkinReferenceCount: Int = 0
) {
    val unsupportedPrimitiveCount: Int
        get() = nonTrianglePrimitiveCount +
            missingJointOrWeightPrimitiveCount +
            missingMeshReferenceCount +
            missingSkinReferenceCount

    val hasSupportedPrimitives: Boolean
        get() = supportedPrimitiveCount > 0
}

data class ParsedModel3D(
    val assetPath: String,
    val format: ModelFormat3D,
    val options: ModelLoadOptions3D,
    val info: ModelInfo3D,
    val litVertices: List<LitVertex3D>,
    val parts: List<ModelPartSource3D> = listOf(ModelPartSource3D.lit(litVertices))
) {
    fun createTerrainCollider(): TerrainMeshCollider3D {
        return TerrainMeshCollider3D.fromLitVertices(litVertices)
    }

    fun createStaticCollider(): StaticMeshCollider3D {
        return StaticMeshCollider3D.fromLitVertices(litVertices)
    }

    fun upload(
        gpu: GpuContext,
        textureCache: GpuTextureCache3D? = null
    ): Model3D {
        return ModelLoader3D.upload(this, gpu, textureCache)
    }
}

sealed class ModelPartSource3D {
    abstract val materialDescriptor: MaterialDescriptor3D
    abstract val vertexCount: Int

    internal abstract fun upload(
        gpu: GpuContext,
        textureCache: GpuTextureCache3D?
    ): ModelPart3D

    data class Lit(
        val vertices: List<LitVertex3D>,
        override val materialDescriptor: MaterialDescriptor3D = MaterialDescriptor3D.solid()
    ) : ModelPartSource3D() {
        override val vertexCount: Int
            get() = vertices.size

        init {
            require(vertices.isNotEmpty()) {
                "Lit model part sources require at least one vertex."
            }
            require(!materialDescriptor.hasTexture) {
                "Lit model part sources cannot use textured material descriptors."
            }
        }

        override fun upload(
            gpu: GpuContext,
            textureCache: GpuTextureCache3D?
        ): ModelPart3D {
            val mesh = LitGpuMesh.create(gpu, vertices)
            val material = try {
                materialDescriptor.upload(gpu, textureCache)
            } catch (e: Throwable) {
                mesh.cleanup()
                throw e
            }
            return try {
                ModelPart3D.lit(
                    mesh = mesh,
                    material = material
                )
            } catch (e: Throwable) {
                mesh.cleanup()
                material.cleanup()
                throw e
            }
        }
    }

    data class TexturedLit(
        val vertices: List<TexturedLitVertex3D>,
        override val materialDescriptor: MaterialDescriptor3D
    ) : ModelPartSource3D() {
        override val vertexCount: Int
            get() = vertices.size

        init {
            require(vertices.isNotEmpty()) {
                "Textured lit model part sources require at least one vertex."
            }
            require(materialDescriptor.hasTexture) {
                "Textured lit model part sources require a textured material descriptor."
            }
        }

        override fun upload(
            gpu: GpuContext,
            textureCache: GpuTextureCache3D?
        ): ModelPart3D {
            val mesh = TexturedLitGpuMesh.create(gpu, vertices)
            val material = try {
                materialDescriptor.upload(gpu, textureCache)
            } catch (e: Throwable) {
                mesh.cleanup()
                throw e
            }
            return try {
                ModelPart3D.texturedLit(
                    mesh = mesh,
                    material = material
                )
            } catch (e: Throwable) {
                mesh.cleanup()
                material.cleanup()
                throw e
            }
        }
    }

    companion object {
        fun lit(
            vertices: List<LitVertex3D>,
            materialDescriptor: MaterialDescriptor3D = MaterialDescriptor3D.solid()
        ): ModelPartSource3D {
            return Lit(vertices, materialDescriptor)
        }

        fun texturedLit(
            vertices: List<TexturedLitVertex3D>,
            materialDescriptor: MaterialDescriptor3D
        ): ModelPartSource3D {
            return TexturedLit(vertices, materialDescriptor)
        }
    }
}

data class Material3D(
    val name: String? = null,
    val baseColor: Color = Color.fromHex("ffffff"),
    val texture: GpuTexture? = null,
    val textureOwnership: GpuResourceOwnership3D = GpuResourceOwnership3D.OWNED
) : GpuResource3D {
    val hasTexture: Boolean
        get() = texture != null

    override fun cleanup() {
        textureOwnership.cleanupIfOwned(texture) { it.cleanup() }
    }

    companion object {
        fun solid(
            color: Color = Color.fromHex("ffffff"),
            name: String? = null
        ): Material3D {
            return Material3D(name = name, baseColor = color)
        }

        fun textured(
            texture: GpuTexture,
            color: Color = Color.fromHex("ffffff"),
            name: String? = null,
            textureOwnership: GpuResourceOwnership3D = GpuResourceOwnership3D.OWNED
        ): Material3D {
            return Material3D(
                name = name,
                baseColor = color,
                texture = texture,
                textureOwnership = textureOwnership
            )
        }

        fun borrowedTexture(
            texture: GpuTexture,
            color: Color = Color.fromHex("ffffff"),
            name: String? = null
        ): Material3D {
            return textured(
                texture = texture,
                color = color,
                name = name,
                textureOwnership = GpuResourceOwnership3D.BORROWED
            )
        }
    }
}

class ModelRenderer3D(
    val litRenderer: LitMeshRenderer3D,
    val texturedLitRenderer: TexturedLitMeshRenderer3D
) {
    fun draw(
        model: Model3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        model.draw(this, frame, transform, camera, light)
    }
}

class Model3D private constructor(
    val info: ModelInfo3D,
    val parts: List<ModelPart3D>,
    private val ownedResources: List<GpuResource3D> = emptyList()
) : GpuResource3D {
    fun draw(
        renderer: ModelRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        parts.forEach { part ->
            part.draw(renderer, frame, transform, camera, light)
        }
    }

    override fun cleanup() {
        var firstError: Throwable? = null
        parts.asReversed().forEach { part ->
            try {
                part.cleanup()
            } catch (e: Throwable) {
                if (firstError == null) {
                    firstError = e
                }
            }
        }
        ownedResources.asReversed().forEach { resource ->
            try {
                resource.cleanup()
            } catch (e: Throwable) {
                if (firstError == null) {
                    firstError = e
                }
            }
        }
        firstError?.let { throw it }
    }

    companion object {
        fun load(
            gpu: GpuContext,
            assetPath: String,
            options: ModelLoadOptions3D = ModelLoadOptions3D(),
            textureCache: GpuTextureCache3D? = null,
            sourceCache: ModelSourceCache3D? = null
        ): Model3D {
            return ModelLoader3D.load(gpu, assetPath, options, textureCache, sourceCache)
        }

        internal fun fromParts(
            info: ModelInfo3D,
            parts: List<ModelPart3D>,
            ownedResources: List<GpuResource3D> = emptyList()
        ): Model3D {
            require(parts.isNotEmpty()) {
                "Model3D requires at least one renderable part."
            }
            return Model3D(info, parts, ownedResources)
        }
    }
}

class ModelPart3D private constructor(
    val material: Material3D,
    private val litMesh: LitGpuMesh? = null,
    private val texturedLitMesh: TexturedLitGpuMesh? = null
) : GpuResource3D {
    val vertexCount: Int =
        texturedLitMesh?.vertexCount?.toInt()
            ?: litMesh?.vertexCount?.toInt()
            ?: 0

    fun draw(
        renderer: ModelRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        when {
            texturedLitMesh != null -> {
                val texture = material.texture
                    ?: throw IllegalStateException("Textured model parts require a material texture.")
                renderer.texturedLitRenderer.draw(frame, texturedLitMesh, texture, transform, camera, light)
            }

            litMesh != null -> renderer.litRenderer.draw(frame, litMesh, transform, camera, light)

            else -> throw IllegalStateException("Model part has no renderable mesh.")
        }
    }

    override fun cleanup() {
        texturedLitMesh?.cleanup()
        litMesh?.cleanup()
        material.cleanup()
    }

    companion object {
        fun lit(
            mesh: LitGpuMesh,
            material: Material3D = Material3D.solid()
        ): ModelPart3D {
            return ModelPart3D(material = material, litMesh = mesh)
        }

        fun texturedLit(
            mesh: TexturedLitGpuMesh,
            material: Material3D
        ): ModelPart3D {
            require(material.texture != null) {
                "Textured lit model parts require a texture material."
            }
            return ModelPart3D(material = material, texturedLitMesh = mesh)
        }
    }
}

object ModelLoader3D {
    fun load(
        gpu: GpuContext,
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D(),
        textureCache: GpuTextureCache3D? = null,
        sourceCache: ModelSourceCache3D? = null
    ): Model3D {
        val source = sourceCache?.load(assetPath, options) ?: loadSource(assetPath, options)
        return upload(source, gpu, textureCache)
    }

    fun loadSource(
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): ParsedModel3D {
        return when (val format = detectFormat(assetPath)) {
            ModelFormat3D.GLB -> loadGlbSource(assetPath, options, format)
            ModelFormat3D.GLTF -> loadGlbSource(assetPath, options, format)
            ModelFormat3D.OBJ -> loadObjSource(assetPath, options, format)
        }
    }

    fun loadParsed(
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): ParsedModel3D {
        return loadSource(assetPath, options)
    }

    internal fun upload(
        source: ParsedModel3D,
        gpu: GpuContext,
        textureCache: GpuTextureCache3D? = null
    ): Model3D {
        val textureCacheScope =
            if (source.parts.any { it.materialDescriptor.hasTexture }) {
                gpu.resolveTextureCache3D(textureCache)
            } else {
                null
            }
        val parts = mutableListOf<ModelPart3D>()
        return try {
            source.parts.forEach { partSource ->
                parts += partSource.upload(gpu, textureCacheScope?.cache)
            }
            Model3D.fromParts(
                info = source.info,
                parts = parts,
                ownedResources = textureCacheScope?.ownedResources.orEmpty()
            )
        } catch (e: Throwable) {
            parts.asReversed().forEach { it.cleanup() }
            textureCacheScope?.ownedResources.orEmpty().asReversed().forEach { it.cleanup() }
            throw e
        }
    }

    fun inspect(assetPath: String): ModelInfo3D {
        return when (val format = detectFormat(assetPath)) {
            ModelFormat3D.GLB -> inspectGlb(assetPath, vertexCount = 0)
            ModelFormat3D.GLTF -> inspectGlb(assetPath, vertexCount = 0)
            ModelFormat3D.OBJ -> ModelInfo3D(assetPath = assetPath, format = format, vertexCount = 0)
        }
    }

    fun detectFormat(assetPath: String): ModelFormat3D {
        return when (assetPath.substringAfterLast('.', "").lowercase()) {
            "glb" -> ModelFormat3D.GLB
            "gltf" -> ModelFormat3D.GLTF
            "obj" -> ModelFormat3D.OBJ
            else -> throw IllegalArgumentException("Unsupported 3D model format for asset: $assetPath")
        }
    }

    private fun loadGlbSource(
        assetPath: String,
        options: ModelLoadOptions3D,
        format: ModelFormat3D
    ): ParsedModel3D {
        val source = GlbMeshLoader.loadTexturedLitSource(
            assetPath = assetPath,
            options = options.toGlbOptions()
        )
        return ParsedModel3D(
            assetPath = assetPath,
            format = format,
            options = options,
            info = source.info.toModelInfo3D(assetPath, source.litVertices.size, format),
            litVertices = source.litVertices,
            parts = source.parts
        )
    }

    private fun loadObjSource(
        assetPath: String,
        options: ModelLoadOptions3D,
        format: ModelFormat3D
    ): ParsedModel3D {
        val vertices = ObjMeshLoader.loadLitVertices(assetPath, options.toObjOptions())
        return ParsedModel3D(
            assetPath = assetPath,
            format = format,
            options = options,
            info = ModelInfo3D(
                assetPath = assetPath,
                format = format,
                vertexCount = vertices.size,
                primitiveCount = vertices.size / 3
            ),
            litVertices = vertices,
            parts = listOf(ModelPartSource3D.lit(vertices))
        )
    }

    private fun inspectGlb(
        assetPath: String,
        vertexCount: Int
    ): ModelInfo3D {
        return GlbMeshLoader.inspect(assetPath).toModelInfo3D(assetPath, vertexCount, detectFormat(assetPath))
    }
}

internal fun GlbModelInfo.toModelInfo3D(
    assetPath: String,
    vertexCount: Int,
    format: ModelFormat3D = ModelFormat3D.GLB
): ModelInfo3D {
    return ModelInfo3D(
        assetPath = assetPath,
        format = format,
        vertexCount = vertexCount,
        meshCount = meshCount,
        primitiveCount = primitiveCount,
        materialCount = materialCount,
        textureCount = textureCount,
        imageCount = imageCount,
        skinCount = skinCount,
        animationCount = animationCount,
        hasTexturedMaterials = hasTexturedMaterials,
        hasSkeleton = hasSkeleton,
        hasAnimations = hasAnimations,
        animations = animations.map { it.toModelInfo3D() },
        skins = skins.map { it.toModelInfo3D() },
        skinningSupport = skinningSupport.toModelInfo3D()
    )
}

private fun GlbAnimationClipInfo.toModelInfo3D(): AnimationClipInfo3D {
    return AnimationClipInfo3D(
        name = name,
        durationSeconds = durationSeconds,
        channelCount = channelCount,
        channels = channels.map { it.toModelInfo3D() }
    )
}

private fun GlbAnimationChannelInfo.toModelInfo3D(): AnimationChannelInfo3D {
    return AnimationChannelInfo3D(
        nodeIndex = nodeIndex,
        nodeName = nodeName,
        path = path,
        interpolation = interpolation,
        keyframeCount = keyframeCount
    )
}

private fun GlbSkinInfo.toModelInfo3D(): SkinInfo3D {
    return SkinInfo3D(
        name = name,
        jointCount = jointCount,
        skeletonRootNodeIndex = skeletonRootNodeIndex,
        jointNames = jointNames
    )
}

private fun GlbSkinningSupportInfo.toModelInfo3D(): ModelSkinningSupportInfo3D {
    return ModelSkinningSupportInfo3D(
        skinnedNodeCount = skinnedNodeCount,
        supportedPrimitiveCount = supportedPrimitiveCount,
        nonTrianglePrimitiveCount = nonTrianglePrimitiveCount,
        missingJointOrWeightPrimitiveCount = missingJointOrWeightPrimitiveCount,
        missingMeshReferenceCount = missingMeshReferenceCount,
        missingSkinReferenceCount = missingSkinReferenceCount
    )
}
