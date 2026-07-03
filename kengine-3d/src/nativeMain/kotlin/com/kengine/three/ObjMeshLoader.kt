package com.kengine.three

import com.kengine.file.File
import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ObjMeshLoadOptions(
    val normalize: Boolean = true,
    val targetSize: Double = 1.8,
    val defaultColor: Color = Color.fromHex("d8e3f0"),
    val shadeFaces: Boolean = true,
    val flipTextureV: Boolean = true
)

object ObjMeshLoader {
    fun load(
        gpu: GpuContext,
        assetPath: String,
        options: ObjMeshLoadOptions = ObjMeshLoadOptions()
    ): GpuMesh {
        return GpuMesh.create(gpu, loadVertices(assetPath, options))
    }

    fun loadLit(
        gpu: GpuContext,
        assetPath: String,
        options: ObjMeshLoadOptions = ObjMeshLoadOptions()
    ): LitGpuMesh {
        return LitGpuMesh.create(gpu, loadLitVertices(assetPath, options))
    }

    fun loadTexturedLit(
        gpu: GpuContext,
        assetPath: String,
        options: ObjMeshLoadOptions = ObjMeshLoadOptions()
    ): TexturedLitGpuMesh {
        return TexturedLitGpuMesh.create(gpu, loadTexturedLitVertices(assetPath, options))
    }

    fun loadVertices(
        assetPath: String,
        options: ObjMeshLoadOptions = ObjMeshLoadOptions()
    ): List<Vertex3D> {
        val filePath = File.resolveAssetPath(assetPath)
        val source = readTextFile(filePath)
        val triangles = parseObj(source, filePath, options)
        return buildVertices(triangles, options)
    }

    fun loadLitVertices(
        assetPath: String,
        options: ObjMeshLoadOptions = ObjMeshLoadOptions()
    ): List<LitVertex3D> {
        val filePath = File.resolveAssetPath(assetPath)
        val source = readTextFile(filePath)
        val triangles = parseObj(source, filePath, options)
        return buildLitVertices(triangles, options)
    }

    fun loadTexturedLitVertices(
        assetPath: String,
        options: ObjMeshLoadOptions = ObjMeshLoadOptions()
    ): List<TexturedLitVertex3D> {
        val filePath = File.resolveAssetPath(assetPath)
        val source = readTextFile(filePath)
        val triangles = parseObj(source, filePath, options)
        return buildTexturedLitVertices(triangles, options)
    }

    private fun parseObj(
        source: String,
        filePath: String,
        options: ObjMeshLoadOptions
    ): List<ObjTriangle> {
        val positions = mutableListOf<Vec3>()
        val textureCoordinates = mutableListOf<ObjUv>()
        val materialColors = mutableMapOf<String, Color>()
        val triangles = mutableListOf<ObjTriangle>()
        var currentColor = options.defaultColor

        source.lines().forEachIndexed { lineIndex, rawLine ->
            val line = rawLine.substringBefore("#").trim()
            if (line.isEmpty()) {
                return@forEachIndexed
            }

            val tokens = line.split(WHITESPACE)
            when (tokens.first()) {
                "v" -> {
                    require(tokens.size >= 4) {
                        "Invalid OBJ vertex at ${lineIndex + 1} in $filePath: $rawLine"
                    }
                    positions += Vec3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                }

                "vt" -> {
                    require(tokens.size >= 3) {
                        "Invalid OBJ texture coordinate at ${lineIndex + 1} in $filePath: $rawLine"
                    }
                    textureCoordinates += ObjUv(tokens[1].toFloat(), tokens[2].toFloat())
                }

                "mtllib" -> {
                    require(tokens.size >= 2) {
                        "Invalid OBJ material library at ${lineIndex + 1} in $filePath: $rawLine"
                    }
                    val materialPath = joinPath(parentDirectory(filePath), tokens.drop(1).joinToString(" "))
                    materialColors.putAll(parseMaterialLibrary(readTextFile(materialPath)))
                }

                "usemtl" -> {
                    currentColor = materialColors[tokens.drop(1).joinToString(" ")] ?: options.defaultColor
                }

                "f" -> {
                    require(tokens.size >= 4) {
                        "Invalid OBJ face at ${lineIndex + 1} in $filePath: $rawLine"
                    }
                    val faceVertices = tokens.drop(1).map {
                        parseFaceVertex(it, positions, textureCoordinates, lineIndex + 1, filePath)
                    }
                    for (index in 1 until faceVertices.lastIndex) {
                        triangles += ObjTriangle(
                            faceVertices[0],
                            faceVertices[index],
                            faceVertices[index + 1],
                            currentColor
                        )
                    }
                }
            }
        }

        require(triangles.isNotEmpty()) {
            "OBJ file contains no renderable faces: $filePath"
        }

        return triangles
    }

