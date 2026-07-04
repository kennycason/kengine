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
import kotlin.math.abs
import kotlin.math.acos
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
    val hasTexturedMaterials: Boolean,
    val animations: List<GlbAnimationClipInfo> = emptyList(),
    val skins: List<GlbSkinInfo> = emptyList(),
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

class GlbAnimatedLitModel internal constructor(
    private val parts: List<GlbAnimatedLitMeshPart>,
    private val nodes: List<GlbNode>,
    private val sceneNodeIndices: List<Int>,
    private val animations: List<GlbAnimationClip>,
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
        val nodeWorldMatrices = sampleNodeWorldMatrices(clipIndex, timeSeconds)
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

    private fun sampleNodeWorldMatrices(
        clipIndex: Int,
        timeSeconds: Double
    ): List<GlbMat4> {
        return sampleNodeWorldMatrices(
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
    val parts: List<GlbTexturedLitMeshPart>
) {
    fun draw(
        renderer: TexturedLitMeshRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        parts.forEach { part ->
            renderer.draw(frame, part.mesh, part.texture, transform, camera, light)
        }
    }

    fun cleanup() {
        parts.forEach { it.cleanup() }
    }
}

data class GlbTexturedLitMeshPart(
    val mesh: TexturedLitGpuMesh,
    val texture: GpuTexture
) {
    fun cleanup() {
        mesh.cleanup()
        texture.cleanup()
    }
}

class GlbSkinnedTexturedLitModel internal constructor(
    private val parts: List<GlbSkinnedTexturedLitMeshPart>,
    private val nodes: List<GlbNode>,
    private val sceneNodeIndices: List<Int>,
    private val skins: List<GlbSkin>,
    private val animations: List<GlbAnimationClip>,
    private val normalizationMatrix: Mat4
) {
    val clips: List<GlbAnimationClipInfo> = animations.map { it.info }

    fun updatePose(
        clipIndex: Int = 0,
        timeSeconds: Double = 0.0
    ) {
        val nodeWorldMatrices = sampleNodeWorldMatrices(
            nodes = nodes,
            sceneNodeIndices = sceneNodeIndices,
            animations = animations,
            clipIndex = clipIndex,
            timeSeconds = timeSeconds
        )
        parts.forEach { part ->
            val skin = skins.getOrNull(part.skinIndex) ?: return@forEach
            val skinMatrices = skinMatricesFor(skin, nodeWorldMatrices)
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
        val modelMatrix = transform.matrix() * normalizationMatrix
        parts.forEach { part ->
            renderer.draw(frame, part.mesh, part.texture, modelMatrix, camera, light)
        }
    }

    fun cleanup() {
        parts.forEach { it.cleanup() }
    }
}

internal data class GlbSkinnedTexturedLitMeshPart(
    val nodeIndex: Int,
    val skinIndex: Int,
    val mesh: TexturedLitGpuMesh,
    val texture: GpuTexture,
    val sourceVertices: List<GlbSkinnedTexturedVertex>
) {
    fun cleanup() {
        mesh.cleanup()
        texture.cleanup()
    }
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
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): GlbTexturedLitModel {
        val parsed = parseGlb(assetPath)
        val materialInfos = parseMaterialInfos(parsed.document, options.defaultColor)
        val textureInfos = parseTextures(parsed.document)
        val imageInfos = parseImages(parsed.document)
        val verticesByTexture = linkedMapOf<Int, MutableList<TexturedLitVertex3D>>()

        fun visitNode(
            nodeIndex: Int,
            parentTransform: GlbMat4
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
                    verticesByTexture = verticesByTexture
                )
            }
            node.children.forEach { childIndex -> visitNode(childIndex, transform) }
        }

        parsed.sceneNodeIndices.forEach { nodeIndex -> visitNode(nodeIndex, GlbMat4.identity()) }
        require(verticesByTexture.values.any { it.isNotEmpty() }) {
            "GLB file contains no supported textured triangle mesh vertices: ${parsed.filePath}"
        }

        val normalizedParts = normalizeTexturedVertices(verticesByTexture, options)
        val parts = mutableListOf<GlbTexturedLitMeshPart>()
        try {
            normalizedParts.forEach { (textureIndex, vertices) ->
                val texture = createTextureForPart(
                    gpu = gpu,
                    textureIndex = textureIndex,
                    assetPath = assetPath,
                    textureInfos = textureInfos,
                    imageInfos = imageInfos,
                    bufferViews = parsed.bufferViews,
                    binary = parsed.binary
                )
                try {
                    parts += GlbTexturedLitMeshPart(
                        mesh = TexturedLitGpuMesh.create(gpu, vertices),
                        texture = texture
                    )
                } catch (e: Throwable) {
                    texture.cleanup()
                    throw e
                }
            }
        } catch (e: Throwable) {
            parts.forEach { it.cleanup() }
            throw e
        }

        return GlbTexturedLitModel(parts)
    }

    fun loadSkinnedTexturedLit(
        gpu: GpuContext,
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): GlbSkinnedTexturedLitModel {
        val parsed = parseGlb(assetPath)
        val materialInfos = parseMaterialInfos(parsed.document, options.defaultColor)
        val textureInfos = parseTextures(parsed.document)
        val imageInfos = parseImages(parsed.document)
        val skins = parseSkinData(parsed)
        val animations = parseAnimations(parsed)
        val restWorldMatrices = sampleNodeWorldMatrices(
            nodes = parsed.nodes,
            sceneNodeIndices = parsed.sceneNodeIndices,
            animations = emptyList(),
            clipIndex = 0,
            timeSeconds = 0.0
        )
        val parts = mutableListOf<GlbSkinnedTexturedLitMeshPart>()
        val initialVerticesForBounds = mutableListOf<TexturedLitVertex3D>()

        require(skins.isNotEmpty()) {
            "GLB file contains no skins for skeletal animation: ${parsed.filePath}"
        }

        try {
            parsed.nodes.forEachIndexed { nodeIndex, node ->
                val meshIndex = node.meshIndex ?: return@forEachIndexed
                val skinIndex = node.skinIndex ?: return@forEachIndexed
                val skin = skins.getOrNull(skinIndex)
                    ?: throw IllegalArgumentException("GLB node references missing skin $skinIndex: ${parsed.filePath}")
                val mesh = parsed.meshes.getOrNull(meshIndex)
                    ?: throw IllegalArgumentException("GLB node references missing mesh $meshIndex: ${parsed.filePath}")
                val skinMatrices = skinMatricesFor(skin, restWorldMatrices)

                mesh.primitives.forEach { primitive ->
                    if (primitive.mode != TRIANGLES_MODE) {
                        return@forEach
                    }
                    if (primitive.jointAccessor == null || primitive.weightAccessor == null) {
                        return@forEach
                    }

                    val material = materialInfos.getOrNull(primitive.materialIndex ?: -1)
                    val textureIndex = material?.textureIndex ?: NO_TEXTURE_KEY
                    val sourceVertices = readSkinnedTexturedVertices(
                        primitive = primitive,
                        accessors = parsed.accessors,
                        bufferViews = parsed.bufferViews,
                        binary = parsed.binary,
                        material = material,
                        fallbackColor = primitiveColor(primitive.materialIndex ?: 0)
                    )
                    if (sourceVertices.isEmpty()) {
                        return@forEach
                    }

                    val initialVertices = sourceVertices.map { vertex -> vertex.toTexturedVertex(skinMatrices) }
                    initialVerticesForBounds += initialVertices
                    val texture = createTextureForPart(
                        gpu = gpu,
                        textureIndex = textureIndex,
                        assetPath = assetPath,
                        textureInfos = textureInfos,
                        imageInfos = imageInfos,
                        bufferViews = parsed.bufferViews,
                        binary = parsed.binary
                    )
                    try {
                        parts += GlbSkinnedTexturedLitMeshPart(
                            nodeIndex = nodeIndex,
                            skinIndex = skinIndex,
                            mesh = TexturedLitGpuMesh.create(gpu, initialVertices),
                            texture = texture,
                            sourceVertices = sourceVertices
                        )
                    } catch (e: Throwable) {
                        texture.cleanup()
                        throw e
                    }
                }
            }

            require(parts.isNotEmpty() && initialVerticesForBounds.isNotEmpty()) {
                "GLB file contains no supported skinned textured triangle mesh vertices: ${parsed.filePath}"
            }
        } catch (e: Throwable) {
            parts.forEach { it.cleanup() }
            throw e
        }

        return GlbSkinnedTexturedLitModel(
            parts = parts,
            nodes = parsed.nodes,
            sceneNodeIndices = parsed.sceneNodeIndices,
            skins = skins,
            animations = animations,
            normalizationMatrix = normalizationMatrix(
                bounds = GlbBounds.fromTexturedVertices(initialVerticesForBounds),
                options = options
            ).toMat4()
        )
    }

    fun loadAnimatedLit(
        gpu: GpuContext,
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): GlbAnimatedLitModel {
        val parsed = parseGlb(assetPath)
        val materialColors = parseMaterialColors(parsed.document, options.defaultColor)
        val animations = parseAnimations(parsed)
        val rawWorldVertices = mutableListOf<LitVertex3D>()
        val meshCache = mutableMapOf<Int, LitGpuMesh>()
        val parts = mutableListOf<GlbAnimatedLitMeshPart>()

        fun visitNodeForBounds(
            nodeIndex: Int,
            parentTransform: GlbMat4
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

        try {
            parsed.nodes.forEachIndexed { nodeIndex, node ->
                val meshIndex = node.meshIndex ?: return@forEachIndexed
                val mesh = parsed.meshes.getOrNull(meshIndex)
                    ?: throw IllegalArgumentException("GLB node references missing mesh $meshIndex: ${parsed.filePath}")
                val gpuMesh = meshCache.getOrPut(meshIndex) {
                    val vertices = mutableListOf<LitVertex3D>()
                    appendMeshVertices(
                        mesh = mesh,
                        transform = GlbMat4.identity(),
                        accessors = parsed.accessors,
                        bufferViews = parsed.bufferViews,
                        binary = parsed.binary,
                        materialColors = materialColors,
                        vertices = vertices
                    )
                    LitGpuMesh.create(gpu, vertices)
                }
                parts += GlbAnimatedLitMeshPart(nodeIndex, gpuMesh)
            }
            parsed.sceneNodeIndices.forEach { nodeIndex -> visitNodeForBounds(nodeIndex, GlbMat4.identity()) }
            require(parts.isNotEmpty() && rawWorldVertices.isNotEmpty()) {
                "GLB file contains no supported animated lit mesh parts: ${parsed.filePath}"
            }
        } catch (e: Throwable) {
            meshCache.values.forEach { it.cleanup() }
            throw e
        }

        return GlbAnimatedLitModel(
            parts = parts,
            nodes = parsed.nodes,
            sceneNodeIndices = parsed.sceneNodeIndices,
            animations = animations,
            normalizationMatrix = normalizationMatrix(GlbBounds.fromVertices(rawWorldVertices), options).toMat4()
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
            parentTransform: GlbMat4
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

        parsed.sceneNodeIndices.forEach { nodeIndex -> visitNode(nodeIndex, GlbMat4.identity()) }
        require(vertices.isNotEmpty()) {
            "GLB file contains no supported triangle mesh vertices: ${parsed.filePath}"
        }

        return normalizeVertices(vertices, options)
    }

    fun inspect(assetPath: String): GlbModelInfo {
        val parsed = parseGlb(assetPath)
        val materialInfos = parseMaterialInfos(parsed.document, Color.fromHex("ffffff"))
        val animations = parseAnimations(parsed).map { it.info }
        val skins = parseSkins(parsed)
        val primitiveCount = parsed.meshes.sumOf { it.primitives.size }
        return GlbModelInfo(
            skinCount = skins.size,
            animationCount = animations.size,
            meshCount = parsed.meshes.size,
            primitiveCount = primitiveCount,
            skinnedPrimitiveCount = parsed.meshes.sumOf { mesh ->
                mesh.primitives.count { it.jointAccessor != null && it.weightAccessor != null }
            },
            materialCount = materialInfos.size,
            textureCount = parsed.document.optionalArray("textures")?.size ?: 0,
            imageCount = parsed.document.optionalArray("images")?.size ?: 0,
            hasTexturedMaterials = materialInfos.any { it.textureIndex != null },
            animations = animations,
            skins = skins
        )
    }

    private fun parseGlb(assetPath: String): ParsedGlb {
        val filePath = File.resolveAssetPath(assetPath)
        val bytes = readBinaryFile(filePath)
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
        val bufferViews = parseBufferViews(document)
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

    private fun appendMeshVertices(
        mesh: GlbMesh,
        transform: GlbMat4,
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
        transform: GlbMat4,
        accessors: List<GlbAccessor>,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray,
        materialInfos: List<GlbMaterialInfo>,
        verticesByTexture: MutableMap<Int, MutableList<TexturedLitVertex3D>>
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
            val textureIndex = material?.textureIndex ?: NO_TEXTURE_KEY
            val uvTransform = material?.uvTransform ?: GlbUvTransform.IDENTITY
            val destination = verticesByTexture.getOrPut(textureIndex) { mutableListOf() }

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
        fallbackColor: Color
    ): List<GlbSkinnedTexturedVertex> {
        val positions = readVec3Accessor(accessors[primitive.positionAccessor], bufferViews, binary)
        val normals = primitive.normalAccessor?.let {
            readVec3Accessor(accessors[it], bufferViews, binary)
        }
        val texCoords = primitive.texCoordAccessor?.let {
            readVec2Accessor(accessors[it], bufferViews, binary)
        }
        val jointIndices = readJointIndexAccessor(accessors[primitive.jointAccessor!!], bufferViews, binary)
        val weights = readWeightAccessor(accessors[primitive.weightAccessor!!], bufferViews, binary)
        val indices = primitive.indicesAccessor?.let {
            readIndexAccessor(accessors[it], bufferViews, binary)
        } ?: positions.indices.toList()
        val color = material?.color ?: fallbackColor
        val uvTransform = material?.uvTransform ?: GlbUvTransform.IDENTITY
        val vertices = mutableListOf<GlbSkinnedTexturedVertex>()

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

        return vertices
    }

    private fun sourceSkinnedTexturedVertex(
        position: Vec3,
        normal: Vec3,
        color: Color,
        uv: GlbVec2,
        joints: GlbJointIndices,
        weights: GlbJointWeights
    ): GlbSkinnedTexturedVertex {
        return GlbSkinnedTexturedVertex(
            position = position,
            normal = normal,
            color = color,
            u = uv.u.toFloat(),
            v = uv.v.toFloat(),
            joints = joints,
            weights = weights.normalized()
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
        verticesByTexture: Map<Int, List<TexturedLitVertex3D>>,
        options: GlbMeshLoadOptions
    ): Map<Int, List<TexturedLitVertex3D>> {
        if (!options.normalize) {
            return verticesByTexture
        }

        val allVertices = verticesByTexture.values.flatten()
        val bounds = GlbBounds.fromTexturedVertices(allVertices)
        val maxSpan = maxOf(bounds.width, bounds.height, bounds.depth)
        val scale = if (maxSpan > 0.0) options.targetSize / maxSpan else 1.0
        val centerX = (bounds.min.x + bounds.max.x) * 0.5
        val centerY = if (options.placeOnGround) bounds.min.y else (bounds.min.y + bounds.max.y) * 0.5
        val centerZ = (bounds.min.z + bounds.max.z) * 0.5

        return verticesByTexture.mapValues { (_, vertices) ->
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

    private fun createTextureForPart(
        gpu: GpuContext,
        textureIndex: Int,
        assetPath: String,
        textureInfos: List<GlbTextureInfo>,
        imageInfos: List<GlbImageInfo>,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): GpuTexture {
        if (textureIndex == NO_TEXTURE_KEY) {
            return GpuTexture.createRgba8(
                gpu = gpu,
                width = 1u,
                height = 1u,
                pixels = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
            )
        }

        val textureInfo = textureInfos.getOrNull(textureIndex)
            ?: throw IllegalArgumentException("GLB material references missing texture $textureIndex: $assetPath")
        val imageIndex = textureInfo.sourceImageIndex
            ?: throw IllegalArgumentException("GLB texture $textureIndex has no source image: $assetPath")
        val imageInfo = imageInfos.getOrNull(imageIndex)
            ?: throw IllegalArgumentException("GLB texture $textureIndex references missing image $imageIndex: $assetPath")
        val bufferViewIndex = imageInfo.bufferViewIndex
            ?: throw IllegalArgumentException("Only embedded GLB images are supported for texture $textureIndex: $assetPath")
        val bufferView = bufferViews.getOrNull(bufferViewIndex)
            ?: throw IllegalArgumentException("GLB image $imageIndex references missing bufferView $bufferViewIndex: $assetPath")
        val imageBytes = binary.copyOfRange(bufferView.byteOffset, bufferView.byteOffset + bufferView.byteLength)
        return GpuTexture.fromEncodedBytes(
            gpu = gpu,
            bytes = imageBytes,
            label = "$assetPath image $imageIndex",
            addressModeU = textureInfo.addressModeU,
            addressModeV = textureInfo.addressModeV
        )
    }

    private fun parseBufferViews(document: JsonObject): List<GlbBufferView> {
        return document.requiredArray("bufferViews").map { element ->
            val item = element.jsonObject
            GlbBufferView(
                byteOffset = item.optionalInt("byteOffset") ?: 0,
                byteLength = item.requiredInt("byteLength"),
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

    private fun parseNodes(document: JsonObject): List<GlbNode> {
        return document.requiredArray("nodes").map { element ->
            val item = element.jsonObject
            GlbNode(
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
            val pbr = element.jsonObject.optionalObject("pbrMetallicRoughness")
            val colorFactor = element.jsonObject
                .optionalObject("pbrMetallicRoughness")
                ?.optionalArray("baseColorFactor")
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
            GlbMaterialInfo(
                color = color,
                textureIndex = baseColorTexture?.optionalInt("index"),
                uvTransform = parseUvTransform(baseColorTexture)
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
                addressModeU = sampler?.addressModeU ?: GpuTextureAddressMode.REPEAT,
                addressModeV = sampler?.addressModeV ?: GpuTextureAddressMode.REPEAT
            )
        }.orEmpty()
    }

    private fun parseTextureSamplers(document: JsonObject): List<GlbTextureSamplerInfo> {
        return document.optionalArray("samplers")?.map { element ->
            val item = element.jsonObject
            GlbTextureSamplerInfo(
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

    private fun parseSkinData(parsed: ParsedGlb): List<GlbSkin> {
        return parsed.document.optionalArray("skins")?.map { element ->
            val item = element.jsonObject
            val joints = item.optionalArray("joints")?.map { it.jsonPrimitive.int }.orEmpty()
            val inverseBindMatrices = item.optionalInt("inverseBindMatrices")?.let { accessorIndex ->
                readMat4Accessor(parsed.accessors[accessorIndex], parsed.bufferViews, parsed.binary)
            } ?: List(joints.size) { GlbMat4.identity() }
            require(inverseBindMatrices.size == joints.size) {
                "GLB skin inverse bind matrix count must match joint count: ${parsed.filePath}"
            }
            GlbSkin(
                name = item.optionalString("name"),
                skeletonRootNodeIndex = item.optionalInt("skeleton"),
                joints = joints,
                inverseBindMatrices = inverseBindMatrices
            )
        }.orEmpty()
    }

    private fun parseAnimations(parsed: ParsedGlb): List<GlbAnimationClip> {
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
                val path = GlbAnimationPath.from(target.requiredString("path"))
                val valueSampler = when (path) {
                    GlbAnimationPath.TRANSLATION,
                    GlbAnimationPath.SCALE -> GlbAnimationValueSampler.Vec3Sampler(
                        times = sampler.inputTimes,
                        values = readVec3Accessor(
                            parsed.accessors[sampler.outputAccessorIndex],
                            parsed.bufferViews,
                            parsed.binary
                        ),
                        interpolation = sampler.interpolation
                    )
                    GlbAnimationPath.ROTATION -> GlbAnimationValueSampler.QuatSampler(
                        times = sampler.inputTimes,
                        values = readQuatAccessor(
                            parsed.accessors[sampler.outputAccessorIndex],
                            parsed.bufferViews,
                            parsed.binary
                        ),
                        interpolation = sampler.interpolation
                    )
                }
                GlbAnimationChannel(
                    targetNodeIndex = targetNode,
                    path = path,
                    sampler = valueSampler
                )
            }
            val durationSeconds = channels.maxOfOrNull { it.sampler.durationSeconds } ?: 0.0
            GlbAnimationClip(
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

    private fun parseNodeTransform(node: JsonObject): GlbNodeTransform {
        val translation = node.optionalArray("translation")
        val rotation = node.optionalArray("rotation")
        val scale = node.optionalArray("scale")
        return GlbNodeTransform(
            translation = Vec3(
                translation?.getOrNull(0)?.jsonPrimitive?.double ?: 0.0,
                translation?.getOrNull(1)?.jsonPrimitive?.double ?: 0.0,
                translation?.getOrNull(2)?.jsonPrimitive?.double ?: 0.0
            ),
            rotation = GlbQuat(
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

    private fun parseNodeMatrix(node: JsonObject): GlbMat4 {
        node.optionalArray("matrix")?.let { matrix ->
            require(matrix.size == 16) {
                "GLB node matrix must contain 16 numbers."
            }
            return GlbMat4(DoubleArray(16) { index -> matrix[index].jsonPrimitive.double })
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

        return GlbMat4(
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
    ): GlbMat4 {
        if (!options.normalize) {
            return GlbMat4.identity()
        }

        val maxSpan = maxOf(bounds.width, bounds.height, bounds.depth)
        val scale = if (maxSpan > 0.0) options.targetSize / maxSpan else 1.0
        val centerX = (bounds.min.x + bounds.max.x) * 0.5
        val centerY = if (options.placeOnGround) bounds.min.y else (bounds.min.y + bounds.max.y) * 0.5
        val centerZ = (bounds.min.z + bounds.max.z) * 0.5
        return GlbMat4(
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
    ): List<GlbQuat> {
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
            GlbQuat(
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
    ): List<GlbMat4> {
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
            GlbMat4(DoubleArray(16) { valueIndex ->
                readFloatLE(binary, offset + valueIndex * 4)
            })
        }
    }

    private fun readJointIndexAccessor(
        accessor: GlbAccessor,
        bufferViews: List<GlbBufferView>,
        binary: ByteArray
    ): List<GlbJointIndices> {
        require(accessor.type == "VEC4") {
            "Expected VEC4 joint accessor, got ${accessor.type}."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val componentSize = accessor.componentSize()
        val stride = bufferView.byteStride ?: accessor.byteSize()
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            GlbJointIndices(
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
    ): List<GlbJointWeights> {
        require(accessor.type == "VEC4") {
            "Expected VEC4 weight accessor, got ${accessor.type}."
        }

        val bufferView = bufferViews[accessor.bufferViewIndex]
        val componentSize = accessor.componentSize()
        val stride = bufferView.byteStride ?: accessor.byteSize()
        return List(accessor.count) { index ->
            val offset = bufferView.byteOffset + accessor.byteOffset + index * stride
            GlbJointWeights(
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

    @OptIn(ExperimentalForeignApi::class)
    private fun readBinaryFile(filePath: String): ByteArray {
        val file = fopen(filePath, "rb")
            ?: throw IllegalArgumentException("Cannot open GLB file: $filePath. Please ensure the file exists.")
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

private fun sampleNodeWorldMatrices(
    nodes: List<GlbNode>,
    sceneNodeIndices: List<Int>,
    animations: List<GlbAnimationClip>,
    clipIndex: Int,
    timeSeconds: Double
): List<GlbMat4> {
    val nodeTransforms = nodes.map { it.transform }.toMutableList()
    animations.getOrNull(clipIndex)?.sampleInto(timeSeconds, nodeTransforms)

    val localMatrices = nodes.mapIndexed { index, node ->
        if (nodeTransforms[index] == node.transform) {
            node.matrix
        } else {
            nodeTransforms[index].matrix()
        }
    }
    val worldMatrices = MutableList(nodes.size) { GlbMat4.identity() }

    fun visit(
        nodeIndex: Int,
        parentTransform: GlbMat4
    ) {
        val node = nodes.getOrNull(nodeIndex) ?: return
        val worldTransform = parentTransform * localMatrices[nodeIndex]
        worldMatrices[nodeIndex] = worldTransform
        node.children.forEach { childIndex -> visit(childIndex, worldTransform) }
    }

    sceneNodeIndices.forEach { nodeIndex -> visit(nodeIndex, GlbMat4.identity()) }
    return worldMatrices
}

private fun skinMatricesFor(
    skin: GlbSkin,
    nodeWorldMatrices: List<GlbMat4>
): List<GlbMat4> {
    return skin.joints.mapIndexed { jointOffset, nodeIndex ->
        val jointWorld = nodeWorldMatrices.getOrNull(nodeIndex) ?: GlbMat4.identity()
        jointWorld * skin.inverseBindMatrices.getOrElse(jointOffset) { GlbMat4.identity() }
    }
}

private fun addVectors(
    a: Vec3,
    b: Vec3
): Vec3 {
    return Vec3(a.x + b.x, a.y + b.y, a.z + b.z)
}

private fun scaleVector(
    value: Vec3,
    scale: Double
): Vec3 {
    return Vec3(value.x * scale, value.y * scale, value.z * scale)
}

private fun normalizeVector(value: Vec3): Vec3 {
    val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
    if (length == 0.0) {
        return Vec3(0.0, 1.0, 0.0)
    }
    return Vec3(value.x / length, value.y / length, value.z / length)
}

private data class ParsedGlb(
    val filePath: String,
    val document: JsonObject,
    val binary: ByteArray,
    val bufferViews: List<GlbBufferView>,
    val accessors: List<GlbAccessor>,
    val meshes: List<GlbMesh>,
    val nodes: List<GlbNode>,
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

internal data class GlbNode(
    val name: String?,
    val meshIndex: Int?,
    val skinIndex: Int?,
    val children: List<Int>,
    val transform: GlbNodeTransform,
    val matrix: GlbMat4
)

private data class GlbAnimationSampler(
    val inputTimes: List<Double>,
    val outputAccessorIndex: Int,
    val interpolation: String
)

internal data class GlbAnimationClip(
    val info: GlbAnimationClipInfo,
    val channels: List<GlbAnimationChannel>,
    val durationSeconds: Double
) {
    fun sampleInto(
        timeSeconds: Double,
        transforms: MutableList<GlbNodeTransform>
    ) {
        if (durationSeconds <= 0.0) {
            return
        }

        val localTime = positiveModulo(timeSeconds, durationSeconds)
        channels.forEach { channel ->
            val transform = transforms.getOrNull(channel.targetNodeIndex) ?: return@forEach
            transforms[channel.targetNodeIndex] = when (channel.path) {
                GlbAnimationPath.TRANSLATION -> transform.copy(
                    translation = channel.sampler.sampleVec3(localTime)
                )
                GlbAnimationPath.ROTATION -> transform.copy(
                    rotation = channel.sampler.sampleQuat(localTime)
                )
                GlbAnimationPath.SCALE -> transform.copy(
                    scale = channel.sampler.sampleVec3(localTime)
                )
            }
        }
    }

    private fun positiveModulo(
        value: Double,
        divisor: Double
    ): Double {
        val result = value % divisor
        return if (result < 0.0) result + divisor else result
    }
}

internal data class GlbAnimationChannel(
    val targetNodeIndex: Int,
    val path: GlbAnimationPath,
    val sampler: GlbAnimationValueSampler
)

internal enum class GlbAnimationPath(
    val value: String
) {
    TRANSLATION("translation"),
    ROTATION("rotation"),
    SCALE("scale");

    companion object {
        fun from(value: String): GlbAnimationPath {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unsupported GLB animation target path: $value")
        }
    }
}

internal sealed class GlbAnimationValueSampler(
    val times: List<Double>,
    val interpolation: String
) {
    val durationSeconds: Double = times.lastOrNull() ?: 0.0
    val keyframeCount: Int = times.size

    open fun sampleVec3(timeSeconds: Double): Vec3 {
        throw IllegalStateException("Animation sampler does not contain Vec3 values.")
    }

    open fun sampleQuat(timeSeconds: Double): GlbQuat {
        throw IllegalStateException("Animation sampler does not contain quaternion values.")
    }

    protected fun frameAt(timeSeconds: Double): GlbAnimationFrame {
        require(times.isNotEmpty()) {
            "Animation sampler contains no keyframes."
        }
        if (times.size == 1 || timeSeconds <= times.first()) {
            return GlbAnimationFrame(0, 0, 0.0)
        }
        for (index in 0 until times.size - 1) {
            val start = times[index]
            val end = times[index + 1]
            if (timeSeconds <= end) {
                val amount = if (end > start) ((timeSeconds - start) / (end - start)).coerceIn(0.0, 1.0) else 0.0
                return if (interpolation == "STEP") {
                    GlbAnimationFrame(index, index, 0.0)
                } else {
                    GlbAnimationFrame(index, index + 1, amount)
                }
            }
        }
        val lastIndex = times.lastIndex
        return GlbAnimationFrame(lastIndex, lastIndex, 0.0)
    }

    class Vec3Sampler(
        times: List<Double>,
        private val values: List<Vec3>,
        interpolation: String
    ) : GlbAnimationValueSampler(times, interpolation) {
        override fun sampleVec3(timeSeconds: Double): Vec3 {
            val frame = frameAt(timeSeconds)
            val from = values[frame.fromIndex]
            val to = values[frame.toIndex]
            return Vec3(
                x = from.x + (to.x - from.x) * frame.amount,
                y = from.y + (to.y - from.y) * frame.amount,
                z = from.z + (to.z - from.z) * frame.amount
            )
        }
    }

    class QuatSampler(
        times: List<Double>,
        private val values: List<GlbQuat>,
        interpolation: String
    ) : GlbAnimationValueSampler(times, interpolation) {
        override fun sampleQuat(timeSeconds: Double): GlbQuat {
            val frame = frameAt(timeSeconds)
            return values[frame.fromIndex].slerp(values[frame.toIndex], frame.amount)
        }
    }
}

internal data class GlbAnimationFrame(
    val fromIndex: Int,
    val toIndex: Int,
    val amount: Double
)

internal data class GlbSkin(
    val name: String?,
    val skeletonRootNodeIndex: Int?,
    val joints: List<Int>,
    val inverseBindMatrices: List<GlbMat4>
)

internal data class GlbSkinnedTexturedVertex(
    val position: Vec3,
    val normal: Vec3,
    val color: Color,
    val u: Float,
    val v: Float,
    val joints: GlbJointIndices,
    val weights: GlbJointWeights
) {
    fun toTexturedVertex(skinMatrices: List<GlbMat4>): TexturedLitVertex3D {
        var skinnedPosition = Vec3(0.0, 0.0, 0.0)
        var skinnedNormal = Vec3(0.0, 0.0, 0.0)
        var totalWeight = 0.0

        for (index in 0 until 4) {
            val weight = weights.values[index]
            if (weight <= 0.0) {
                continue
            }
            val skinMatrix = skinMatrices.getOrNull(joints.values[index]) ?: continue
            skinnedPosition = addVectors(skinnedPosition, scaleVector(skinMatrix.transformPoint(position), weight))
            skinnedNormal = addVectors(skinnedNormal, scaleVector(skinMatrix.transformVector(normal), weight))
            totalWeight += weight
        }

        if (totalWeight <= 0.0) {
            skinnedPosition = position
            skinnedNormal = normal
        }

        return TexturedLitVertex3D(
            position = skinnedPosition,
            normal = normalizeVector(skinnedNormal),
            color = color,
            u = u,
            v = v
        )
    }
}

internal data class GlbJointIndices(
    val values: IntArray
)

internal data class GlbJointWeights(
    val values: DoubleArray
) {
    fun normalized(): GlbJointWeights {
        val total = values.sum()
        if (total <= 0.0) {
            return this
        }
        return GlbJointWeights(DoubleArray(values.size) { index -> values[index] / total })
    }
}

internal data class GlbNodeTransform(
    val translation: Vec3,
    val rotation: GlbQuat,
    val scale: Vec3
) {
    fun matrix(): GlbMat4 {
        return GlbMat4.fromTransform(translation, rotation, scale)
    }
}

internal data class GlbQuat(
    val x: Double,
    val y: Double,
    val z: Double,
    val w: Double
) {
    fun normalized(): GlbQuat {
        val length = sqrt(x * x + y * y + z * z + w * w)
        if (length == 0.0) {
            return GlbQuat(0.0, 0.0, 0.0, 1.0)
        }
        return GlbQuat(x / length, y / length, z / length, w / length)
    }

    fun slerp(
        target: GlbQuat,
        amount: Double
    ): GlbQuat {
        var end = target
        var cosine = x * target.x + y * target.y + z * target.z + w * target.w
        if (cosine < 0.0) {
            cosine = -cosine
            end = GlbQuat(-target.x, -target.y, -target.z, -target.w)
        }

        if (cosine > 0.9995) {
            return GlbQuat(
                x = x + (end.x - x) * amount,
                y = y + (end.y - y) * amount,
                z = z + (end.z - z) * amount,
                w = w + (end.w - w) * amount
            ).normalized()
        }

        val angle = acos(cosine.coerceIn(-1.0, 1.0))
        val sine = sin(angle)
        if (abs(sine) < 0.000001) {
            return this
        }

        val fromScale = sin((1.0 - amount) * angle) / sine
        val toScale = sin(amount * angle) / sine
        return GlbQuat(
            x = x * fromScale + end.x * toScale,
            y = y * fromScale + end.y * toScale,
            z = z * fromScale + end.z * toScale,
            w = w * fromScale + end.w * toScale
        ).normalized()
    }
}

private data class GlbMaterialInfo(
    val color: Color,
    val textureIndex: Int?,
    val uvTransform: GlbUvTransform
)

private data class GlbTextureInfo(
    val sourceImageIndex: Int?,
    val addressModeU: GpuTextureAddressMode,
    val addressModeV: GpuTextureAddressMode
)

private data class GlbTextureSamplerInfo(
    val addressModeU: GpuTextureAddressMode,
    val addressModeV: GpuTextureAddressMode
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

internal data class GlbMat4(
    val values: DoubleArray
) {
    operator fun times(other: GlbMat4): GlbMat4 {
        val result = DoubleArray(16)
        for (column in 0 until 4) {
            for (row in 0 until 4) {
                result[column * 4 + row] =
                    values[0 * 4 + row] * other.values[column * 4 + 0] +
                    values[1 * 4 + row] * other.values[column * 4 + 1] +
                    values[2 * 4 + row] * other.values[column * 4 + 2] +
                    values[3 * 4 + row] * other.values[column * 4 + 3]
            }
        }
        return GlbMat4(result)
    }

    fun transformPoint(point: Vec3): Vec3 {
        return Vec3(
            x = values[0] * point.x + values[4] * point.y + values[8] * point.z + values[12],
            y = values[1] * point.x + values[5] * point.y + values[9] * point.z + values[13],
            z = values[2] * point.x + values[6] * point.y + values[10] * point.z + values[14]
        )
    }

    fun transformVector(vector: Vec3): Vec3 {
        return Vec3(
            x = values[0] * vector.x + values[4] * vector.y + values[8] * vector.z,
            y = values[1] * vector.x + values[5] * vector.y + values[9] * vector.z,
            z = values[2] * vector.x + values[6] * vector.y + values[10] * vector.z
        )
    }

    fun toMat4(): Mat4 {
        return Mat4(FloatArray(16) { index -> values[index].toFloat() })
    }

    companion object {
        fun identity(): GlbMat4 {
            return GlbMat4(
                doubleArrayOf(
                    1.0, 0.0, 0.0, 0.0,
                    0.0, 1.0, 0.0, 0.0,
                    0.0, 0.0, 1.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
                )
            )
        }

        fun fromTransform(
            translation: Vec3,
            rotation: GlbQuat,
            scale: Vec3
        ): GlbMat4 {
            val quat = rotation.normalized()
            val xx = quat.x * quat.x
            val yy = quat.y * quat.y
            val zz = quat.z * quat.z
            val xy = quat.x * quat.y
            val xz = quat.x * quat.z
            val yz = quat.y * quat.z
            val wx = quat.w * quat.x
            val wy = quat.w * quat.y
            val wz = quat.w * quat.z
            val m00 = 1.0 - 2.0 * (yy + zz)
            val m01 = 2.0 * (xy + wz)
            val m02 = 2.0 * (xz - wy)
            val m10 = 2.0 * (xy - wz)
            val m11 = 1.0 - 2.0 * (xx + zz)
            val m12 = 2.0 * (yz + wx)
            val m20 = 2.0 * (xz + wy)
            val m21 = 2.0 * (yz - wx)
            val m22 = 1.0 - 2.0 * (xx + yy)
            return GlbMat4(
                doubleArrayOf(
                    m00 * scale.x, m01 * scale.x, m02 * scale.x, 0.0,
                    m10 * scale.y, m11 * scale.y, m12 * scale.y, 0.0,
                    m20 * scale.z, m21 * scale.z, m22 * scale.z, 0.0,
                    translation.x, translation.y, translation.z, 1.0
                )
            )
        }

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
