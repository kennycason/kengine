package com.kengine.three

import com.kengine.file.File
import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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
    val materialCount: Int,
    val textureCount: Int,
    val imageCount: Int,
    val hasTexturedMaterials: Boolean,
    val hasSkeleton: Boolean = skinCount > 0,
    val hasAnimations: Boolean = animationCount > 0
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
        return GlbModelInfo(
            skinCount = parsed.document.optionalArray("skins")?.size ?: 0,
            animationCount = parsed.document.optionalArray("animations")?.size ?: 0,
            materialCount = materialInfos.size,
            textureCount = parsed.document.optionalArray("textures")?.size ?: 0,
            imageCount = parsed.document.optionalArray("images")?.size ?: 0,
            hasTexturedMaterials = materialInfos.any { it.textureIndex != null }
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
        return GpuTexture.fromEncodedBytes(gpu, imageBytes, "$assetPath image $imageIndex")
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
                type = item.requiredString("type")
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
                meshIndex = item.optionalInt("mesh"),
                children = item.optionalArray("children")?.map { it.jsonPrimitive.int }.orEmpty(),
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
        return document.optionalArray("textures")?.map { element ->
            val item = element.jsonObject
            GlbTextureInfo(sourceImageIndex = item.optionalInt("source"))
        }.orEmpty()
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
    val type: String
)

private data class GlbMesh(
    val primitives: List<GlbPrimitive>
)

private data class GlbPrimitive(
    val positionAccessor: Int,
    val normalAccessor: Int?,
    val texCoordAccessor: Int?,
    val indicesAccessor: Int?,
    val materialIndex: Int?,
    val mode: Int
)

private data class GlbNode(
    val meshIndex: Int?,
    val children: List<Int>,
    val matrix: GlbMat4
)

private data class GlbMaterialInfo(
    val color: Color,
    val textureIndex: Int?,
    val uvTransform: GlbUvTransform
)

private data class GlbTextureInfo(
    val sourceImageIndex: Int?
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

private data class GlbMat4(
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
