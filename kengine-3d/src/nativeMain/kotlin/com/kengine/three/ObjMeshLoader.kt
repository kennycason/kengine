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
        val source = readTextFile(filePath, "OBJ model file")
        val triangles = parseObj(source, filePath, options)
        return buildVertices(triangles, options)
    }

    fun loadLitVertices(
        assetPath: String,
        options: ObjMeshLoadOptions = ObjMeshLoadOptions()
    ): List<LitVertex3D> {
        val filePath = File.resolveAssetPath(assetPath)
        val source = readTextFile(filePath, "OBJ model file")
        val triangles = parseObj(source, filePath, options)
        return buildLitVertices(triangles, options)
    }

    fun loadTexturedLitVertices(
        assetPath: String,
        options: ObjMeshLoadOptions = ObjMeshLoadOptions()
    ): List<TexturedLitVertex3D> {
        val filePath = File.resolveAssetPath(assetPath)
        val source = readTextFile(filePath, "OBJ model file")
        val triangles = parseObj(source, filePath, options)
        return buildTexturedLitVertices(triangles, options)
    }

    internal fun loadModelSource(
        assetPath: String,
        options: ObjMeshLoadOptions = ObjMeshLoadOptions()
    ): ObjModelSource3D {
        val filePath = File.resolveAssetPath(assetPath)
        val source = readTextFile(filePath, "OBJ model file")
        val triangles = parseObj(source, filePath, options)
        return buildModelSource(triangles, options)
    }

    private fun parseObj(
        source: String,
        filePath: String,
        options: ObjMeshLoadOptions
    ): List<ObjTriangle> {
        val positions = mutableListOf<Vec3>()
        val textureCoordinates = mutableListOf<ObjUv>()
        val materials = mutableMapOf<String, ObjMaterial>()
        val triangles = mutableListOf<ObjTriangle>()
        var currentMaterial = ObjMaterial(color = options.defaultColor)

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
                    val materialPath = ModelResourcePath3D.resolveSiblingPath(
                        filePath = filePath,
                        path = tokens.drop(1).joinToString(" "),
                        resolveRootRelativeFromParent = true
                    )
                    materials.putAll(
                        parseMaterialLibrary(
                            source = readTextFile(
                                filePath = materialPath,
                                description = "OBJ material library",
                                referencedBy = filePath
                            ),
                            materialPath = materialPath
                        )
                    )
                }

                "usemtl" -> {
                    val materialName = tokens.drop(1).joinToString(" ")
                    currentMaterial = materials[materialName]
                        ?: ObjMaterial(name = materialName, color = options.defaultColor)
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
                            currentMaterial
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

    private fun parseMaterialLibrary(
        source: String,
        materialPath: String
    ): Map<String, ObjMaterial> {
        val materials = mutableMapOf<String, ObjMaterial>()
        var materialName: String? = null
        var materialColor = Color.fromHex("ffffff")
        var materialTexturePaths = ObjMaterialTexturePaths()

        fun commitMaterial() {
            val name = materialName ?: return
            materials[name] = ObjMaterial(
                name = name,
                color = materialColor,
                texturePaths = materialTexturePaths
            )
        }

        fun parseTexturePath(tokens: List<String>): String? {
            return parseMapPath(tokens)
                ?.let {
                    ModelResourcePath3D.resolveSiblingPath(
                        filePath = materialPath,
                        path = it,
                        resolveRootRelativeFromParent = true
                    )
                }
        }

        source.lines().forEach { rawLine ->
            val line = rawLine.substringBefore("#").trim()
            if (line.isEmpty()) {
                return@forEach
            }

            val tokens = line.split(WHITESPACE)
            when (tokens.first()) {
                "newmtl" -> {
                    commitMaterial()
                    materialName = tokens.drop(1).joinToString(" ")
                    materialColor = Color.fromHex("ffffff")
                    materialTexturePaths = ObjMaterialTexturePaths()
                }

                "Kd" -> {
                    if (tokens.size >= 4) {
                        materialColor = Color.fromRGBA(
                            tokens[1].toFloat(),
                            tokens[2].toFloat(),
                            tokens[3].toFloat()
                        )
                    }
                }

                "map_Kd" -> {
                    materialTexturePaths = materialTexturePaths.copy(baseColor = parseTexturePath(tokens))
                }

                "map_Bump", "bump", "norm" -> {
                    materialTexturePaths = materialTexturePaths.copy(normal = parseTexturePath(tokens))
                }

                "map_Ks" -> {
                    materialTexturePaths = materialTexturePaths.copy(specular = parseTexturePath(tokens))
                }

                "map_Pr" -> {
                    materialTexturePaths = materialTexturePaths.copy(roughness = parseTexturePath(tokens))
                }

                "map_Pm" -> {
                    materialTexturePaths = materialTexturePaths.copy(metallic = parseTexturePath(tokens))
                }

                "map_Ke" -> {
                    materialTexturePaths = materialTexturePaths.copy(emissive = parseTexturePath(tokens))
                }

                "map_Ka" -> {
                    materialTexturePaths = materialTexturePaths.copy(ambient = parseTexturePath(tokens))
                }

                "map_d" -> {
                    materialTexturePaths = materialTexturePaths.copy(alpha = parseTexturePath(tokens))
                }

                "disp", "map_disp" -> {
                    materialTexturePaths = materialTexturePaths.copy(displacement = parseTexturePath(tokens))
                }
            }
        }

        commitMaterial()
        return materials
    }

    private fun parseMapPath(tokens: List<String>): String? {
        var index = 1
        while (index < tokens.size) {
            val token = tokens[index]
            if (!token.startsWith("-")) {
                return tokens.drop(index).joinToString(" ")
            }
            index += when (token) {
                "-blendu", "-blendv", "-boost", "-bm", "-cc", "-clamp", "-imfchan", "-texres", "-type" -> 2
                "-mm" -> 3
                "-o", "-s", "-t" -> 4
                else -> 1
            }
        }
        return null
    }

    private fun buildVertices(
        triangles: List<ObjTriangle>,
        options: ObjMeshLoadOptions
    ): List<Vertex3D> {
        val transform = ObjGeometryTransform.fromTriangles(triangles, options)
        return buildVertices(triangles, transform, options)
    }

    private fun buildVertices(
        triangles: List<ObjTriangle>,
        transform: ObjGeometryTransform,
        options: ObjMeshLoadOptions
    ): List<Vertex3D> {
        return triangles.flatMap { triangle ->
            val a = transform.apply(triangle.a.position)
            val b = transform.apply(triangle.b.position)
            val c = transform.apply(triangle.c.position)
            val color = if (options.shadeFaces) {
                shadeColor(triangle.material.color, faceLight(a, b, c))
            } else {
                triangle.material.color
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
        val transform = ObjGeometryTransform.fromTriangles(triangles, options)
        return buildLitVertices(triangles, transform)
    }

    private fun buildLitVertices(
        triangles: List<ObjTriangle>,
        transform: ObjGeometryTransform
    ): List<LitVertex3D> {
        return triangles.flatMap { triangle ->
            val a = transform.apply(triangle.a.position)
            val b = transform.apply(triangle.b.position)
            val c = transform.apply(triangle.c.position)
            val normal = normalize(cross(subtract(b, a), subtract(c, a)))

            listOf(
                LitVertex3D(a, normal, triangle.material.color),
                LitVertex3D(b, normal, triangle.material.color),
                LitVertex3D(c, normal, triangle.material.color)
            )
        }
    }

    private fun buildTexturedLitVertices(
        triangles: List<ObjTriangle>,
        options: ObjMeshLoadOptions
    ): List<TexturedLitVertex3D> {
        val transform = ObjGeometryTransform.fromTriangles(triangles, options)
        return buildTexturedLitVertices(triangles, transform, options)
    }

    private fun buildTexturedLitVertices(
        triangles: List<ObjTriangle>,
        transform: ObjGeometryTransform,
        options: ObjMeshLoadOptions
    ): List<TexturedLitVertex3D> {
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
            val a = transform.apply(triangle.a.position)
            val b = transform.apply(triangle.b.position)
            val c = transform.apply(triangle.c.position)
            val normal = normalize(cross(subtract(b, a), subtract(c, a)))
            val uvA = uvOrFallback(triangle.a.uv, ObjUv(0f, 1f))
            val uvB = uvOrFallback(triangle.b.uv, ObjUv(0f, 0f))
            val uvC = uvOrFallback(triangle.c.uv, ObjUv(1f, 0f))

            listOf(
                TexturedLitVertex3D(a, normal, triangle.material.color, uvA.u, textureV(uvA)),
                TexturedLitVertex3D(b, normal, triangle.material.color, uvB.u, textureV(uvB)),
                TexturedLitVertex3D(c, normal, triangle.material.color, uvC.u, textureV(uvC))
            )
        }
    }

    private fun buildModelSource(
        triangles: List<ObjTriangle>,
        options: ObjMeshLoadOptions
    ): ObjModelSource3D {
        val transform = ObjGeometryTransform.fromTriangles(triangles, options)
        val trianglesByMaterial = linkedMapOf<ObjMaterial, MutableList<ObjTriangle>>()
        triangles.forEach { triangle ->
            trianglesByMaterial.getOrPut(triangle.material) { mutableListOf() } += triangle
        }

        val parts = mutableListOf<ModelPartSource3D>()
        val litVertices = mutableListOf<LitVertex3D>()
        trianglesByMaterial.forEach { (material, materialTriangles) ->
            val textureAsset = material.texturePaths.baseColor?.let(GpuTextureAsset3D::file)
            val textureSet = material.texturePaths.toTextureSet3D()
            if (textureAsset != null) {
                val texturedVertices = buildTexturedLitVertices(materialTriangles, transform, options)
                litVertices += texturedVertices.map { it.toLitVertex() }
                parts += ModelPartSource3D.texturedLit(
                    vertices = texturedVertices,
                    materialDescriptor = MaterialDescriptor3D.textured(
                        textureAsset = textureAsset,
                        color = material.color,
                        name = material.name,
                        textures = textureSet
                    )
                )
            } else {
                val partVertices = buildLitVertices(materialTriangles, transform)
                litVertices += partVertices
                parts += ModelPartSource3D.lit(
                    vertices = partVertices,
                    materialDescriptor = MaterialDescriptor3D.solid(
                        color = material.color,
                        name = material.name,
                        textures = textureSet
                    )
                )
            }
        }

        val usedMaterials = trianglesByMaterial.keys
        return ObjModelSource3D(
            litVertices = litVertices,
            parts = parts,
            materialCount = usedMaterials.count { it.name != null },
            textureCount = usedMaterials.flatMap { it.texturePaths.all }.distinct().size,
            textureSlotUsage = MaterialTextureSlotUsage3D.fromTextureSets(
                usedMaterials.map { it.texturePaths.toTextureSet3D() }
            )
        )
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

    private val WHITESPACE = Regex("\\s+")
}

internal data class ObjModelSource3D(
    val litVertices: List<LitVertex3D>,
    val parts: List<ModelPartSource3D>,
    val materialCount: Int,
    val textureCount: Int,
    val textureSlotUsage: MaterialTextureSlotUsage3D
)

private data class ObjTriangle(
    val a: ObjFaceVertex,
    val b: ObjFaceVertex,
    val c: ObjFaceVertex,
    val material: ObjMaterial
)

private data class ObjMaterial(
    val name: String? = null,
    val color: Color,
    val texturePaths: ObjMaterialTexturePaths = ObjMaterialTexturePaths()
)

private data class ObjMaterialTexturePaths(
    val baseColor: String? = null,
    val normal: String? = null,
    val specular: String? = null,
    val roughness: String? = null,
    val metallic: String? = null,
    val emissive: String? = null,
    val ambient: String? = null,
    val alpha: String? = null,
    val displacement: String? = null
) {
    val all: List<String>
        get() = listOfNotNull(
            baseColor,
            normal,
            specular,
            roughness,
            metallic,
            emissive,
            ambient,
            alpha,
            displacement
        )

    fun toTextureSet3D(): MaterialTextureSet3D {
        return MaterialTextureSet3D(
            baseColor = baseColor?.let(GpuTextureAsset3D::file),
            normal = normal?.let(GpuTextureAsset3D::file),
            specular = specular?.let(GpuTextureAsset3D::file),
            roughness = roughness?.let(GpuTextureAsset3D::file),
            metallic = metallic?.let(GpuTextureAsset3D::file),
            emissive = emissive?.let(GpuTextureAsset3D::file),
            ambient = ambient?.let(GpuTextureAsset3D::file),
            alpha = alpha?.let(GpuTextureAsset3D::file),
            displacement = displacement?.let(GpuTextureAsset3D::file)
        )
    }
}

private data class ObjFaceVertex(
    val position: Vec3,
    val uv: ObjUv?
)

private data class ObjUv(
    val u: Float,
    val v: Float
)

private data class ObjGeometryTransform(
    val center: Vec3,
    val scale: Double,
    val enabled: Boolean
) {
    fun apply(position: Vec3): Vec3 {
        if (!enabled) {
            return position
        }
        return Vec3(
            (position.x - center.x) * scale,
            (position.y - center.y) * scale,
            (position.z - center.z) * scale
        )
    }

    companion object {
        fun fromTriangles(
            triangles: List<ObjTriangle>,
            options: ObjMeshLoadOptions
        ): ObjGeometryTransform {
            if (!options.normalize) {
                return ObjGeometryTransform(
                    center = Vec3(0.0, 0.0, 0.0),
                    scale = 1.0,
                    enabled = false
                )
            }
            val bounds = ObjBounds.fromTriangles(triangles)
            val maxSpan = maxOf(bounds.width, bounds.height, bounds.depth)
            return ObjGeometryTransform(
                center = bounds.center(),
                scale = if (maxSpan > 0.0) options.targetSize / maxSpan else 1.0,
                enabled = true
            )
        }
    }
}

private fun TexturedLitVertex3D.toLitVertex(): LitVertex3D {
    return LitVertex3D(
        position = position,
        normal = normal,
        color = color
    )
}

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
