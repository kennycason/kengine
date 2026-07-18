package com.kengine.three

import com.kengine.graphics.Color

enum class ModelFormat3D {
    GLB,
    OBJ
}

data class ModelLoadOptions3D(
    val normalize: Boolean = true,
    val targetSize: Double = 1.8,
    val placeOnGround: Boolean = true,
    val defaultColor: Color = Color.fromHex("ffffff"),
    val objShadeFaces: Boolean = true,
    val objFlipTextureV: Boolean = true
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
    val skins: List<SkinInfo3D> = emptyList()
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

data class ParsedModel3D(
    val assetPath: String,
    val format: ModelFormat3D,
    val options: ModelLoadOptions3D,
    val info: ModelInfo3D,
    val litVertices: List<LitVertex3D>
) {
    fun createTerrainCollider(): TerrainMeshCollider3D {
        return TerrainMeshCollider3D.fromLitVertices(litVertices)
    }

    fun createStaticCollider(): StaticMeshCollider3D {
        return StaticMeshCollider3D.fromLitVertices(litVertices)
    }

    fun upload(gpu: GpuContext): Model3D {
        return Model3D.load(gpu, assetPath, options)
    }
}

data class Material3D(
    val name: String? = null,
    val baseColor: Color = Color.fromHex("ffffff"),
    val texture: GpuTexture? = null
) : GpuResource3D {
    val hasTexture: Boolean
        get() = texture != null

    override fun cleanup() {
        texture?.cleanup()
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
            name: String? = null
        ): Material3D {
            return Material3D(name = name, baseColor = color, texture = texture)
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
    val parts: List<ModelPart3D>
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
        parts.asReversed().forEach { it.cleanup() }
    }

    companion object {
        fun load(
            gpu: GpuContext,
            assetPath: String,
            options: ModelLoadOptions3D = ModelLoadOptions3D()
        ): Model3D {
            return ModelLoader3D.load(gpu, assetPath, options)
        }

        internal fun fromParts(
            info: ModelInfo3D,
            parts: List<ModelPart3D>
        ): Model3D {
            require(parts.isNotEmpty()) {
                "Model3D requires at least one renderable part."
            }
            return Model3D(info, parts)
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
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): Model3D {
        return when (val format = detectFormat(assetPath)) {
            ModelFormat3D.GLB -> loadGlb(gpu, assetPath, options)
            ModelFormat3D.OBJ -> loadObj(gpu, assetPath, options, format)
        }
    }

    fun loadParsed(
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): ParsedModel3D {
        return when (val format = detectFormat(assetPath)) {
            ModelFormat3D.GLB -> {
                val litVertices = GlbMeshLoader.loadLitVertices(assetPath, options.toGlbOptions())
                ParsedModel3D(
                    assetPath = assetPath,
                    format = format,
                    options = options,
                    info = inspectGlb(assetPath, litVertices.size),
                    litVertices = litVertices
                )
            }

            ModelFormat3D.OBJ -> {
                val litVertices = ObjMeshLoader.loadLitVertices(assetPath, options.toObjOptions())
                ParsedModel3D(
                    assetPath = assetPath,
                    format = format,
                    options = options,
                    info = ModelInfo3D(
                        assetPath = assetPath,
                        format = format,
                        vertexCount = litVertices.size,
                        primitiveCount = litVertices.size / 3
                    ),
                    litVertices = litVertices
                )
            }
        }
    }

    fun inspect(assetPath: String): ModelInfo3D {
        return when (val format = detectFormat(assetPath)) {
            ModelFormat3D.GLB -> inspectGlb(assetPath, vertexCount = 0)
            ModelFormat3D.OBJ -> ModelInfo3D(assetPath = assetPath, format = format, vertexCount = 0)
        }
    }

    fun detectFormat(assetPath: String): ModelFormat3D {
        return when (assetPath.substringAfterLast('.', "").lowercase()) {
            "glb" -> ModelFormat3D.GLB
            "obj" -> ModelFormat3D.OBJ
            else -> throw IllegalArgumentException("Unsupported 3D model format for asset: $assetPath")
        }
    }

    private fun loadGlb(
        gpu: GpuContext,
        assetPath: String,
        options: ModelLoadOptions3D
    ): Model3D {
        val glb = GlbMeshLoader.loadTexturedLit(gpu, assetPath, options.toGlbOptions())
        val parts = glb.parts.mapIndexed { index, part ->
            ModelPart3D.texturedLit(
                mesh = part.mesh,
                material = Material3D.textured(
                    texture = part.texture,
                    name = "glb-material-$index"
                )
            )
        }
        return try {
            Model3D.fromParts(
                info = inspectGlb(
                    assetPath = assetPath,
                    vertexCount = parts.sumOf { it.vertexCount }
                ),
                parts = parts
            )
        } catch (e: Throwable) {
            parts.asReversed().forEach { it.cleanup() }
            throw e
        }
    }

    private fun loadObj(
        gpu: GpuContext,
        assetPath: String,
        options: ModelLoadOptions3D,
        format: ModelFormat3D
    ): Model3D {
        val vertices = ObjMeshLoader.loadLitVertices(assetPath, options.toObjOptions())
        val mesh = LitGpuMesh.create(gpu, vertices)
        val parts = listOf(ModelPart3D.lit(mesh))
        return try {
            Model3D.fromParts(
                info = ModelInfo3D(
                    assetPath = assetPath,
                    format = format,
                    vertexCount = vertices.size,
                    primitiveCount = vertices.size / 3
                ),
                parts = parts
            )
        } catch (e: Throwable) {
            parts.asReversed().forEach { it.cleanup() }
            throw e
        }
    }

    private fun inspectGlb(
        assetPath: String,
        vertexCount: Int
    ): ModelInfo3D {
        val info = GlbMeshLoader.inspect(assetPath)
        return ModelInfo3D(
            assetPath = assetPath,
            format = ModelFormat3D.GLB,
            vertexCount = vertexCount,
            meshCount = info.meshCount,
            primitiveCount = info.primitiveCount,
            materialCount = info.materialCount,
            textureCount = info.textureCount,
            imageCount = info.imageCount,
            skinCount = info.skinCount,
            animationCount = info.animationCount,
            hasTexturedMaterials = info.hasTexturedMaterials,
            hasSkeleton = info.hasSkeleton,
            hasAnimations = info.hasAnimations,
            animations = info.animations.map { it.toModelInfo() },
            skins = info.skins.map { it.toModelInfo() }
        )
    }

    private fun GlbAnimationClipInfo.toModelInfo(): AnimationClipInfo3D {
        return AnimationClipInfo3D(
            name = name,
            durationSeconds = durationSeconds,
            channelCount = channelCount,
            channels = channels.map { it.toModelInfo() }
        )
    }

    private fun GlbAnimationChannelInfo.toModelInfo(): AnimationChannelInfo3D {
        return AnimationChannelInfo3D(
            nodeIndex = nodeIndex,
            nodeName = nodeName,
            path = path,
            interpolation = interpolation,
            keyframeCount = keyframeCount
        )
    }

    private fun GlbSkinInfo.toModelInfo(): SkinInfo3D {
        return SkinInfo3D(
            name = name,
            jointCount = jointCount,
            skeletonRootNodeIndex = skeletonRootNodeIndex,
            jointNames = jointNames
        )
    }

}
