package com.kengine.three

import com.kengine.file.File
import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.cos
import kotlin.math.sin
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import kotlin.math.sqrt

data class GlbMeshLoadOptions(
    val normalize: Boolean = true,
    val targetSize: Double = 18.0,
    val placeOnGround: Boolean = true,
    val defaultColor: Color = Color.fromHex("8fbf7a")
)

data class GlbModelInfo(
    val skinCount: Int,
    val animationCount: Int,
    val meshCount: Int,
    val primitiveCount: Int,
    val skinnedPrimitiveCount: Int,
    val materialCount: Int,
    val textureCount: Int,
    val imageCount: Int,
    val textureSlotUsage: MaterialTextureSlotUsage3D,
    val hasTexturedMaterials: Boolean,
    val animations: List<GlbAnimationClipInfo> = emptyList(),
    val skins: List<GlbSkinInfo> = emptyList(),
    val skinningSupport: GlbSkinningSupportInfo = GlbSkinningSupportInfo(),
    val hasSkeleton: Boolean = skinCount > 0,
    val hasAnimations: Boolean = animationCount > 0
)

data class GlbAnimationClipInfo(
    val name: String,
    val durationSeconds: Double,
    val channelCount: Int,
    val channels: List<GlbAnimationChannelInfo>
)

data class GlbAnimationChannelInfo(
    val nodeIndex: Int,
    val nodeName: String?,
    val path: String,
    val interpolation: String,
    val keyframeCount: Int
)

data class GlbSkinInfo(
    val name: String?,
    val jointCount: Int,
    val skeletonRootNodeIndex: Int?,
    val jointNames: List<String?>,
    val inverseBindMatrixCount: Int
)

data class GlbSkinningSupportInfo(
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

    internal fun unsupportedSummary(): String {
        val reasons = mutableListOf<String>()
        if (skinnedNodeCount == 0) {
            reasons += "no mesh nodes reference a skin"
        }
        if (nonTrianglePrimitiveCount > 0) {
            reasons += "$nonTrianglePrimitiveCount skinned primitive(s) are not triangle lists"
        }
        if (missingJointOrWeightPrimitiveCount > 0) {
            reasons += "$missingJointOrWeightPrimitiveCount triangle primitive(s) are missing JOINTS_0 or WEIGHTS_0"
        }
        if (missingMeshReferenceCount > 0) {
            reasons += "$missingMeshReferenceCount skinned node(s) reference missing meshes"
        }
        if (missingSkinReferenceCount > 0) {
            reasons += "$missingSkinReferenceCount skinned node(s) reference missing skins"
        }
        return reasons.joinToString("; ").ifEmpty { "no supported skinned triangle primitives were found" }
    }
}

class GlbAnimatedLitModel internal constructor(
    private val parts: List<GlbAnimatedLitMeshPart>,
    private val nodes: List<ModelNode3D>,
    private val sceneNodeIndices: List<Int>,
    private val animations: List<ModelAnimationClip3D>,
    private val normalizationMatrix: Mat4
) {
    val clips: List<GlbAnimationClipInfo> = animations.map { it.info }

    fun draw(
        renderer: LitMeshRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D(),
        clipIndex: Int = 0,
        timeSeconds: Double = 0.0
    ) {
        val modelMatrix = transform.matrix() * normalizationMatrix
        val nodeWorldMatrices = sampleWorldMatrices(clipIndex, timeSeconds)
        parts.forEach { part ->
            renderer.draw(
                frame = frame,
                mesh = part.mesh,
                modelMatrix = modelMatrix * nodeWorldMatrices[part.nodeIndex].toMat4(),
                camera = camera,
                light = light
            )
        }
    }

    fun cleanup() {
        parts.map { it.mesh }.distinct().forEach { it.cleanup() }
    }

    private fun sampleWorldMatrices(
        clipIndex: Int,
        timeSeconds: Double
    ): List<ModelMatrix3D> {
        return sampleModelNodeWorldMatrices3D(
            nodes = nodes,
            sceneNodeIndices = sceneNodeIndices,
            animations = animations,
            clipIndex = clipIndex,
            timeSeconds = timeSeconds
        )
    }
}

internal data class GlbAnimatedLitMeshPart(
    val nodeIndex: Int,
    val mesh: LitGpuMesh
)

class GlbTexturedLitModel(
    val parts: List<GlbTexturedLitMeshPart>,
    private val ownedResources: List<GpuResource3D> = emptyList()
) : GpuResource3D {
    private var cleanedUp = false

    fun draw(
        renderer: TexturedLitMeshRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        parts.forEach { part ->
            renderer.draw(
                frame = frame,
                mesh = part.mesh,
                texture = part.texture,
                transform = transform,
                camera = camera,
                light = light,
                normalTexture = part.normalTexture,
                useNormalTexture = part.material.hasAuthoredNormalTexture
            )
        }
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        cleanupGlbResources3D(parts, ownedResources)
    }
}

data class GlbTexturedLitMeshPart(
    val mesh: TexturedLitGpuMesh,
    val material: Material3D
) : GpuResource3D {
    val texture: GpuTexture
        get() = material.texture ?: throw IllegalStateException("GLB textured lit mesh part requires a material texture.")
    val normalTexture: GpuTexture
        get() = material.normalTexture ?: throw IllegalStateException("GLB textured lit mesh part requires a material normal texture.")

    override fun cleanup() {
        mesh.cleanup()
        material.cleanup()
    }
}

internal data class GlbTexturedLitLoad3D(
    val parts: List<GlbTexturedLitMeshPart>,
    val ownedResources: List<GpuResource3D>
)

internal data class GlbTexturedLitSource3D(
    val info: GlbModelInfo,
    val litVertices: List<LitVertex3D>,
    val parts: List<ModelPartSource3D.TexturedLit>
)

class GlbSkinnedTexturedLitModel internal constructor(
    private val gpu: GpuContext,
    private val parts: List<GlbSkinnedTexturedLitMeshPart>,
    private val nodes: List<ModelNode3D>,
    private val sceneNodeIndices: List<Int>,
    private val skins: List<ModelSkin3D>,
    private val animations: List<ModelAnimationClip3D>,
    private val normalizationMatrix: Mat4,
    private val ownedResources: List<GpuResource3D> = emptyList()
) {
    val clips: List<GlbAnimationClipInfo> = animations.map { it.info }
    val maxSkinJointCount: Int = skins.maxOf { it.joints.size }
    private var sharedInstance: GlbSkinnedTexturedLitModelInstance? = null
    private var cleanedUp = false

    fun updatePose(
        clipIndex: Int = 0,
        timeSeconds: Double = 0.0
    ) {
        requireSharedInstance().updatePose(clipIndex, timeSeconds)
    }

    fun draw(
        renderer: TexturedLitMeshRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        requireSharedInstance().draw(renderer, frame, transform, camera, light)
    }

    fun createInstance(): GlbSkinnedTexturedLitModelInstance {
        check(!cleanedUp) {
            "GlbSkinnedTexturedLitModel has already been cleaned up."
        }

        val instanceParts = mutableListOf<GlbSkinnedTexturedLitModelInstancePart>()
        try {
            parts.forEach { part ->
                instanceParts += GlbSkinnedTexturedLitModelInstancePart(
                    nodeIndex = part.nodeIndex,
                    skinIndex = part.skinIndex,
                    mesh = TexturedLitGpuMesh.create(gpu, part.restVertices),
                    texture = part.texture,
                    normalTexture = part.normalTexture,
                    useNormalTexture = part.material.hasAuthoredNormalTexture,
                    sourceVertices = part.sourceVertices
                )
            }
        } catch (e: Throwable) {
            instanceParts.forEach { it.cleanup() }
            throw e
        }

        return GlbSkinnedTexturedLitModelInstance(
            parts = instanceParts,
            nodes = nodes,
            sceneNodeIndices = sceneNodeIndices,
            skins = skins,
            animations = animations,
            normalizationMatrix = normalizationMatrix
        )
    }

    fun createGpuSkinnedInstance(): GlbGpuSkinnedTexturedLitModelInstance {
        check(!cleanedUp) {
            "GlbSkinnedTexturedLitModel has already been cleaned up."
        }
        require(maxSkinJointCount <= SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS) {
            "GPU joint-palette skinning supports at most ${SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS} joints per skin, " +
                "but this GLB has a skin with $maxSkinJointCount joints. Use ${AnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER} for this asset."
        }

        val instanceParts = mutableListOf<GlbGpuSkinnedTexturedLitModelInstancePart>()
        try {
            parts.forEach { part ->
                instanceParts += GlbGpuSkinnedTexturedLitModelInstancePart(
                    skinIndex = part.skinIndex,
                    mesh = SkinnedTexturedLitGpuMesh.create(gpu, part.skinnedVertices),
                    texture = part.texture,
                    normalTexture = part.normalTexture,
                    useNormalTexture = part.material.hasAuthoredNormalTexture
                )
            }
        } catch (e: Throwable) {
            instanceParts.forEach { it.cleanup() }
            throw e
        }

        return GlbGpuSkinnedTexturedLitModelInstance(
            parts = instanceParts,
            nodes = nodes,
            sceneNodeIndices = sceneNodeIndices,
            skins = skins,
            animations = animations,
            normalizationMatrix = normalizationMatrix
        )
    }

    fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        val instance = sharedInstance
        sharedInstance = null
        val resources = mutableListOf<GpuResource3D>()
        instance?.let { resources += it }
        resources += parts
        cleanupGlbResources3D(
            resources = resources,
            ownedResources = ownedResources
        )
    }

    private fun requireSharedInstance(): GlbSkinnedTexturedLitModelInstance {
        return sharedInstance ?: createInstance().also { sharedInstance = it }
    }
}

internal data class GlbSkinnedTexturedLitMeshPart(
    val nodeIndex: Int,
    val skinIndex: Int,
    val material: Material3D,
    val restVertices: List<TexturedLitVertex3D>,
    val skinnedVertices: List<SkinnedTexturedLitVertex3D>,
    val sourceVertices: List<SkinnedTexturedLitVertexSource3D>
) : GpuResource3D {
    val texture: GpuTexture
        get() = material.texture ?: throw IllegalStateException("GLB skinned textured lit mesh part requires a material texture.")
    val normalTexture: GpuTexture
        get() = material.normalTexture ?: throw IllegalStateException("GLB skinned textured lit mesh part requires a material normal texture.")

    override fun cleanup() {
        material.cleanup()
    }
}

class GlbGpuSkinnedTexturedLitModelInstance internal constructor(
    private val parts: List<GlbGpuSkinnedTexturedLitModelInstancePart>,
    private val nodes: List<ModelNode3D>,
    private val sceneNodeIndices: List<Int>,
    private val skins: List<ModelSkin3D>,
    private val animations: List<ModelAnimationClip3D>,
    private val normalizationMatrix: Mat4
) : GpuResource3D {
    private var skinMatricesBySkin: List<List<Mat4>> = sampleSkinMatrices(emptyList(), 0, 0.0)
    private var cleanedUp = false

    fun updatePose(
        clipIndex: Int = 0,
        timeSeconds: Double = 0.0
    ) {
        check(!cleanedUp) {
            "GlbGpuSkinnedTexturedLitModelInstance has already been cleaned up."
        }

        skinMatricesBySkin = sampleSkinMatrices(
            animationClips = animations,
            clipIndex = clipIndex,
            timeSeconds = timeSeconds
        )
    }

    private fun sampleSkinMatrices(
        animationClips: List<ModelAnimationClip3D>,
        clipIndex: Int,
        timeSeconds: Double
    ): List<List<Mat4>> {
        val nodeWorldMatrices = sampleModelNodeWorldMatrices3D(
            nodes = nodes,
            sceneNodeIndices = sceneNodeIndices,
            animations = animationClips,
            clipIndex = clipIndex,
            timeSeconds = timeSeconds
        )
        return skins.map { skin ->
            skinMatricesForModelSkin3D(skin, nodeWorldMatrices).map { it.toMat4() }
        }
    }

    fun draw(
        renderer: SkinnedTexturedLitMeshRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        check(!cleanedUp) {
            "GlbGpuSkinnedTexturedLitModelInstance has already been cleaned up."
        }

        val modelMatrix = transform.matrix() * normalizationMatrix
        parts.forEach { part ->
            renderer.draw(
                frame = frame,
                mesh = part.mesh,
                texture = part.texture,
                modelMatrix = modelMatrix,
                camera = camera,
                skinMatrices = skinMatricesBySkin.getOrElse(part.skinIndex) { listOf(Mat4.identity()) },
                light = light,
                normalTexture = part.normalTexture,
                useNormalTexture = part.useNormalTexture
            )
        }
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        parts.forEach { it.cleanup() }
    }
}