    private fun parseMaterialLibrary(source: String): Map<String, Color> {
        val colors = mutableMapOf<String, Color>()
        var materialName: String? = null

        source.lines().forEach { rawLine ->
            val line = rawLine.substringBefore("#").trim()
            if (line.isEmpty()) {
                return@forEach
            }

            val tokens = line.split(WHITESPACE)
            when (tokens.first()) {
                "newmtl" -> {
                    materialName = tokens.drop(1).joinToString(" ")
                }

                "Kd" -> {
                    val name = materialName ?: return@forEach
                    if (tokens.size >= 4) {
                        colors[name] = Color.fromRGBA(
                            tokens[1].toFloat(),
                            tokens[2].toFloat(),
                            tokens[3].toFloat()
                        )
                    }
                }
            }
        }

        return colors
    }

    private fun buildVertices(
        triangles: List<ObjTriangle>,
        options: ObjMeshLoadOptions
    ): List<Vertex3D> {
        val bounds = ObjBounds.fromTriangles(triangles)
        val center = bounds.center()
        val scale = if (options.normalize) {
            val maxSpan = maxOf(bounds.width, bounds.height, bounds.depth)
            if (maxSpan > 0.0) options.targetSize / maxSpan else 1.0
        } else {
            1.0
        }

        fun transform(position: Vec3): Vec3 {
            if (!options.normalize) {
                return position
            }
            return Vec3(
                (position.x - center.x) * scale,
                (position.y - center.y) * scale,
                (position.z - center.z) * scale
            )
        }

        return triangles.flatMap { triangle ->
            val a = transform(triangle.a.position)
            val b = transform(triangle.b.position)
            val c = transform(triangle.c.position)
            val color = if (options.shadeFaces) {
                shadeColor(triangle.color, faceLight(a, b, c))
            } else {
                triangle.color
            }

            listOf(
                Vertex3D(a, color),
                Vertex3D(b, color),
                Vertex3D(c, color)
            )
        }
    }

    private fun buildLitVertices(
        triangles: List<ObjTriangle>,
        options: ObjMeshLoadOptions
    ): List<LitVertex3D> {
        val bounds = ObjBounds.fromTriangles(triangles)
        val center = bounds.center()
        val scale = if (options.normalize) {
            val maxSpan = maxOf(bounds.width, bounds.height, bounds.depth)
            if (maxSpan > 0.0) options.targetSize / maxSpan else 1.0
        } else {
            1.0
        }

        fun transform(position: Vec3): Vec3 {
            if (!options.normalize) {
                return position
            }
            return Vec3(
                (position.x - center.x) * scale,
                (position.y - center.y) * scale,
                (position.z - center.z) * scale
            )
        }

        return triangles.flatMap { triangle ->
            val a = transform(triangle.a.position)
            val b = transform(triangle.b.position)
            val c = transform(triangle.c.position)
            val normal = normalize(cross(subtract(b, a), subtract(c, a)))

            listOf(
                LitVertex3D(a, normal, triangle.color),
                LitVertex3D(b, normal, triangle.color),
                LitVertex3D(c, normal, triangle.color)
            )
        }
    }

    private fun buildTexturedLitVertices(
        triangles: List<ObjTriangle>,
        options: ObjMeshLoadOptions
    ): List<TexturedLitVertex3D> {
        val bounds = ObjBounds.fromTriangles(triangles)
        val center = bounds.center()
        val scale = if (options.normalize) {
            val maxSpan = maxOf(bounds.width, bounds.height, bounds.depth)
            if (maxSpan > 0.0) options.targetSize / maxSpan else 1.0
        } else {
            1.0
        }

        fun transform(position: Vec3): Vec3 {
            if (!options.normalize) {
                return position
            }
            return Vec3(
                (position.x - center.x) * scale,
                (position.y - center.y) * scale,
                (position.z - center.z) * scale
            )
        }

        fun uvOrFallback(
            uv: ObjUv?,
            fallback: ObjUv
        ): ObjUv {
            return uv ?: fallback
        }

        fun textureV(uv: ObjUv): Float {
            return if (options.flipTextureV) 1f - uv.v else uv.v
        }

        return triangles.flatMap { triangle ->
            val a = transform(triangle.a.position)
            val b = transform(triangle.b.position)
            val c = transform(triangle.c.position)
            val normal = normalize(cross(subtract(b, a), subtract(c, a)))
            val uvA = uvOrFallback(triangle.a.uv, ObjUv(0f, 1f))
            val uvB = uvOrFallback(triangle.b.uv, ObjUv(0f, 0f))
            val uvC = uvOrFallback(triangle.c.uv, ObjUv(1f, 0f))

            listOf(
                TexturedLitVertex3D(a, normal, triangle.color, uvA.u, textureV(uvA)),
                TexturedLitVertex3D(b, normal, triangle.color, uvB.u, textureV(uvB)),
                TexturedLitVertex3D(c, normal, triangle.color, uvC.u, textureV(uvC))
            )
        }
    }

