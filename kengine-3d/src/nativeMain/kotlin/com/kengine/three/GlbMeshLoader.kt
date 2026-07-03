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

object GlbMeshLoader {
    private const val GLB_MAGIC = 0x46546C67
    private const val GLB_VERSION_2 = 2
    private const val JSON_CHUNK = 0x4E4F534A
    private const val BIN_CHUNK = 0x004E4942
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

    fun loadLitVertices(
        assetPath: String,
        options: GlbMeshLoadOptions = GlbMeshLoadOptions()
    ): List<LitVertex3D> {
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
        val materialColors = parseMaterialColors(document, options.defaultColor)
        val vertices = mutableListOf<LitVertex3D>()

        fun visitNode(
            nodeIndex: Int,
            parentTransform: GlbMat4
        ) {
            val node = nodes.getOrNull(nodeIndex) ?: return
            val transform = parentTransform * node.matrix
            node.meshIndex?.let { meshIndex ->
                val mesh = meshes.getOrNull(meshIndex)
                    ?: throw IllegalArgumentException("GLB node references missing mesh $meshIndex: $filePath")
                appendMeshVertices(mesh, transform, accessors, bufferViews, binary, materialColors, vertices)
            }
            node.children.forEach { childIndex -> visitNode(childIndex, transform) }
        }

        sceneNodeIndices.forEach { nodeIndex -> visitNode(nodeIndex, GlbMat4.identity()) }
        require(vertices.isNotEmpty()) {
            "GLB file contains no supported triangle mesh vertices: $filePath"
        }

        return normalizeVertices(vertices, options)
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
        return document.optionalArray("materials")?.mapIndexed { index, element ->
            val colorFactor = element.jsonObject
                .optionalObject("pbrMetallicRoughness")
                ?.optionalArray("baseColorFactor")
            if (colorFactor != null && colorFactor.size >= 3) {
                Color.fromRGBA(
                    r = colorFactor[0].jsonPrimitive.double.toFloat(),
                    g = colorFactor[1].jsonPrimitive.double.toFloat(),
                    b = colorFactor[2].jsonPrimitive.double.toFloat(),
                    a = colorFactor.getOrNull(3)?.jsonPrimitive?.double?.toFloat() ?: 1f
                )
            } else {
                primitiveColor(index, defaultColor)
            }
        }.orEmpty()
    }

    private fun parseNodeMatrix(node: JsonObject): GlbMat4 {
        node.optionalArray("matrix")?.let { matrix ->
            require(matrix.size == 16) {
                "GLB node matrix must contain 16 numbers."
            }
            return GlbMat4(DoubleArray(16) { index -> matrix[index].jsonPrimitive.double })
        }

        val translation = node.optionalArray("translation")
        val scale = node.optionalArray("scale")
        val tx = translation?.getOrNull(0)?.jsonPrimitive?.double ?: 0.0
        val ty = translation?.getOrNull(1)?.jsonPrimitive?.double ?: 0.0
        val tz = translation?.getOrNull(2)?.jsonPrimitive?.double ?: 0.0
        val sx = scale?.getOrNull(0)?.jsonPrimitive?.double ?: 1.0
        val sy = scale?.getOrNull(1)?.jsonPrimitive?.double ?: 1.0
        val sz = scale?.getOrNull(2)?.jsonPrimitive?.double ?: 1.0

        return GlbMat4(
            doubleArrayOf(
                sx, 0.0, 0.0, 0.0,
                0.0, sy, 0.0, 0.0,
                0.0, 0.0, sz, 0.0,
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

    private fun JsonObject.requiredString(name: String): String {
        return this[name]?.jsonPrimitive?.content ?: throw IllegalArgumentException("GLB JSON is missing string '$name'.")
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
    val indicesAccessor: Int?,
    val materialIndex: Int?,
    val mode: Int
)

private data class GlbNode(
    val meshIndex: Int?,
    val children: List<Int>,
    val matrix: GlbMat4
)

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
    }
}