internal data class GlbGpuSkinnedTexturedLitModelInstancePart(
    val skinIndex: Int,
    val mesh: SkinnedTexturedLitGpuMesh,
    val texture: GpuTexture,
    val normalTexture: GpuTexture,
    val useNormalTexture: Boolean
) {
    fun cleanup() {
        mesh.cleanup()
    }
}

class GlbSkinnedTexturedLitModelInstance internal constructor(
    private val parts: List<GlbSkinnedTexturedLitModelInstancePart>,
    private val nodes: List<ModelNode3D>,
    private val sceneNodeIndices: List<Int>,
    private val skins: List<ModelSkin3D>,
    private val animations: List<ModelAnimationClip3D>,
    private val normalizationMatrix: Mat4
) : GpuResource3D {
    private var cleanedUp = false

    fun updatePose(
        clipIndex: Int = 0,
        timeSeconds: Double = 0.0
    ) {
        check(!cleanedUp) {
            "GlbSkinnedTexturedLitModelInstance has already been cleaned up."
        }

        val nodeWorldMatrices = sampleModelNodeWorldMatrices3D(
            nodes = nodes,
            sceneNodeIndices = sceneNodeIndices,
            animations = animations,
            clipIndex = clipIndex,
            timeSeconds = timeSeconds
        )
        parts.forEach { part ->
            val skin = skins.getOrNull(part.skinIndex) ?: return@forEach
            val skinMatrices = skinMatricesForModelSkin3D(skin, nodeWorldMatrices)
            part.mesh.update(part.sourceVertices.map { vertex -> vertex.toTexturedVertex(skinMatrices) })
        }
    }

    fun draw(
        renderer: TexturedLitMeshRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        check(!cleanedUp) {
            "GlbSkinnedTexturedLitModelInstance has already been cleaned up."
        }

        val modelMatrix = transform.matrix() * normalizationMatrix
        parts.forEach { part ->
            renderer.draw(
                frame = frame,
                mesh = part.mesh,
                texture = part.texture,
                modelMatrix = modelMatrix,
                camera = camera,
                light = light,
                normalTexture = part.normalTexture,
                useNormalTexture = part.useNormalTexture
            )
        }
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        parts.forEach { it.cleanup() }
    }
}

internal data class GlbSkinnedTexturedLitModelInstancePart(
    val nodeIndex: Int,
    val skinIndex: Int,
    val mesh: TexturedLitGpuMesh,
    val texture: GpuTexture,
    val normalTexture: GpuTexture,
    val useNormalTexture: Boolean,
    val sourceVertices: List<SkinnedTexturedLitVertexSource3D>
) {
    fun cleanup() {
        mesh.cleanup()
    }
}