    private fun parseFaceVertex(
        faceToken: String,
        positions: List<Vec3>,
        textureCoordinates: List<ObjUv>,
        lineNumber: Int,
        filePath: String
    ): ObjFaceVertex {
        val parts = faceToken.split("/")
        val positionToken = parts.firstOrNull().orEmpty()
        require(positionToken.isNotBlank()) {
            "OBJ face is missing a position index at $lineNumber in $filePath: $faceToken"
        }

        val positionIndex = parseObjIndex(
            positionToken,
            positions.size,
            "position",
            lineNumber,
            filePath
        )
        val uv = if (parts.size >= 2 && parts[1].isNotBlank()) {
            val uvIndex = parseObjIndex(
                parts[1],
                textureCoordinates.size,
                "texture coordinate",
                lineNumber,
                filePath
            )
            textureCoordinates[uvIndex]
        } else {
            null
        }

        return ObjFaceVertex(positions[positionIndex], uv)
    }

    private fun parseObjIndex(
        indexToken: String,
        positionCount: Int,
        kind: String,
        lineNumber: Int,
        filePath: String
    ): Int {
        val rawIndex = indexToken.toInt()
        val index = if (rawIndex > 0) rawIndex - 1 else positionCount + rawIndex
        require(index in 0 until positionCount) {
            "OBJ $kind index $rawIndex is out of bounds at $lineNumber in $filePath."
        }
        return index
    }

    private fun faceLight(
        a: Vec3,
        b: Vec3,
        c: Vec3
    ): Double {
        val normal = normalize(cross(subtract(b, a), subtract(c, a)))
        val light = normalize(Vec3(-0.35, 0.85, 0.6))
        return 0.42 + abs(dot(normal, light)) * 0.58
    }

    private fun shadeColor(color: Color, intensity: Double): Color {
        fun shade(channel: UByte): UByte {
            return (channel.toInt() * intensity)
                .roundToInt()
                .coerceIn(0, 255)
                .toUByte()
        }

        return Color(
            r = shade(color.r),
            g = shade(color.g),
            b = shade(color.b),
            a = color.a
        )
    }

    private fun subtract(a: Vec3, b: Vec3): Vec3 {
        return Vec3(a.x - b.x, a.y - b.y, a.z - b.z)
    }

    private fun cross(a: Vec3, b: Vec3): Vec3 {
        return Vec3(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        )
    }

    private fun dot(a: Vec3, b: Vec3): Double {
        return a.x * b.x + a.y * b.y + a.z * b.z
    }

    private fun normalize(value: Vec3): Vec3 {
        val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
        if (length == 0.0) {
            return Vec3(0.0, 0.0, 0.0)
        }
        return Vec3(value.x / length, value.y / length, value.z / length)
    }

    private fun parentDirectory(path: String): String {
        val index = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        return if (index >= 0) path.substring(0, index) else "."
    }

    private fun joinPath(
        parent: String,
        child: String
    ): String {
        if (parent.endsWith("/") || parent.endsWith("\\")) {
            return parent + child
        }
        return "$parent/$child"
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readTextFile(filePath: String): String {
        val file = fopen(filePath, "r")
            ?: throw IllegalArgumentException("Cannot open file: $filePath. Please ensure the file exists and the path is correct.")
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

    private val WHITESPACE = Regex("\\s+")
}

private data class ObjTriangle(
    val a: ObjFaceVertex,
    val b: ObjFaceVertex,
    val c: ObjFaceVertex,
    val color: Color
)

private data class ObjFaceVertex(
    val position: Vec3,
    val uv: ObjUv?
)

private data class ObjUv(
    val u: Float,
    val v: Float
)

private data class ObjBounds(
    val min: Vec3,
    val max: Vec3
) {
    val width: Double = max.x - min.x
    val height: Double = max.y - min.y
    val depth: Double = max.z - min.z

    fun center(): Vec3 {
        return Vec3(
            (min.x + max.x) * 0.5,
            (min.y + max.y) * 0.5,
            (min.z + max.z) * 0.5
        )
    }

    companion object {
        fun fromTriangles(triangles: List<ObjTriangle>): ObjBounds {
            var minX = Double.POSITIVE_INFINITY
            var minY = Double.POSITIVE_INFINITY
            var minZ = Double.POSITIVE_INFINITY
            var maxX = Double.NEGATIVE_INFINITY
            var maxY = Double.NEGATIVE_INFINITY
            var maxZ = Double.NEGATIVE_INFINITY

            triangles.forEach { triangle ->
                listOf(triangle.a.position, triangle.b.position, triangle.c.position).forEach { position ->
                    minX = minOf(minX, position.x)
                    minY = minOf(minY, position.y)
                    minZ = minOf(minZ, position.z)
                    maxX = maxOf(maxX, position.x)
                    maxY = maxOf(maxY, position.y)
                    maxZ = maxOf(maxZ, position.z)
                }
            }

            return ObjBounds(
                min = Vec3(minX, minY, minZ),
                max = Vec3(maxX, maxY, maxZ)
            )
        }
    }
}