private fun cleanupGlbResources3D(
    resources: List<GpuResource3D>,
    ownedResources: List<GpuResource3D>
) {
    var firstError: Throwable? = null
    (resources.asReversed() + ownedResources.asReversed()).forEach { resource ->
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

object GlbMeshLoader {
    private const val GLB_MAGIC = 0x46546C67
    private const val GLB_VERSION_2 = 2
    private const val JSON_CHUNK = 0x4E4F534A
    private const val BIN_CHUNK = 0x004E4942
    private const val NO_TEXTURE_KEY = -1
    private const val COMPONENT_UNSIGNED_BYTE = 5121
    private const val COMPONENT_UNSIGNED_SHORT = 5123
    private const val COMPONENT_UNSIGNED_INT = 5125
    private const val COMPONENT_FLOAT = 5126
    private const val GLTF_WRAP_REPEAT = 10497
    private const val GLTF_WRAP_CLAMP_TO_EDGE = 33071
    private const val GLTF_WRAP_MIRRORED_REPEAT = 33648
    private const val GLTF_FILTER_NEAREST = 9728
    private const val GLTF_FILTER_LINEAR = 9729
    private const val GLTF_FILTER_NEAREST_MIPMAP_NEAREST = 9984
    private const val GLTF_FILTER_LINEAR_MIPMAP_NEAREST = 9985
    private const val GLTF_FILTER_NEAREST_MIPMAP_LINEAR = 9986
    private const val GLTF_FILTER_LINEAR_MIPMAP_LINEAR = 9987
    private const val TRIANGLES_MODE = 4

    fun loadLit(
        gpu: GpuContext,
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): LitGpuMesh {
        return LitGpuMesh.create(gpu, loadLitVertices(assetPath, options))
    }

    fun loadTexturedLit(
        gpu: GpuContext,
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions(),
        textureCache: GpuTextureCache3D? = null
    ): GlbTexturedLitModel {
        val loaded = loadTexturedLitParts(gpu, assetPath, options, textureCache)
        return GlbTexturedLitModel(
            parts = loaded.parts,
            ownedResources = loaded.ownedResources
        )
    }

    internal fun loadTexturedLitParts(
        gpu: GpuContext,
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions(),
        textureCache: GpuTextureCache3D? = null
    ): GlbTexturedLitLoad3D {
        val source = loadTexturedLitSource(assetPath, options)
        val textureCacheScope = gpu.resolveTextureCache3D(textureCache)
        val parts = mutableListOf<GlbTexturedLitMeshPart>()
        try {
            source.parts.forEach { partSource ->
                parts += GlbTexturedLitMeshPart(
                    mesh = TexturedLitGpuMesh.create(gpu, partSource.vertices),
                    material = partSource.materialDescriptor.upload(gpu, textureCacheScope.cache)
                )
            }
        } catch (e: Throwable) {
            parts.forEach { it.cleanup() }
            textureCacheScope.ownedResources.asReversed().forEach { it.cleanup() }
            throw e
        }

        return GlbTexturedLitLoad3D(
            parts = parts,
            ownedResources = textureCacheScope.ownedResources
        )
    }

    internal fun loadTexturedLitSource(
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): GlbTexturedLitSource3D {
        val parsed = parseGlb(assetPath)
        val materialInfos = parseMaterialInfos(parsed.document, options.defaultColor)
        val textureInfos = parseTextures(parsed.document)
        val imageInfos = parseImages(parsed.document)
        val verticesByMaterial = linkedMapOf<GlbMaterialPartKey, MutableList<TexturedLitVertex3D>>()

        fun visitNode(
            nodeIndex: Int,
            parentTransform: ModelMatrix3D
        ) {
            val node = parsed.nodes.getOrNull(nodeIndex) ?: return
            val transform = parentTransform * node.matrix
            node.meshIndex?.let { meshIndex ->
                val mesh = parsed.meshes.getOrNull(meshIndex)
                    ?: throw IllegalArgumentException("GLB node references missing mesh $meshIndex: ${parsed.filePath}")
                appendTexturedMeshVertices(
                    mesh = mesh,
                    transform = transform,
                    accessors = parsed.accessors,
                    bufferViews = parsed.bufferViews,
                    binary = parsed.binary,
                    materialInfos = materialInfos,
                    verticesByMaterial = verticesByMaterial
                )
            }
            node.children.forEach { childIndex -> visitNode(childIndex, transform) }
        }

        parsed.sceneNodeIndices.forEach { nodeIndex -> visitNode(nodeIndex, ModelMatrix3D.identity()) }
        require(verticesByMaterial.values.any { it.isNotEmpty() }) {
            "GLB file contains no supported textured triangle mesh vertices: ${parsed.filePath}"
        }

        val normalizedParts = normalizeTexturedVertices(verticesByMaterial, options)
        val parts = normalizedParts.map { (materialKey, vertices) ->
            val materialInfo = materialKey.materialIndex?.let { materialInfos.getOrNull(it) }
            ModelPartSource3D.TexturedLit(
                vertices = vertices,
                materialDescriptor = createMaterialDescriptorForPart(
                    materialInfo = materialInfo,
                    assetPath = assetPath,
                    textureInfos = textureInfos,
                    imageInfos = imageInfos,
                    bufferViews = parsed.bufferViews,
                    binary = parsed.binary,
                    fallbackBaseColor = Color.fromHex("ffffff"),
                    fallbackName = if (materialKey.baseColorTextureIndex == NO_TEXTURE_KEY) {
                        "glb-material:no-texture"
                    } else {
                        "glb-texture-${materialKey.baseColorTextureIndex}"
                    }
                )
            )
        }
        val litVertices = normalizedParts.values.flatten().map { it.toLitVertex() }

        return GlbTexturedLitSource3D(
            info = inspectParsedGlb(parsed, materialInfos),
            litVertices = litVertices,
            parts = parts,
        )
    }

    fun loadSkinnedTexturedLit(
        gpu: GpuContext,
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions(),
        textureCache: GpuTextureCache3D? = null
    ): GlbSkinnedTexturedLitModel {
        return uploadSkinnedTexturedLit(
            gpu = gpu,
            source = loadSkinnedTexturedLitSource(assetPath, options),
            textureCache = textureCache
        )
    }

    internal fun loadSkinnedTexturedLitSource(
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): SkinnedTexturedLitModelSource3D {
        val parsed = parseGlb(assetPath)
        val materialInfos = parseMaterialInfos(parsed.document, options.defaultColor)
        val textureInfos = parseTextures(parsed.document)
        val imageInfos = parseImages(parsed.document)
        val skins = parseSkinData(parsed)
        val skinningSupport = inspectSkinningSupport(parsed, skins.size)
        val animations = parseAnimations(parsed)
        val restWorldMatrices = sampleModelNodeWorldMatrices3D(
            nodes = parsed.nodes,
            sceneNodeIndices = parsed.sceneNodeIndices,
            animations = emptyList(),
            clipIndex = 0,
            timeSeconds = 0.0
        )
        val parts = mutableListOf<SkinnedTexturedLitMeshPartSource3D>()
        val initialVerticesForBounds = mutableListOf<TexturedLitVertex3D>()

        require(skins.isNotEmpty()) {
            "GLB file contains no skins for skeletal animation: ${parsed.filePath}"
        }

        parsed.nodes.forEachIndexed { nodeIndex, node ->
            val meshIndex = node.meshIndex ?: return@forEachIndexed
            val skinIndex = node.skinIndex ?: return@forEachIndexed
            val skin = skins.getOrNull(skinIndex)
                ?: throw IllegalArgumentException(
                    "GLB skinned node ${node.label(nodeIndex)} references missing skin $skinIndex " +
                        "(skins=${skins.size}): ${parsed.filePath}"
                )
            val mesh = parsed.meshes.getOrNull(meshIndex)
                ?: throw IllegalArgumentException(
                    "GLB skinned node ${node.label(nodeIndex)} references missing mesh $meshIndex " +
                        "(meshes=${parsed.meshes.size}): ${parsed.filePath}"
                )
            val skinMatrices = skinMatricesForModelSkin3D(skin, restWorldMatrices)

            mesh.primitives.forEachIndexed { primitiveIndex, primitive ->
                if (primitive.mode != TRIANGLES_MODE) {
                    return@forEachIndexed
                }
                if (primitive.jointAccessor == null || primitive.weightAccessor == null) {
                    return@forEachIndexed
                }

                val materialInfo = materialInfos.getOrNull(primitive.materialIndex ?: -1)
                val primitiveContext = skinnedPrimitiveContext(
                    parsed = parsed,
                    nodeIndex = nodeIndex,
                    node = node,
                    meshIndex = meshIndex,
                    skinIndex = skinIndex,
                    primitiveIndex = primitiveIndex
                )
                val sourceVertices = readSkinnedTexturedVertices(
                    primitive = primitive,
                    accessors = parsed.accessors,
                    bufferViews = parsed.bufferViews,
                    binary = parsed.binary,
                    material = materialInfo,
                    fallbackColor = primitiveColor(primitive.materialIndex ?: 0),
                    skinJointCount = skin.joints.size,
                    context = primitiveContext
                )
                if (sourceVertices.isEmpty()) {
                    return@forEachIndexed
                }

                val initialVertices = sourceVertices.map { vertex -> vertex.toTexturedVertex(skinMatrices) }
                initialVerticesForBounds += initialVertices
                parts += SkinnedTexturedLitMeshPartSource3D(
                    nodeIndex = nodeIndex,
                    skinIndex = skinIndex,
                    materialDescriptor = createMaterialDescriptorForPart(
                        materialInfo = materialInfo,
                        assetPath = assetPath,
                        textureInfos = textureInfos,
                        imageInfos = imageInfos,
                        bufferViews = parsed.bufferViews,
                        binary = parsed.binary,
                        fallbackBaseColor = primitiveColor(primitive.materialIndex ?: 0),
                        fallbackName = null
                    ),
                    restVertices = initialVertices,
                    skinnedVertices = sourceVertices.map { vertex -> vertex.toSkinnedTexturedLitVertex() },
                    sourceVertices = sourceVertices
                )
            }
        }

        require(parts.isNotEmpty() && initialVerticesForBounds.isNotEmpty()) {
            "GLB file contains no supported skinned textured triangle mesh vertices: ${parsed.filePath}. " +
                "Skinning support: ${skinningSupport.unsupportedSummary()}."
        }

        val vertexCount = parts.sumOf { it.restVertices.size }
        return SkinnedTexturedLitModelSource3D(
            info = inspectParsedGlb(parsed, materialInfos).toModelInfo3D(
                assetPath = assetPath,
                vertexCount = vertexCount,
                format = ModelLoader3D.detectFormat(assetPath)
            ),
            parts = parts,
            nodes = parsed.nodes,
            sceneNodeIndices = parsed.sceneNodeIndices,
            skins = skins,
            animationClips = animations,
            normalizationMatrix = normalizationMatrix(
                bounds = GlbBounds.fromTexturedVertices(initialVerticesForBounds),
                options = options
            ).toMat4()
        )
    }

    internal fun uploadSkinnedTexturedLit(
        gpu: GpuContext,
        source: SkinnedTexturedLitModelSource3D,
        textureCache: GpuTextureCache3D? = null
    ): GlbSkinnedTexturedLitModel {
        val textureCacheScope =
            if (source.parts.any { it.materialDescriptor.hasTexture }) {
                gpu.resolveTextureCache3D(textureCache)
            } else {
                null
            }
        val parts = mutableListOf<GlbSkinnedTexturedLitMeshPart>()
        try {
            source.parts.forEach { partSource ->
                parts += GlbSkinnedTexturedLitMeshPart(
                    nodeIndex = partSource.nodeIndex,
                    skinIndex = partSource.skinIndex,
                    material = partSource.materialDescriptor.upload(gpu, textureCacheScope?.cache),
                    restVertices = partSource.restVertices,
                    skinnedVertices = partSource.skinnedVertices,
                    sourceVertices = partSource.sourceVertices
                )
            }
        } catch (e: Throwable) {
            parts.asReversed().forEach { it.cleanup() }
            textureCacheScope?.ownedResources.orEmpty().asReversed().forEach { it.cleanup() }
            throw e
        }

        return GlbSkinnedTexturedLitModel(
            gpu = gpu,
            parts = parts,
            nodes = source.nodes,
            sceneNodeIndices = source.sceneNodeIndices,
            skins = source.skins,
            animations = source.animationClips,
            normalizationMatrix = source.normalizationMatrix,
            ownedResources = textureCacheScope?.ownedResources.orEmpty()
        )
    }

    fun loadAnimatedLit(
        gpu: GpuContext,
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): GlbAnimatedLitModel {
        return uploadAnimatedLit(
            gpu = gpu,
            source = loadAnimatedLitSource(assetPath, options)
        )
    }

    internal fun loadAnimatedLitSource(
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): NodeAnimatedLitModelSource3D {
        val parsed = parseGlb(assetPath)
        val materialInfos = parseMaterialInfos(parsed.document, options.defaultColor)
        val materialColors = materialInfos.map { it.color }
        val animations = parseAnimations(parsed)
        val rawWorldVertices = mutableListOf<LitVertex3D>()
        val verticesByMesh = mutableMapOf<Int, List<LitVertex3D>>()
        val parts = mutableListOf<NodeAnimatedLitMeshPartSource3D>()

        fun visitNodeForBounds(
            nodeIndex: Int,
            parentTransform: ModelMatrix3D
        ) {
            val node = parsed.nodes.getOrNull(nodeIndex) ?: return
            val transform = parentTransform * node.matrix
            node.meshIndex?.let { meshIndex ->
                val mesh = parsed.meshes.getOrNull(meshIndex)
                    ?: throw IllegalArgumentException("GLB node references missing mesh $meshIndex: ${parsed.filePath}")
                appendMeshVertices(mesh, transform, parsed.accessors, parsed.bufferViews, parsed.binary, materialColors, rawWorldVertices)
            }
            node.children.forEach { childIndex -> visitNodeForBounds(childIndex, transform) }
        }

        parsed.nodes.forEachIndexed { nodeIndex, node ->
            val meshIndex = node.meshIndex ?: return@forEachIndexed
            val mesh = parsed.meshes.getOrNull(meshIndex)
                ?: throw IllegalArgumentException("GLB node references missing mesh $meshIndex: ${parsed.filePath}")
            val vertices = verticesByMesh.getOrPut(meshIndex) {
                val meshVertices = mutableListOf<LitVertex3D>()
                appendMeshVertices(
                    mesh = mesh,
                    transform = ModelMatrix3D.identity(),
                    accessors = parsed.accessors,
                    bufferViews = parsed.bufferViews,
                    binary = parsed.binary,
                    materialColors = materialColors,
                    vertices = meshVertices
                )
                meshVertices
            }
            if (vertices.isNotEmpty()) {
                parts += NodeAnimatedLitMeshPartSource3D(
                    nodeIndex = nodeIndex,
                    meshIndex = meshIndex,
                    vertices = vertices
                )
            }
        }
        parsed.sceneNodeIndices.forEach { nodeIndex -> visitNodeForBounds(nodeIndex, ModelMatrix3D.identity()) }
        require(parts.isNotEmpty() && rawWorldVertices.isNotEmpty()) {
            "GLB file contains no supported animated lit mesh parts: ${parsed.filePath}"
        }

        val vertexCount = parts.sumOf { it.vertices.size }
        return NodeAnimatedLitModelSource3D(
            info = inspectParsedGlb(parsed, materialInfos).toModelInfo3D(
                assetPath = assetPath,
                vertexCount = vertexCount,
                format = ModelLoader3D.detectFormat(assetPath)
            ),
            parts = parts,
            nodes = parsed.nodes,
            sceneNodeIndices = parsed.sceneNodeIndices,
            animationClips = animations,
            normalizationMatrix = normalizationMatrix(GlbBounds.fromVertices(rawWorldVertices), options).toMat4()
        )
    }

    internal fun uploadAnimatedLit(
        gpu: GpuContext,
        source: NodeAnimatedLitModelSource3D
    ): GlbAnimatedLitModel {
        val meshCache = mutableMapOf<Int, LitGpuMesh>()
        val parts = mutableListOf<GlbAnimatedLitMeshPart>()
        try {
            source.parts.forEach { partSource ->
                val mesh = meshCache.getOrPut(partSource.meshIndex) {
                    LitGpuMesh.create(gpu, partSource.vertices)
                }
                parts += GlbAnimatedLitMeshPart(partSource.nodeIndex, mesh)
            }
        } catch (e: Throwable) {
            meshCache.values.forEach { it.cleanup() }
            throw e
        }

        return GlbAnimatedLitModel(
            parts = parts,
            nodes = source.nodes,
            sceneNodeIndices = source.sceneNodeIndices,
            animations = source.animationClips,
            normalizationMatrix = source.normalizationMatrix
        )
    }

    fun loadLitVertices(
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): List<LitVertex3D> {
        val parsed = parseGlb(assetPath)
        val materialColors = parseMaterialColors(parsed.document, options.defaultColor)
        val vertices = mutableListOf<LitVertex3D>()

        fun visitNode(
            nodeIndex: Int,
            parentTransform: ModelMatrix3D
        ) {
            val node = parsed.nodes.getOrNull(nodeIndex) ?: return
            val transform = parentTransform * node.matrix
            node.meshIndex?.let { meshIndex ->
                val mesh = parsed.meshes.getOrNull(meshIndex)
                    ?: throw IllegalArgumentException("GLB node references missing mesh $meshIndex: ${parsed.filePath}")
                appendMeshVertices(mesh, transform, parsed.accessors, parsed.bufferViews, parsed.binary, materialColors, vertices)
            }
            node.children.forEach { childIndex -> visitNode(childIndex, transform) }
        }

        parsed.sceneNodeIndices.forEach { nodeIndex -> visitNode(nodeIndex, ModelMatrix3D.identity()) }
        require(vertices.isNotEmpty()) {
            "GLB file contains no supported triangle mesh vertices: ${parsed.filePath}"
        }

        return normalizeVertices(vertices, options)
    }

    fun inspect(assetPath: String): GlbModelInfo {
        val parsed = parseGlb(assetPath)
        val materialInfos = parseMaterialInfos(parsed.document, Color.fromHex("ffffff"))
        return inspectParsedGlb(parsed, materialInfos)
    }

    private fun inspectParsedGlb(
        parsed: ParsedGlb,
        materialInfos: List<GlbMaterialInfo>
    ): GlbModelInfo {
        val animations = parseAnimations(parsed).map { it.info }
        val skins = parseSkins(parsed)
        val skinningSupport = inspectSkinningSupport(parsed, skins.size)
        val primitiveCount = parsed.meshes.sumOf { it.primitives.size }
        return GlbModelInfo(
            skinCount = skins.size,
            animationCount = animations.size,
            meshCount = parsed.meshes.size,
            primitiveCount = primitiveCount,
            skinnedPrimitiveCount = skinningSupport.supportedPrimitiveCount,
            materialCount = materialInfos.size,
            textureCount = parsed.document.optionalArray("textures")?.size ?: 0,
            imageCount = parsed.document.optionalArray("images")?.size ?: 0,
            textureSlotUsage = materialTextureSlotUsage3D(materialInfos),
            hasTexturedMaterials = materialInfos.any { it.textureSlots.baseColor != null },
            animations = animations,
            skins = skins,
            skinningSupport = skinningSupport
        )
    }

    private fun parseGlb(assetPath: String): ParsedGlb {
        val filePath = File.resolveAssetPath(assetPath)
        return when (ModelLoader3D.detectFormat(filePath)) {
            ModelFormat3D.GLB -> parseBinaryGlb(filePath)
            ModelFormat3D.GLTF -> parseJsonGltf(filePath)
            else -> throw IllegalArgumentException("Unsupported glTF model format: $filePath")
        }
    }

    private fun parseBinaryGlb(filePath: String): ParsedGlb {
        val bytes = readBinaryFile(
            filePath = filePath,
            description = "GLB model file"
        )
        require(readIntLE(bytes, 0) == GLB_MAGIC) {
            "GLB file has an invalid magic header: $filePath"
        }
        require(readIntLE(bytes, 4) == GLB_VERSION_2) {
            "Only GLB 2.0 files are supported: $filePath"
        }

        var jsonText: String? = null
        var binaryChunk: ByteArray? = null
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkLength = readIntLE(bytes, offset)
            val chunkType = readIntLE(bytes, offset + 4)
            val chunkStart = offset + 8
            val chunkEnd = chunkStart + chunkLength
            require(chunkEnd <= bytes.size) {
                "GLB chunk extends past end of file: $filePath"
            }

            when (chunkType) {
                JSON_CHUNK -> jsonText = bytes.decodeToString(chunkStart, chunkEnd)
                    .trimEnd('\u0000', ' ', '\n', '\r', '\t')
                BIN_CHUNK -> binaryChunk = bytes.copyOfRange(chunkStart, chunkEnd)
            }
            offset = chunkEnd
        }

        val json = jsonText ?: throw IllegalArgumentException("GLB file is missing a JSON chunk: $filePath")
        val binary = binaryChunk ?: throw IllegalArgumentException("GLB file is missing a binary chunk: $filePath")
        val document = Json.parseToJsonElement(json).jsonObject
        val bufferViews = parseBufferViews(document, binary.size, filePath)
        val accessors = parseAccessors(document)
        val meshes = parseMeshes(document)
        val nodes = parseNodes(document)
        val sceneNodeIndices = parseSceneNodeIndices(document)
        return ParsedGlb(
            filePath = filePath,
            document = document,
            binary = binary,
            bufferViews = bufferViews,
            accessors = accessors,
            meshes = meshes,
            nodes = nodes,
            sceneNodeIndices = sceneNodeIndices
        )
    }

    private fun parseJsonGltf(filePath: String): ParsedGlb {
        val document = Json.parseToJsonElement(
            readTextFile(
                filePath = filePath,
                description = "GLTF model file"
            )
        ).jsonObject
        val binary = readGltfPrimaryBuffer(document, filePath)
        val bufferViews = parseBufferViews(document, binary.size, filePath)
        val accessors = parseAccessors(document)
        val meshes = parseMeshes(document)
        val nodes = parseNodes(document)
        val sceneNodeIndices = parseSceneNodeIndices(document)
        return ParsedGlb(
            filePath = filePath,
            document = document,
            binary = binary,
            bufferViews = bufferViews,
            accessors = accessors,
            meshes = meshes,
            nodes = nodes,
            sceneNodeIndices = sceneNodeIndices
        )
    }

    private fun readGltfPrimaryBuffer(
        document: JsonObject,
        filePath: String
    ): ByteArray {
        val buffers = document.requiredArray("buffers")
        require(buffers.isNotEmpty()) {
            "GLTF file has no buffers: $filePath"
        }
        if (buffers.size > 1) {
            throw IllegalArgumentException("Only single-buffer GLTF files are supported: $filePath")
        }
        val buffer = buffers.first().jsonObject
        val uri = buffer.optionalString("uri")
            ?: throw IllegalArgumentException("External GLTF buffer is missing uri: $filePath")
        requireSupportedFileUri(uri, "buffer", filePath)
        val bufferPath = ModelResourcePath3D.resolveSiblingPath(
            filePath = filePath,
            path = uri,
            decodeUriPath = true,
            stripFragment = true
        )
        val bytes = readBinaryFile(
            filePath = bufferPath,
            description = "GLTF buffer '$uri'",
            referencedBy = filePath
        )
        val declaredByteLength = buffer.requiredInt("byteLength")
        require(bytes.size >= declaredByteLength) {
            "GLTF buffer '$uri' is shorter than declared byteLength $declaredByteLength: $filePath"
        }
        return bytes
    }

    private fun appendMeshVertices(
        mesh: GlbMesh,
        transform: ModelMatrix3D,
        accessors: List<GlbAccessor>,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray,
        materialColors: List<Color>,
        vertices: MutableList<LitVertex3D>
    ) {
        mesh.primitives.forEach { primitive ->
            if (primitive.mode != TRIANGLES_MODE) {
                return@forEach
            }

            val positions = readVec3Accessor(accessors[primitive.positionAccessor], bufferViews, binary)
            val normals = primitive.normalAccessor?.let {
                readVec3Accessor(accessors[it], bufferViews, binary)
            }
            val indices = primitive.indicesAccessor?.let {
                readIndexAccessor(accessors[it], bufferViews, binary)
            } ?: positions.indices.toList()
            val color = materialColors.getOrElse(primitive.materialIndex ?: -1) { primitiveColor(primitive.materialIndex ?: 0) }

            for (index in 0 until indices.size - 2 step 3) {
                val ia = indices[index]
                val ib = indices[index + 1]
                val ic = indices[index + 2]
                val a = transform.transformPoint(positions[ia])
                val b = transform.transformPoint(positions[ib])
                val c = transform.transformPoint(positions[ic])
                val normalA = normals?.getOrNull(ia)?.let(transform::transformVector)?.let(::normalize)
                val normalB = normals?.getOrNull(ib)?.let(transform::transformVector)?.let(::normalize)
                val normalC = normals?.getOrNull(ic)?.let(transform::transformVector)?.let(::normalize)
                val faceNormal = normalize(cross(subtract(b, a), subtract(c, a)))

                vertices += LitVertex3D(a, normalA ?: faceNormal, color)
                vertices += LitVertex3D(b, normalB ?: faceNormal, color)
                vertices += LitVertex3D(c, normalC ?: faceNormal, color)
            }
        }
    }

    private fun appendTexturedMeshVertices(
        mesh: GlbMesh,
        transform: ModelMatrix3D,
        accessors: List<GlbAccessor>,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray,
        materialInfos: List<GlbMaterialInfo>,
        verticesByMaterial: MutableMap<GlbMaterialPartKey, MutableList<TexturedLitVertex3D>>
    ) {
        mesh.primitives.forEach { primitive ->
            if (primitive.mode != TRIANGLES_MODE) {
                return@forEach
            }

            val positions = readVec3Accessor(accessors[primitive.positionAccessor], bufferViews, binary)
            val normals = primitive.normalAccessor?.let {
                readVec3Accessor(accessors[it], bufferViews, binary)
            }
            val texCoords = primitive.texCoordAccessor?.let {
                readVec2Accessor(accessors[it], bufferViews, binary)
            }
            val indices = primitive.indicesAccessor?.let {
                readIndexAccessor(accessors[it], bufferViews, binary)
            } ?: positions.indices.toList()
            val material = materialInfos.getOrNull(primitive.materialIndex ?: -1)
            val color = material?.color ?: primitiveColor(primitive.materialIndex ?: 0)
            val uvTransform = material?.uvTransform ?: GlbUvTransform.IDENTITY
            val materialKey = GlbMaterialPartKey(
                materialIndex = primitive.materialIndex,
                baseColorTextureIndex = material?.textureSlots?.baseColor ?: NO_TEXTURE_KEY
            )
            val destination = verticesByMaterial.getOrPut(materialKey) { mutableListOf() }

            for (index in 0 until indices.size - 2 step 3) {
                val ia = indices[index]
                val ib = indices[index + 1]
                val ic = indices[index + 2]
                val a = transform.transformPoint(positions[ia])
                val b = transform.transformPoint(positions[ib])
                val c = transform.transformPoint(positions[ic])
                val normalA = normals?.getOrNull(ia)?.let(transform::transformVector)?.let(::normalize)
                val normalB = normals?.getOrNull(ib)?.let(transform::transformVector)?.let(::normalize)
                val normalC = normals?.getOrNull(ic)?.let(transform::transformVector)?.let(::normalize)
                val faceNormal = normalize(cross(subtract(b, a), subtract(c, a)))
                val uvA = texCoords?.getOrNull(ia)?.let(uvTransform::apply) ?: GlbVec2.ZERO
                val uvB = texCoords?.getOrNull(ib)?.let(uvTransform::apply) ?: GlbVec2.ZERO
                val uvC = texCoords?.getOrNull(ic)?.let(uvTransform::apply) ?: GlbVec2.ZERO

                destination += TexturedLitVertex3D(a, normalA ?: faceNormal, color, uvA.u.toFloat(), uvA.v.toFloat())
                destination += TexturedLitVertex3D(b, normalB ?: faceNormal, color, uvB.u.toFloat(), uvB.v.toFloat())
                destination += TexturedLitVertex3D(c, normalC ?: faceNormal, color, uvC.u.toFloat(), uvC.v.toFloat())
            }
        }
    }

    private fun readSkinnedTexturedVertices(
        primitive: GlbPrimitive,
        accessors: List<GlbAccessor>,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray,
        material: GlbMaterialInfo?,
        fallbackColor: Color,
        skinJointCount: Int,
        context: String
    ): List<SkinnedTexturedLitVertexSource3D> {
        val positions = readVec3Accessor(
            requireAccessor(accessors, primitive.positionAccessor, "POSITION", context),
            bufferViews,
            binary
        )
        val normals = primitive.normalAccessor?.let {
            readVec3Accessor(requireAccessor(accessors, it, "NORMAL", context), bufferViews, binary)
        }
        val texCoords = primitive.texCoordAccessor?.let {
            readVec2Accessor(requireAccessor(accessors, it, "TEXCOORD_0", context), bufferViews, binary)
        }
        val jointIndices = readJointIndexAccessor(
            requireAccessor(accessors, primitive.jointAccessor!!, "JOINTS_0", context),
            bufferViews,
            binary
        )
        val weights = readWeightAccessor(
            requireAccessor(accessors, primitive.weightAccessor!!, "WEIGHTS_0", context),
            bufferViews,
            binary
        )
        val indices = primitive.indicesAccessor?.let {
            readIndexAccessor(requireAccessor(accessors, it, "indices", context), bufferViews, binary)
        } ?: positions.indices.toList()
        val color = material?.color ?: fallbackColor
        val uvTransform = material?.uvTransform ?: GlbUvTransform.IDENTITY
        val vertices = mutableListOf<SkinnedTexturedLitVertexSource3D>()

        require(jointIndices.size >= positions.size) {
            "GLB skinned primitive has ${positions.size} positions but only ${jointIndices.size} JOINTS_0 entries: $context"
        }
        require(weights.size >= positions.size) {
            "GLB skinned primitive has ${positions.size} positions but only ${weights.size} WEIGHTS_0 entries: $context"
        }

        for (index in 0 until indices.size - 2 step 3) {
            val ia = indices[index]
            val ib = indices[index + 1]
            val ic = indices[index + 2]
            val a = positions[ia]
            val b = positions[ib]
            val c = positions[ic]
            val faceNormal = normalize(cross(subtract(b, a), subtract(c, a)))
            vertices += sourceSkinnedTexturedVertex(
                position = a,
                normal = normals?.getOrNull(ia) ?: faceNormal,
                color = color,
                uv = texCoords?.getOrNull(ia)?.let(uvTransform::apply) ?: GlbVec2.ZERO,
                joints = jointIndices[ia],
                weights = weights[ia]
            )
            vertices += sourceSkinnedTexturedVertex(
                position = b,
                normal = normals?.getOrNull(ib) ?: faceNormal,
                color = color,
                uv = texCoords?.getOrNull(ib)?.let(uvTransform::apply) ?: GlbVec2.ZERO,
                joints = jointIndices[ib],
                weights = weights[ib]
            )
            vertices += sourceSkinnedTexturedVertex(
                position = c,
                normal = normals?.getOrNull(ic) ?: faceNormal,
                color = color,
                uv = texCoords?.getOrNull(ic)?.let(uvTransform::apply) ?: GlbVec2.ZERO,
                joints = jointIndices[ic],
                weights = weights[ic]
            )
        }

        validateSkinJointReferences(vertices, skinJointCount, context)
        return vertices
    }

    private fun requireAccessor(
        accessors: List<GlbAccessor>,
        accessorIndex: Int,
        label: String,
        context: String
    ): GlbAccessor {
        return accessors.getOrNull(accessorIndex)
            ?: throw IllegalArgumentException(
                "GLB skinned primitive references missing $label accessor $accessorIndex " +
                    "(accessors=${accessors.size}): $context"
            )
    }

    private fun validateSkinJointReferences(
        vertices: List<SkinnedTexturedLitVertexSource3D>,
        skinJointCount: Int,
        context: String
    ) {
        vertices.forEachIndexed { vertexIndex, vertex ->
            for (component in 0 until 4) {
                val weight = vertex.weights.values[component]
                if (weight <= 0.0) {
                    continue
                }

                val jointIndex = vertex.joints.values[component]
                require(jointIndex < skinJointCount) {
                    "GLB skinned primitive vertex $vertexIndex references joint $jointIndex, " +
                        "but its skin has only $skinJointCount joints: $context"
                }
            }
        }
    }

    private fun sourceSkinnedTexturedVertex(
        position: Vec3,
        normal: Vec3,
        color: Color,
        uv: GlbVec2,
        joints: SkinJointIndicesSource3D,
        weights: SkinJointWeightsSource3D
    ): SkinnedTexturedLitVertexSource3D {
        return SkinnedTexturedLitVertexSource3D(
            position = position,
            normal = normal,
            color = color,
            u = uv.u.toFloat(),
            v = uv.v.toFloat(),
            joints = joints,
            weights = weights.normalized()
        )
    }

    private fun TexturedLitVertex3D.toLitVertex(): LitVertex3D {
        return LitVertex3D(
            position = position,
            normal = normal,
            color = color
        )
    }

    private fun normalizeVertices(
        vertices: List<LitVertex3D>,
        options: GlbMeshLoadOptions
    ): List<LitVertex3D> {
        if (!options.normalize) {
            return vertices
        }

        val bounds = GlbBounds.fromVertices(vertices)
        val maxSpan = maxOf(bounds.width, bounds.height, bounds.depth)
        val scale = if (maxSpan > 0.0) options.targetSize / maxSpan else 1.0
        val centerX = (bounds.min.x + bounds.max.x) * 0.5
        val centerY = if (options.placeOnGround) bounds.min.y else (bounds.min.y + bounds.max.y) * 0.5
        val centerZ = (bounds.min.z + bounds.max.z) * 0.5

        return vertices.map { vertex ->
            vertex.copy(
                position = Vec3(
                    x = (vertex.position.x - centerX) * scale,
                    y = (vertex.position.y - centerY) * scale,
                    z = (vertex.position.z - centerZ) * scale
                )
            )
        }
    }

    private fun normalizeTexturedVertices(
        verticesByMaterial: Map<GlbMaterialPartKey, List<TexturedLitVertex3D>>,
        options: GlbMeshLoadOptions
    ): Map<GlbMaterialPartKey, List<TexturedLitVertex3D>> {
        if (!options.normalize) {
            return verticesByMaterial
        }

        val allVertices = verticesByMaterial.values.flatten()
        val bounds = GlbBounds.fromTexturedVertices(allVertices)
        val maxSpan = maxOf(bounds.width, bounds.height, bounds.depth)
        val scale = if (maxSpan > 0.0) options.targetSize / maxSpan else 1.0
        val centerX = (bounds.min.x + bounds.max.x) * 0.5
        val centerY = if (options.placeOnGround) bounds.min.y else (bounds.min.y + bounds.max.y) * 0.5
        val centerZ = (bounds.min.z + bounds.max.z) * 0.5

        return verticesByMaterial.mapValues { (_, vertices) ->
            vertices.map { vertex ->
                vertex.copy(
                    position = Vec3(
                        x = (vertex.position.x - centerX) * scale,
                        y = (vertex.position.y - centerY) * scale,
                        z = (vertex.position.z - centerZ) * scale
                    )
                )
            }
        }
    }

    private fun createMaterialDescriptorForPart(
        materialInfo: GlbMaterialInfo?,
        assetPath: String,
        textureInfos: List<GlbTextureInfo>,
        imageInfos: List<GlbImageInfo>,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray,
        fallbackBaseColor: Color,
        fallbackName: String?
    ): MaterialDescriptor3D {
        val textureSlots = materialInfo?.textureSlots ?: GlbMaterialTextureSlots()
        val baseColorTextureIndex = textureSlots.baseColor ?: NO_TEXTURE_KEY
        return MaterialDescriptor3D.textured(
            textureAsset = createTextureAssetForPart(
                textureIndex = baseColorTextureIndex,
                assetPath = assetPath,
                textureInfos = textureInfos,
                imageInfos = imageInfos,
                bufferViews = bufferViews,
                binary = binary
            ),
            color = materialInfo?.color ?: fallbackBaseColor,
            name = materialInfo?.name ?: fallbackName ?: if (baseColorTextureIndex == NO_TEXTURE_KEY) {
                "glb-material:no-texture"
            } else {
                "glb-texture-$baseColorTextureIndex"
            },
            textures = createMaterialTextureSetForPart(
                textureSlots = textureSlots,
                assetPath = assetPath,
                textureInfos = textureInfos,
                imageInfos = imageInfos,
                bufferViews = bufferViews,
                binary = binary
            )
        )
    }

    private fun createMaterialTextureSetForPart(
        textureSlots: GlbMaterialTextureSlots,
        assetPath: String,
        textureInfos: List<GlbTextureInfo>,
        imageInfos: List<GlbImageInfo>,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): MaterialTextureSet3D {
        fun textureAsset(textureIndex: Int?): GpuTextureAsset3D? {
            if (textureIndex == null) {
                return null
            }
            return createTextureAssetForPart(
                textureIndex = textureIndex,
                assetPath = assetPath,
                textureInfos = textureInfos,
                imageInfos = imageInfos,
                bufferViews = bufferViews,
                binary = binary
            )
        }

        return MaterialTextureSet3D(
            baseColor = textureAsset(textureSlots.baseColor),
            normal = textureAsset(textureSlots.normal),
            metallicRoughness = textureAsset(textureSlots.metallicRoughness),
            specular = textureAsset(textureSlots.specular),
            emissive = textureAsset(textureSlots.emissive),
            ambient = textureAsset(textureSlots.occlusion)
        )
    }

    private fun createTextureAssetForPart(
        textureIndex: Int,
        assetPath: String,
        textureInfos: List<GlbTextureInfo>,
        imageInfos: List<GlbImageInfo>,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): GpuTextureAsset3D {
        if (textureIndex == NO_TEXTURE_KEY) {
            return GpuTextureAsset3D.whiteRgba8(
                cacheKey = "$assetPath:no-texture:white-rgba8"
            )
        }

        val textureInfo = textureInfos.getOrNull(textureIndex)
            ?: throw IllegalArgumentException("GLB material references missing texture $textureIndex: $assetPath")
        val imageIndex = textureInfo.sourceImageIndex
            ?: throw IllegalArgumentException("GLB texture $textureIndex has no source image: $assetPath")
        val imageInfo = imageInfos.getOrNull(imageIndex)
            ?: throw IllegalArgumentException("GLB texture $textureIndex references missing image $imageIndex: $assetPath")
        imageInfo.uri?.let { uri ->
            requireSupportedFileUri(uri, "image", assetPath)
            val imagePath = ModelResourcePath3D.requireExistingFile(
                path = ModelResourcePath3D.resolveSiblingPath(
                    filePath = File.resolveAssetPath(assetPath),
                    path = uri,
                    decodeUriPath = true,
                    stripFragment = true
                ),
                description = "GLTF image '$uri'",
                referencedBy = assetPath
            )
            return GpuTextureAsset3D.file(
                assetPath = imagePath,
                samplerDescriptor = textureInfo.samplerDescriptor
            )
        }
        val bufferViewIndex = imageInfo.bufferViewIndex
            ?: throw IllegalArgumentException("Only embedded GLB images are supported for texture $textureIndex: $assetPath")
        val bufferView = bufferViews.getOrNull(bufferViewIndex)
            ?: throw IllegalArgumentException("GLB image $imageIndex references missing bufferView $bufferViewIndex: $assetPath")
        val cacheKey = "$assetPath:glb-image:$imageIndex"
        return GpuTextureAsset3D.encodedByteRange(
            cacheKey = cacheKey,
            bytes = binary,
            byteOffset = bufferView.byteOffset,
            byteLength = bufferView.byteLength,
            label = "$assetPath image $imageIndex",
            samplerDescriptor = textureInfo.samplerDescriptor
        )
    }

    private fun parseBufferViews(
        document: JsonObject,
        binarySize: Int,
        filePath: String
    ): List<GlbBufferView> {
        return document.requiredArray("bufferViews").mapIndexed { index, element ->
            val item = element.jsonObject
            val bufferIndex = item.optionalInt("buffer") ?: 0
            require(bufferIndex == 0) {
                "Only single-buffer GLTF/GLB buffer views are supported: bufferView $index in $filePath references buffer $bufferIndex."
            }
            val byteOffset = item.optionalInt("byteOffset") ?: 0
            val byteLength = item.requiredInt("byteLength")
            require(byteOffset >= 0 && byteLength >= 0 && byteOffset + byteLength <= binarySize) {
                "GLTF bufferView $index byte range is outside the loaded buffer: $filePath"
            }
            GlbBufferView(
                byteOffset = byteOffset,
                byteLength = byteLength,
                byteStride = item.optionalInt("byteStride")
            )
        }
    }

    private fun parseAccessors(document: JsonObject): List<GlbAccessor> {
        return document.requiredArray("accessors").map { element ->
            val item = element.jsonObject
            GlbAccessor(
                bufferViewIndex = item.requiredInt("bufferView"),
                byteOffset = item.optionalInt("byteOffset") ?: 0,
                componentType = item.requiredInt("componentType"),
                count = item.requiredInt("count"),
                type = item.requiredString("type"),
                normalized = item.optionalBoolean("normalized") ?: false
            )
        }
    }

    private fun parseMeshes(document: JsonObject): List<GlbMesh> {
        return document.requiredArray("meshes").map { meshElement ->
            val mesh = meshElement.jsonObject
            val primitives = mesh.requiredArray("primitives").mapNotNull { primitiveElement ->
                val primitive = primitiveElement.jsonObject
                val attributes = primitive.requiredObject("attributes")
                val positionAccessor = attributes.optionalInt("POSITION") ?: return@mapNotNull null
                GlbPrimitive(
                    positionAccessor = positionAccessor,
                    normalAccessor = attributes.optionalInt("NORMAL"),
                    texCoordAccessor = attributes.optionalInt("TEXCOORD_0"),
                    jointAccessor = attributes.optionalInt("JOINTS_0"),
                    weightAccessor = attributes.optionalInt("WEIGHTS_0"),
                    indicesAccessor = primitive.optionalInt("indices"),
                    materialIndex = primitive.optionalInt("material"),
                    mode = primitive.optionalInt("mode") ?: TRIANGLES_MODE
                )
            }
            GlbMesh(primitives)
        }
    }

    private fun parseNodes(document: JsonObject): List<ModelNode3D> {
        return document.requiredArray("nodes").map { element ->
            val item = element.jsonObject
            ModelNode3D(
                name = item.optionalString("name"),
                meshIndex = item.optionalInt("mesh"),
                skinIndex = item.optionalInt("skin"),
                children = item.optionalArray("children")?.map { it.jsonPrimitive.int }.orEmpty(),
                transform = parseNodeTransform(item),
                matrix = parseNodeMatrix(item)
            )
        }
    }

    private fun parseSceneNodeIndices(document: JsonObject): List<Int> {
        val scenes = document.requiredArray("scenes")
        val sceneIndex = document.optionalInt("scene") ?: 0
        return scenes.getOrNull(sceneIndex)
            ?.jsonObject
            ?.optionalArray("nodes")
            ?.map { it.jsonPrimitive.int }
            .orEmpty()
    }

    private fun parseMaterialColors(
        document: JsonObject,
        defaultColor: Color
    ): List<Color> {
        return parseMaterialInfos(document, defaultColor).map { it.color }
    }

    private fun parseMaterialInfos(
        document: JsonObject,
        defaultColor: Color
    ): List<GlbMaterialInfo> {
        return document.optionalArray("materials")?.mapIndexed { index, element ->
            val material = element.jsonObject
            val pbr = material.optionalObject("pbrMetallicRoughness")
            val colorFactor = pbr?.optionalArray("baseColorFactor")
            val color = if (colorFactor != null && colorFactor.size >= 3) {
                Color.fromRGBA(
                    r = colorFactor[0].jsonPrimitive.double.toFloat(),
                    g = colorFactor[1].jsonPrimitive.double.toFloat(),
                    b = colorFactor[2].jsonPrimitive.double.toFloat(),
                    a = colorFactor.getOrNull(3)?.jsonPrimitive?.double?.toFloat() ?: 1f
                )
            } else {
                primitiveColor(index, defaultColor)
            }
            val baseColorTexture = pbr?.optionalObject("baseColorTexture")
            val materialExtensions = material.optionalObject("extensions")
            val specularExtension = materialExtensions?.optionalObject("KHR_materials_specular")
            val specularGlossinessExtension = materialExtensions?.optionalObject("KHR_materials_pbrSpecularGlossiness")
            val fallbackDiffuseTexture = specularGlossinessExtension?.optionalObject("diffuseTexture")
            val baseColorTextureInfo = baseColorTexture ?: fallbackDiffuseTexture
            val specularTexture = specularExtension?.optionalObject("specularColorTexture")
                ?: specularExtension?.optionalObject("specularTexture")
                ?: specularGlossinessExtension?.optionalObject("specularGlossinessTexture")
            GlbMaterialInfo(
                name = material.optionalString("name"),
                color = color,
                textureSlots = GlbMaterialTextureSlots(
                    baseColor = baseColorTextureInfo?.optionalInt("index"),
                    normal = material.optionalObject("normalTexture")?.optionalInt("index"),
                    metallicRoughness = pbr?.optionalObject("metallicRoughnessTexture")?.optionalInt("index"),
                    specular = specularTexture?.optionalInt("index"),
                    emissive = material.optionalObject("emissiveTexture")?.optionalInt("index"),
                    occlusion = material.optionalObject("occlusionTexture")?.optionalInt("index")
                ),
                uvTransform = parseUvTransform(baseColorTextureInfo)
            )
        }.orEmpty()
    }

    private fun parseTextures(document: JsonObject): List<GlbTextureInfo> {
        val samplers = parseTextureSamplers(document)
        return document.optionalArray("textures")?.map { element ->
            val item = element.jsonObject
            val sampler = item.optionalInt("sampler")?.let { samplerIndex -> samplers.getOrNull(samplerIndex) }
            GlbTextureInfo(
                sourceImageIndex = item.optionalInt("source"),
                samplerDescriptor = sampler ?: GpuSamplerDescriptor3D.NEAREST_REPEAT
            )
        }.orEmpty()
    }

    private fun materialTextureSlotUsage3D(materialInfos: List<GlbMaterialInfo>): MaterialTextureSlotUsage3D {
        var baseColor = 0
        var normal = 0
        var metallicRoughness = 0
        var specular = 0
        var emissive = 0
        var ambient = 0

        materialInfos.forEach { material ->
            if (material.textureSlots.baseColor != null) baseColor += 1
            if (material.textureSlots.normal != null) normal += 1
            if (material.textureSlots.metallicRoughness != null) metallicRoughness += 1
            if (material.textureSlots.specular != null) specular += 1
            if (material.textureSlots.emissive != null) emissive += 1
            if (material.textureSlots.occlusion != null) ambient += 1
        }

        return MaterialTextureSlotUsage3D(
            baseColor = baseColor,
            normal = normal,
            metallicRoughness = metallicRoughness,
            specular = specular,
            emissive = emissive,
            ambient = ambient
        )
    }

    private fun parseTextureSamplers(document: JsonObject): List<GpuSamplerDescriptor3D> {
        return document.optionalArray("samplers")?.map { element ->
            val item = element.jsonObject
            val minFilter = parseTextureMinFilter(item.optionalInt("minFilter"))
            GpuSamplerDescriptor3D(
                minFilter = minFilter.filter,
                magFilter = parseTextureMagFilter(item.optionalInt("magFilter")),
                mipmapMode = minFilter.mipmapMode,
                addressModeU = parseTextureAddressMode(item.optionalInt("wrapS") ?: GLTF_WRAP_REPEAT),
                addressModeV = parseTextureAddressMode(item.optionalInt("wrapT") ?: GLTF_WRAP_REPEAT)
            )
        }.orEmpty()
    }

    private fun parseTextureAddressMode(value: Int): GpuTextureAddressMode {
        return when (value) {
            GLTF_WRAP_CLAMP_TO_EDGE -> GpuTextureAddressMode.CLAMP_TO_EDGE
            GLTF_WRAP_MIRRORED_REPEAT -> GpuTextureAddressMode.MIRRORED_REPEAT
            else -> GpuTextureAddressMode.REPEAT
        }
    }

    private fun parseTextureMinFilter(value: Int?): GlbTextureMinFilter {
        return when (value) {
            GLTF_FILTER_LINEAR -> GlbTextureMinFilter(
                filter = GpuTextureFilter3D.LINEAR,
                mipmapMode = GpuTextureMipmapMode3D.NEAREST
            )
            GLTF_FILTER_NEAREST_MIPMAP_NEAREST -> GlbTextureMinFilter(
                filter = GpuTextureFilter3D.NEAREST,
                mipmapMode = GpuTextureMipmapMode3D.NEAREST
            )
            GLTF_FILTER_LINEAR_MIPMAP_NEAREST -> GlbTextureMinFilter(
                filter = GpuTextureFilter3D.LINEAR,
                mipmapMode = GpuTextureMipmapMode3D.NEAREST
            )
            GLTF_FILTER_NEAREST_MIPMAP_LINEAR -> GlbTextureMinFilter(
                filter = GpuTextureFilter3D.NEAREST,
                mipmapMode = GpuTextureMipmapMode3D.LINEAR
            )
            GLTF_FILTER_LINEAR_MIPMAP_LINEAR -> GlbTextureMinFilter(
                filter = GpuTextureFilter3D.LINEAR,
                mipmapMode = GpuTextureMipmapMode3D.LINEAR
            )
            else -> GlbTextureMinFilter(
                filter = GpuTextureFilter3D.NEAREST,
                mipmapMode = GpuTextureMipmapMode3D.NEAREST
            )
        }
    }

    private fun parseTextureMagFilter(value: Int?): GpuTextureFilter3D {
        return when (value) {
            GLTF_FILTER_LINEAR -> GpuTextureFilter3D.LINEAR
            else -> GpuTextureFilter3D.NEAREST
        }
    }

    private fun parseImages(document: JsonObject): List<GlbImageInfo> {
        return document.optionalArray("images")?.map { element ->
            val item = element.jsonObject
            GlbImageInfo(
                bufferViewIndex = item.optionalInt("bufferView"),
                mimeType = item.optionalString("mimeType"),
                uri = item.optionalString("uri")
            )
        }.orEmpty()
    }

    private fun parseSkins(parsed: ParsedGlb): List<GlbSkinInfo> {
        return parseSkinData(parsed).map { skin ->
            GlbSkinInfo(
                name = skin.name,
                jointCount = skin.joints.size,
                skeletonRootNodeIndex = skin.skeletonRootNodeIndex,
                jointNames = skin.joints.map { jointIndex -> parsed.nodes.getOrNull(jointIndex)?.name },
                inverseBindMatrixCount = skin.inverseBindMatrices.size
            )
        }
    }

    private fun inspectSkinningSupport(
        parsed: ParsedGlb,
        skinCount: Int
    ): GlbSkinningSupportInfo {
        var skinnedNodeCount = 0
        var supportedPrimitiveCount = 0
        var nonTrianglePrimitiveCount = 0
        var missingJointOrWeightPrimitiveCount = 0
        var missingMeshReferenceCount = 0
        var missingSkinReferenceCount = 0

        parsed.nodes.forEach { node ->
            val meshIndex = node.meshIndex ?: return@forEach
            val skinIndex = node.skinIndex ?: return@forEach
            skinnedNodeCount++

            if (skinIndex !in 0 until skinCount) {
                missingSkinReferenceCount++
                return@forEach
            }

            val mesh = parsed.meshes.getOrNull(meshIndex)
            if (mesh == null) {
                missingMeshReferenceCount++
                return@forEach
            }

            mesh.primitives.forEach { primitive ->
                if (primitive.mode != TRIANGLES_MODE) {
                    nonTrianglePrimitiveCount++
                    return@forEach
                }
                if (primitive.jointAccessor == null || primitive.weightAccessor == null) {
                    missingJointOrWeightPrimitiveCount++
                    return@forEach
                }

                supportedPrimitiveCount++
            }
        }

        return GlbSkinningSupportInfo(
            skinnedNodeCount = skinnedNodeCount,
            supportedPrimitiveCount = supportedPrimitiveCount,
            nonTrianglePrimitiveCount = nonTrianglePrimitiveCount,
            missingJointOrWeightPrimitiveCount = missingJointOrWeightPrimitiveCount,
            missingMeshReferenceCount = missingMeshReferenceCount,
            missingSkinReferenceCount = missingSkinReferenceCount
        )
    }

    private fun skinnedPrimitiveContext(
        parsed: ParsedGlb,
        nodeIndex: Int,
        node: ModelNode3D,
        meshIndex: Int,
        skinIndex: Int,
        primitiveIndex: Int
    ): String {
        return "${parsed.filePath} (node ${node.label(nodeIndex)}, mesh $meshIndex, skin $skinIndex, primitive $primitiveIndex)"
    }

    private fun ModelNode3D.label(index: Int): String {
        return name?.let { "$index '$it'" } ?: index.toString()
    }

    private fun parseSkinData(parsed: ParsedGlb): List<ModelSkin3D> {
        return parsed.document.optionalArray("skins")?.map { element ->
            val item = element.jsonObject
            val joints = item.optionalArray("joints")?.map { it.jsonPrimitive.int }.orEmpty()
            val inverseBindMatrices = item.optionalInt("inverseBindMatrices")?.let { accessorIndex ->
                readMat4Accessor(parsed.accessors[accessorIndex], parsed.bufferViews, parsed.binary)
            } ?: List(joints.size) { ModelMatrix3D.identity() }
            require(inverseBindMatrices.size == joints.size) {
                "GLB skin inverse bind matrix count must match joint count: ${parsed.filePath}"
            }
            ModelSkin3D(
                name = item.optionalString("name"),
                skeletonRootNodeIndex = item.optionalInt("skeleton"),
                joints = joints,
                inverseBindMatrices = inverseBindMatrices
            )
        }.orEmpty()
    }

    private fun parseAnimations(parsed: ParsedGlb): List<ModelAnimationClip3D> {
        return parsed.document.optionalArray("animations")?.mapIndexed { animationIndex, element ->
            val animation = element.jsonObject
            val samplers = animation.requiredArray("samplers").map { samplerElement ->
                val sampler = samplerElement.jsonObject
                GlbAnimationSampler(
                    inputTimes = readScalarFloatAccessor(
                        parsed.accessors[sampler.requiredInt("input")],
                        parsed.bufferViews,
                        parsed.binary
                    ),
                    outputAccessorIndex = sampler.requiredInt("output"),
                    interpolation = sampler.optionalString("interpolation") ?: "LINEAR"
                )
            }
            val channels = animation.requiredArray("channels").map { channelElement ->
                val channel = channelElement.jsonObject
                val target = channel.requiredObject("target")
                val samplerIndex = channel.requiredInt("sampler")
                val sampler = samplers.getOrNull(samplerIndex)
                    ?: throw IllegalArgumentException("GLB animation references missing sampler $samplerIndex: ${parsed.filePath}")
                val targetNode = target.requiredInt("node")
                val path = ModelAnimationChannelPath3D.from(target.requiredString("path"))
                val valueSampler = when (path) {
                    ModelAnimationChannelPath3D.TRANSLATION,
                    ModelAnimationChannelPath3D.SCALE -> ModelAnimationValueSampler3D.Vec3Sampler(
                        times = sampler.inputTimes,
                        values = readVec3Accessor(
                            parsed.accessors[sampler.outputAccessorIndex],
                            parsed.bufferViews,
                            parsed.binary
                        ),
                        interpolation = sampler.interpolation
                    )
                    ModelAnimationChannelPath3D.ROTATION -> ModelAnimationValueSampler3D.QuatSampler(
                        times = sampler.inputTimes,
                        values = readQuatAccessor(
                            parsed.accessors[sampler.outputAccessorIndex],
                            parsed.bufferViews,
                            parsed.binary
                        ),
                        interpolation = sampler.interpolation
                    )
                }
                ModelAnimationChannel3D(
                    targetNodeIndex = targetNode,
                    path = path,
                    sampler = valueSampler
                )
            }
            val durationSeconds = channels.maxOfOrNull { it.sampler.durationSeconds } ?: 0.0
            ModelAnimationClip3D(
                info = GlbAnimationClipInfo(
                    name = animation.optionalString("name") ?: "Animation ${animationIndex + 1}",
                    durationSeconds = durationSeconds,
                    channelCount = channels.size,
                    channels = channels.map { channel ->
                        GlbAnimationChannelInfo(
                            nodeIndex = channel.targetNodeIndex,
                            nodeName = parsed.nodes.getOrNull(channel.targetNodeIndex)?.name,
                            path = channel.path.value,
                            interpolation = channel.sampler.interpolation,
                            keyframeCount = channel.sampler.keyframeCount
                        )
                    }
                ),
                channels = channels,
                durationSeconds = durationSeconds
            )
        }.orEmpty()
    }

    private fun parseUvTransform(baseColorTexture: JsonObject?): GlbUvTransform {
        val extension = baseColorTexture
            ?.optionalObject("extensions")
            ?.optionalObject("KHR_texture_transform")
            ?: return GlbUvTransform.IDENTITY
        val offset = extension.optionalArray("offset")
        val scale = extension.optionalArray("scale")
        return GlbUvTransform(
            offsetU = offset?.getOrNull(0)?.jsonPrimitive?.double ?: 0.0,
            offsetV = offset?.getOrNull(1)?.jsonPrimitive?.double ?: 0.0,
            scaleU = scale?.getOrNull(0)?.jsonPrimitive?.double ?: 1.0,
            scaleV = scale?.getOrNull(1)?.jsonPrimitive?.double ?: 1.0,
            rotation = extension.optionalDouble("rotation") ?: 0.0
        )
    }

    private fun parseNodeTransform(node: JsonObject): ModelNodeTransform3D {
        val translation = node.optionalArray("translation")
        val rotation = node.optionalArray("rotation")
        val scale = node.optionalArray("scale")
        return ModelNodeTransform3D(
            translation = Vec3(
                translation?.getOrNull(0)?.jsonPrimitive?.double ?: 0.0,
                translation?.getOrNull(1)?.jsonPrimitive?.double ?: 0.0,
                translation?.getOrNull(2)?.jsonPrimitive?.double ?: 0.0
            ),
            rotation = ModelQuaternion3D(
                rotation?.getOrNull(0)?.jsonPrimitive?.double ?: 0.0,
                rotation?.getOrNull(1)?.jsonPrimitive?.double ?: 0.0,
                rotation?.getOrNull(2)?.jsonPrimitive?.double ?: 0.0,
                rotation?.getOrNull(3)?.jsonPrimitive?.double ?: 1.0
            ).normalized(),
            scale = Vec3(
                scale?.getOrNull(0)?.jsonPrimitive?.double ?: 1.0,
                scale?.getOrNull(1)?.jsonPrimitive?.double ?: 1.0,
                scale?.getOrNull(2)?.jsonPrimitive?.double ?: 1.0
            )
        )
    }

    private fun parseNodeMatrix(node: JsonObject): ModelMatrix3D {
        node.optionalArray("matrix")?.let { matrix ->
            require(matrix.size == 16) {
                "GLB node matrix must contain 16 numbers."
            }
            return ModelMatrix3D(DoubleArray(16) { index -> matrix[index].jsonPrimitive.double })
        }

        val translation = node.optionalArray("translation")
        val rotation = node.optionalArray("rotation")
        val scale = node.optionalArray("scale")
        val tx = translation?.getOrNull(0)?.jsonPrimitive?.double ?: 0.0
        val ty = translation?.getOrNull(1)?.jsonPrimitive?.double ?: 0.0
        val tz = translation?.getOrNull(2)?.jsonPrimitive?.double ?: 0.0
        val rx = rotation?.getOrNull(0)?.jsonPrimitive?.double ?: 0.0
        val ry = rotation?.getOrNull(1)?.jsonPrimitive?.double ?: 0.0
        val rz = rotation?.getOrNull(2)?.jsonPrimitive?.double ?: 0.0
        val rw = rotation?.getOrNull(3)?.jsonPrimitive?.double ?: 1.0
        val sx = scale?.getOrNull(0)?.jsonPrimitive?.double ?: 1.0
        val sy = scale?.getOrNull(1)?.jsonPrimitive?.double ?: 1.0
        val sz = scale?.getOrNull(2)?.jsonPrimitive?.double ?: 1.0
        val xx = rx * rx
        val yy = ry * ry
        val zz = rz * rz
        val xy = rx * ry
        val xz = rx * rz
        val yz = ry * rz
        val wx = rw * rx
        val wy = rw * ry
        val wz = rw * rz
        val m00 = 1.0 - 2.0 * (yy + zz)
        val m01 = 2.0 * (xy + wz)
        val m02 = 2.0 * (xz - wy)
        val m10 = 2.0 * (xy - wz)
        val m11 = 1.0 - 2.0 * (xx + zz)
        val m12 = 2.0 * (yz + wx)
        val m20 = 2.0 * (xz + wy)
        val m21 = 2.0 * (yz - wx)
        val m22 = 1.0 - 2.0 * (xx + yy)

        return ModelMatrix3D(
            doubleArrayOf(
                m00 * sx, m01 * sx, m02 * sx, 0.0,
                m10 * sy, m11 * sy, m12 * sy, 0.0,
                m20 * sz, m21 * sz, m22 * sz, 0.0,
                tx, ty, tz, 1.0
            )
        )
    }

    private fun normalizationMatrix(
        bounds: GlbBounds,
        options: GlbMeshLoadOptions
    ): ModelMatrix3D {
        if (!options.normalize) {
            return ModelMatrix3D.identity()
        }

        val maxSpan = maxOf(bounds.width, bounds.height, bounds.depth)
        val scale = if (maxSpan > 0.0) options.targetSize / maxSpan else 1.0
        val centerX = (bounds.min.x + bounds.max.x) * 0.5
        val centerY = if (options.placeOnGround) bounds.min.y else (bounds.min.y + bounds.max.y) * 0.5
        val centerZ = (bounds.min.z + bounds.max.z) * 0.5
        return ModelMatrix3D(
            doubleArrayOf(
                scale, 0.0, 0.0, 0.0,
                0.0, scale, 0.0, 0.0,
                0.0, 0.0, scale, 0.0,
                -centerX * scale, -centerY * scale, -centerZ * scale, 1.0
            )
        )
    }

    private fun readVec3Accessor(
        accessor: GlbAccessor,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): List<Vec3> {
        require(accessor.type == "VEC3") {
            "Expected VEC3 accessor, got ${accessor.type}."
        }
        require(accessor.componentType == COMPONENT_FLOAT) {
            "Only float VEC3 accessors are supported."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val stride = bufferView.byteStride ?: accessor.byteSize()
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            Vec3(
                readFloatLE(binary, offset),
                readFloatLE(binary, offset + 4),
                readFloatLE(binary, offset + 8)
            )
        }
    }

    private fun readVec2Accessor(
        accessor: GlbAccessor,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): List<GlbVec2> {
        require(accessor.type == "VEC2") {
            "Expected VEC2 accessor, got ${accessor.type}."
        }
        require(accessor.componentType == COMPONENT_FLOAT) {
            "Only float VEC2 accessors are supported."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val stride = bufferView.byteStride ?: accessor.byteSize()
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            GlbVec2(
                readFloatLE(binary, offset),
                readFloatLE(binary, offset + 4)
            )
        }
    }

    private fun readScalarFloatAccessor(
        accessor: GlbAccessor,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): List<Double> {
        require(accessor.type == "SCALAR") {
            "Expected SCALAR accessor, got ${accessor.type}."
        }
        require(accessor.componentType == COMPONENT_FLOAT) {
            "Only float SCALAR accessors are supported."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val stride = bufferView.byteStride ?: accessor.byteSize()
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            readFloatLE(binary, offset)
        }
    }

    private fun readQuatAccessor(
        accessor: GlbAccessor,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): List<ModelQuaternion3D> {
        require(accessor.type == "VEC4") {
            "Expected VEC4 accessor, got ${accessor.type}."
        }
        require(accessor.componentType == COMPONENT_FLOAT) {
            "Only float VEC4 accessors are supported."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val stride = bufferView.byteStride ?: accessor.byteSize()
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            ModelQuaternion3D(
                readFloatLE(binary, offset),
                readFloatLE(binary, offset + 4),
                readFloatLE(binary, offset + 8),
                readFloatLE(binary, offset + 12)
            ).normalized()
        }
    }

    private fun readMat4Accessor(
        accessor: GlbAccessor,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): List<ModelMatrix3D> {
        require(accessor.type == "MAT4") {
            "Expected MAT4 accessor, got ${accessor.type}."
        }
        require(accessor.componentType == COMPONENT_FLOAT) {
            "Only float MAT4 accessors are supported."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val stride = bufferView.byteStride ?: accessor.byteSize()
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            ModelMatrix3D(DoubleArray(16) { valueIndex ->
                readFloatLE(binary, offset + valueIndex * 4)
            })
        }
    }

    private fun readJointIndexAccessor(
        accessor: GlbAccessor,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): List<SkinJointIndicesSource3D> {
        require(accessor.type == "VEC4") {
            "Expected VEC4 joint accessor, got ${accessor.type}."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val componentSize = accessor.componentSize()
        val stride = bufferView.byteStride ?: accessor.byteSize()
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            SkinJointIndicesSource3D(
                values = IntArray(4) { component ->
                    val componentOffset = offset + component * componentSize
                    when (accessor.componentType) {
                        COMPONENT_UNSIGNED_BYTE -> binary[componentOffset].toInt() and 0xff
                        COMPONENT_UNSIGNED_SHORT -> readUnsignedShortLE(binary, componentOffset)
                        else -> throw IllegalArgumentException("Unsupported joint index component type: ${accessor.componentType}")
                    }
                }
            )
        }
    }

    private fun readWeightAccessor(
        accessor: GlbAccessor,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): List<SkinJointWeightsSource3D> {
        require(accessor.type == "VEC4") {
            "Expected VEC4 weight accessor, got ${accessor.type}."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val componentSize = accessor.componentSize()
        val stride = bufferView.byteStride ?: accessor.byteSize()
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            SkinJointWeightsSource3D(
                values = DoubleArray(4) { component ->
                    val componentOffset = offset + component * componentSize
                    when (accessor.componentType) {
                        COMPONENT_FLOAT -> readFloatLE(binary, componentOffset)
                        COMPONENT_UNSIGNED_BYTE -> {
                            val raw = binary[componentOffset].toInt() and 0xff
                            if (accessor.normalized) raw / 255.0 else raw.toDouble()
                        }
                        COMPONENT_UNSIGNED_SHORT -> {
                            val raw = readUnsignedShortLE(binary, componentOffset)
                            if (accessor.normalized) raw / 65535.0 else raw.toDouble()
                        }
                        else -> throw IllegalArgumentException("Unsupported joint weight component type: ${accessor.componentType}")
                    }
                }
            )
        }
    }

    private fun readIndexAccessor(
        accessor: GlbAccessor,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): List<Int> {
        require(accessor.type == "SCALAR") {
            "Expected SCALAR index accessor, got ${accessor.type}."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val componentSize = accessor.componentSize()
        val stride = bufferView.byteStride ?: componentSize
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            when (accessor.componentType) {
                COMPONENT_UNSIGNED_BYTE -> binary[offset].toInt() and 0xff
                COMPONENT_UNSIGNED_SHORT -> readUnsignedShortLE(binary, offset)
                COMPONENT_UNSIGNED_INT -> readUnsignedIntLE(binary, offset).toInt()
                else -> throw IllegalArgumentException("Unsupported index component type: ${accessor.componentType}")
            }
        }
    }

    private fun primitiveColor(
        index: Int,
        fallback: Color = Color.fromHex("8fbf7a")
    ): Color {
        if (index < 0) {
            return fallback
        }
        val palette = listOf(
            Color.fromHex("6fb36b"),
            Color.fromHex("9bc66e"),
            Color.fromHex("c4b66a"),
            Color.fromHex("7eb79b"),
            Color.fromHex("628bbd"),
            Color.fromHex("a37db6"),
            Color.fromHex("c98265"),
            Color.fromHex("7c9f62")
        )
        return palette[index % palette.size]
    }

    private fun subtract(
        a: Vec3,
        b: Vec3
    ): Vec3 {
        return Vec3(a.x - b.x, a.y - b.y, a.z - b.z)
    }

    private fun cross(
        a: Vec3,
        b: Vec3
    ): Vec3 {
        return Vec3(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        )
    }

    private fun normalize(value: Vec3): Vec3 {
        val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
        if (length == 0.0) {
            return Vec3(0.0, 1.0, 0.0)
        }
        return Vec3(value.x / length, value.y / length, value.z / length)
    }

    private fun JsonObject.requiredArray(name: String): JsonArray {
        return this[name]?.jsonArray ?: throw IllegalArgumentException("GLB JSON is missing array '$name'.")
    }

    private fun JsonObject.optionalArray(name: String): JsonArray? {
        return this[name]?.jsonArray
    }

    private fun JsonObject.requiredObject(name: String): JsonObject {
        return this[name]?.jsonObject ?: throw IllegalArgumentException("GLB JSON is missing object '$name'.")
    }

    private fun JsonObject.optionalObject(name: String): JsonObject? {
        return this[name]?.jsonObject
    }

    private fun JsonObject.requiredInt(name: String): Int {
        return optionalInt(name) ?: throw IllegalArgumentException("GLB JSON is missing int '$name'.")
    }

    private fun JsonObject.optionalInt(name: String): Int? {
        return this[name]?.jsonPrimitive?.int
    }

    private fun JsonObject.optionalDouble(name: String): Double? {
        return this[name]?.jsonPrimitive?.double
    }

    private fun JsonObject.optionalBoolean(name: String): Boolean? {
        return this[name]?.jsonPrimitive?.boolean
    }

    private fun JsonObject.requiredString(name: String): String {
        return this[name]?.jsonPrimitive?.content ?: throw IllegalArgumentException("GLB JSON is missing string '$name'.")
    }

    private fun JsonObject.optionalString(name: String): String? {
        return this[name]?.jsonPrimitive?.content
    }

    private fun GlbAccessor.componentSize(): Int {
        return when (componentType) {
            COMPONENT_UNSIGNED_BYTE -> 1
            COMPONENT_UNSIGNED_SHORT -> 2
            COMPONENT_UNSIGNED_INT, COMPONENT_FLOAT -> 4
            else -> throw IllegalArgumentException("Unsupported accessor component type: $componentType")
        }
    }

    private fun GlbAccessor.componentCount(): Int {
        return when (type) {
            "SCALAR" -> 1
            "VEC2" -> 2
            "VEC3" -> 3
            "VEC4" -> 4
            "MAT4" -> 16
            else -> throw IllegalArgumentException("Unsupported accessor type: $type")
        }
    }

    private fun GlbAccessor.byteSize(): Int {
        return componentSize() * componentCount()
    }

    private fun readFloatLE(
        data: ByteArray,
        offset: Int
    ): Double {
        return Float.fromBits(readIntLE(data, offset)).toDouble()
    }

    private fun readIntLE(
        data: ByteArray,
        offset: Int
    ): Int {
        return (data[offset].toInt() and 0xff) or
            ((data[offset + 1].toInt() and 0xff) shl 8) or
            ((data[offset + 2].toInt() and 0xff) shl 16) or
            ((data[offset + 3].toInt() and 0xff) shl 24)
    }

    private fun readUnsignedShortLE(
        data: ByteArray,
        offset: Int
    ): Int {
        return (data[offset].toInt() and 0xff) or
            ((data[offset + 1].toInt() and 0xff) shl 8)
    }

    private fun readUnsignedIntLE(
        data: ByteArray,
        offset: Int
    ): Long {
        return (data[offset].toLong() and 0xffL) or
            ((data[offset + 1].toLong() and 0xffL) shl 8) or
            ((data[offset + 2].toLong() and 0xffL) shl 16) or
            ((data[offset + 3].toLong() and 0xffL) shl 24)
    }

    private fun requireSupportedFileUri(
        uri: String,
        kind: String,
        assetPath: String
    ) {
        require(!uri.startsWith("data:", ignoreCase = true)) {
            "Embedded data URI GLTF $kind resources are not supported yet: $assetPath"
        }
        require(!uri.contains("://")) {
            "Remote or scheme-qualified GLTF $kind resources are not supported yet: $uri in $assetPath"
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readTextFile(
        filePath: String,
        description: String,
        referencedBy: String? = null
    ): String {
        ModelResourcePath3D.requireExistingFile(filePath, description, referencedBy)
        val file = fopen(filePath, "r")
            ?: throw IllegalArgumentException(
                ModelResourcePath3D.cannotOpenFileMessage(filePath, description, referencedBy)
            )
        val buffer = ByteArray(8192)
        val content = StringBuilder()
        try {
            while (true) {
                val bytesRead = fread(buffer.refTo(0), 1.toULong(), buffer.size.toULong(), file)
                if (bytesRead == 0.toULong()) {
                    break
                }
                content.append(buffer.decodeToString(endIndex = bytesRead.toInt()))
            }
        } finally {
            fclose(file)
        }
        return content.toString()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readBinaryFile(
        filePath: String,
        description: String,
        referencedBy: String? = null
    ): ByteArray {
        ModelResourcePath3D.requireExistingFile(filePath, description, referencedBy)
        val file = fopen(filePath, "rb")
            ?: throw IllegalArgumentException(
                ModelResourcePath3D.cannotOpenFileMessage(filePath, description, referencedBy)
            )
        val chunks = mutableListOf<ByteArray>()
        val buffer = ByteArray(8192)
        var totalBytes = 0
        try {
            while (true) {
                val bytesRead = fread(buffer.refTo(0), 1.toULong(), buffer.size.toULong(), file).toInt()
                if (bytesRead == 0) {
                    break
                }
                chunks += buffer.copyOf(bytesRead)
                totalBytes += bytesRead
            }
        } finally {
            fclose(file)
        }

        val result = ByteArray(totalBytes)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }
}

private data class ParsedGlb(
    val filePath: String,
    val document: JsonObject,
    val binary: ByteArray,
    val bufferViews: List<GlbBufferView>,
    val accessors: List<GlbAccessor>,
    val meshes: List<GlbMesh>,
    val nodes: List<ModelNode3D>,
    val sceneNodeIndices: List<Int>
)

private data class GlbBufferView(
    val byteOffset: Int,
    val byteLength: Int,
    val byteStride: Int?
)

private data class GlbAccessor(
    val bufferViewIndex: Int,
    val byteOffset: Int,
    val componentType: Int,
    val count: Int,
    val type: String,
    val normalized: Boolean
)

private data class GlbMesh(
    val primitives: List<GlbPrimitive>
)

private data class GlbPrimitive(
    val positionAccessor: Int,
    val normalAccessor: Int?,
    val texCoordAccessor: Int?,
    val jointAccessor: Int?,
    val weightAccessor: Int?,
    val indicesAccessor: Int?,
    val materialIndex: Int?,
    val mode: Int
)

private data class GlbAnimationSampler(
    val inputTimes: List<Double>,
    val outputAccessorIndex: Int,
    val interpolation: String
)

private data class GlbMaterialInfo(
    val name: String?,
    val color: Color,
    val textureSlots: GlbMaterialTextureSlots,
    val uvTransform: GlbUvTransform
)

private data class GlbMaterialTextureSlots(
    val baseColor: Int? = null,
    val normal: Int? = null,
    val metallicRoughness: Int? = null,
    val specular: Int? = null,
    val emissive: Int? = null,
    val occlusion: Int? = null
)

private data class GlbMaterialPartKey(
    val materialIndex: Int?,
    val baseColorTextureIndex: Int
)

private data class GlbTextureInfo(
    val sourceImageIndex: Int?,
    val samplerDescriptor: GpuSamplerDescriptor3D
)

private data class GlbTextureMinFilter(
    val filter: GpuTextureFilter3D,
    val mipmapMode: GpuTextureMipmapMode3D
)

private data class GlbImageInfo(
    val bufferViewIndex: Int?,
    val mimeType: String?,
    val uri: String?
)

private data class GlbVec2(
    val u: Double,
    val v: Double
) {
    companion object {
        val ZERO = GlbVec2(0.0, 0.0)
    }
}

private data class GlbUvTransform(
    val offsetU: Double,
    val offsetV: Double,
    val scaleU: Double,
    val scaleV: Double,
    val rotation: Double
) {
    fun apply(uv: GlbVec2): GlbVec2 {
        val scaledU = uv.u * scaleU
        val scaledV = uv.v * scaleV
        if (rotation == 0.0) {
            return GlbVec2(scaledU + offsetU, scaledV + offsetV)
        }

        val cosine = cos(rotation)
        val sine = sin(rotation)
        return GlbVec2(
            u = scaledU * cosine - scaledV * sine + offsetU,
            v = scaledU * sine + scaledV * cosine + offsetV
        )
    }

    companion object {
        val IDENTITY = GlbUvTransform(
            offsetU = 0.0,
            offsetV = 0.0,
            scaleU = 1.0,
            scaleV = 1.0,
            rotation = 0.0
        )
    }
}

private data class GlbBounds(
    val min: Vec3,
    val max: Vec3
) {
    val width: Double = max.x - min.x
    val height: Double = max.y - min.y
    val depth: Double = max.z - min.z

    companion object {
        fun fromVertices(vertices: List<LitVertex3D>): GlbBounds {
            var minX = Double.POSITIVE_INFINITY
            var minY = Double.POSITIVE_INFINITY
            var minZ = Double.POSITIVE_INFINITY
            var maxX = Double.NEGATIVE_INFINITY
            var maxY = Double.NEGATIVE_INFINITY
            var maxZ = Double.NEGATIVE_INFINITY

            vertices.forEach { vertex ->
                minX = minOf(minX, vertex.position.x)
                minY = minOf(minY, vertex.position.y)
                minZ = minOf(minZ, vertex.position.z)
                maxX = maxOf(maxX, vertex.position.x)
                maxY = maxOf(maxY, vertex.position.y)
                maxZ = maxOf(maxZ, vertex.position.z)
            }

            return GlbBounds(
                min = Vec3(minX, minY, minZ),
                max = Vec3(maxX, maxY, maxZ)
            )
        }

        fun fromTexturedVertices(vertices: List<TexturedLitVertex3D>): GlbBounds {
            var minX = Double.POSITIVE_INFINITY
            var minY = Double.POSITIVE_INFINITY
            var minZ = Double.POSITIVE_INFINITY
            var maxX = Double.NEGATIVE_INFINITY
            var maxY = Double.NEGATIVE_INFINITY
            var maxZ = Double.NEGATIVE_INFINITY

            vertices.forEach { vertex ->
                minX = minOf(minX, vertex.position.x)
                minY = minOf(minY, vertex.position.y)
                minZ = minOf(minZ, vertex.position.z)
                maxX = maxOf(maxX, vertex.position.x)
                maxY = maxOf(maxY, vertex.position.y)
                maxZ = maxOf(maxZ, vertex.position.z)
            }

            return GlbBounds(
                min = Vec3(minX, minY, minZ),
                max = Vec3(maxX, maxY, maxZ)
            )
        }
    }
}
