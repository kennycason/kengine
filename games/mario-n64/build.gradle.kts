import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.n64-game")
}

group = "kengine.n64.mario"
version = "1.0.0"

val battlefieldDaeFile = file("assets/models/bob-omb-battlefield/Area1.dae")
val battlefieldTextureDir = file("assets/models/bob-omb-battlefield")
val marioDaeFile = file("assets/models/mario-static/Mario64Static.dae")
val marioTextureDir = file("assets/models/mario-static")
val marioPoseTextureDir = file("assets/models/mario-animated-poses")
val marioPoseDaeFiles = linkedMapOf(
    "mario-idle" to file("assets/models/mario-animated-poses/mario-idle.dae"),
    "mario-walk-a" to file("assets/models/mario-animated-poses/mario-walk-a.dae"),
    "mario-walk-b" to file("assets/models/mario-animated-poses/mario-walk-b.dae"),
    "mario-run-a" to file("assets/models/mario-animated-poses/mario-run-a.dae"),
    "mario-run-b" to file("assets/models/mario-animated-poses/mario-run-b.dae"),
    "mario-jump" to file("assets/models/mario-animated-poses/mario-jump.dae"),
    "mario-fall" to file("assets/models/mario-animated-poses/mario-fall.dae")
)
val marioTextureWrapModes = mapOf(
    "Mario64Static_texture_0001.png" to BakedTextureWrapModes.clampBoth(),
    "Mario64Static_texture_0002.png" to BakedTextureWrapModes.clampS(),
    "Mario64Static_texture_0006.png" to BakedTextureWrapModes.clampBoth(),
    "Mario64Static_texture_0008.png" to BakedTextureWrapModes.clampS()
)

val generateMario64ModelAssets by tasks.registering {
    group = "n64"
    description = "Bakes Bob-Omb Battlefield and Mario COLLADA assets for the N64 renderer."

    inputs.file(battlefieldDaeFile)
    inputs.dir(battlefieldTextureDir)
    inputs.file(marioDaeFile)
    inputs.dir(marioTextureDir)
    inputs.files(marioPoseDaeFiles.values)
    inputs.dir(marioPoseTextureDir)
    val kotlinOutputFile = layout.projectDirectory.file("src/commonMain/kotlin/mario64/Mario64ModelAssets.kt")
    val collisionKotlinOutputFile = layout.projectDirectory.file("src/commonMain/kotlin/mario64/Mario64CollisionAssets.kt")
    val cOutputFile = layout.projectDirectory.file("src/main/c/kengine_n64_world_mesh.h")
    outputs.file(kotlinOutputFile)
    outputs.file(collisionKotlinOutputFile)
    outputs.file(cOutputFile)

    doLast {
        val battlefield = parseDaeWorld(battlefieldDaeFile, battlefieldTextureDir)
        val staticMario = parseDaeWorld(
            marioDaeFile,
            marioTextureDir,
            targetSize = 180.0,
            placeOnGround = true,
            mergeEquivalentMaterials = true,
            textureWrapModes = marioTextureWrapModes
        )
        val marioPoses = marioPoseDaeFiles.map { (assetName, daeFile) ->
            assetName to parseDaeWorld(
                daeFile,
                marioPoseTextureDir,
                targetSize = 180.0,
                placeOnGround = true,
                mergeEquivalentMaterials = true
            )
        }
        val mario = marioPoses.first().second

        val kotlinOut = kotlinOutputFile.asFile
        kotlinOut.parentFile.mkdirs()
        writeTextIfChanged(
            kotlinOut,
            renderMario64ModelAssets(battlefield, mario, marioPoses.size, staticMario)
        )

        val collisionKotlinOut = collisionKotlinOutputFile.asFile
        collisionKotlinOut.parentFile.mkdirs()
        writeTextIfChanged(collisionKotlinOut, renderMario64CollisionAssets(battlefield.collision))

        val cOut = cOutputFile.asFile
        cOut.parentFile.mkdirs()
        writeTextIfChanged(cOut, renderMario64MeshC(battlefield, marioPoses))

        println(
            "Mario64 world: ${battlefield.vertexCount} render vertices, ${battlefield.triangleCount} render triangles, " +
                "${battlefield.collision.vertexCount} collision vertices, ${battlefield.collision.triangleCount} collision triangles, " +
                "${battlefield.colors.size} materials; Mario: ${mario.vertexCount} vertices, " +
                "${mario.triangleCount} triangles, ${mario.colors.size} materials, ${marioPoses.size} baked poses"
        )
    }
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }

    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"

    when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64()
        hostOs == "Mac OS X" && !isArm64 -> macosX64()
        hostOs == "Linux" && isArm64 -> linuxArm64()
        hostOs == "Linux" && !isArm64 -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
        else -> throw GradleException("Host OS [$hostOs] is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kengine-core"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

kengineN64 {
    artifactBaseName.set("mario-n64")
    displayName.set("Mario 64 World")
    mainClass.set("mario64.Mario64Game")
}

tasks.matching { task ->
    task.name.startsWith("compile") && task.name.contains("Kotlin")
}.configureEach {
    dependsOn(generateMario64ModelAssets)
}

gradle.projectsEvaluated {
    rootProject.findProject(":kengine-n64")?.tasks?.matching { task ->
        task.name == "compileMarioN64KotlinStatic"
    }?.configureEach {
        dependsOn(generateMario64ModelAssets)
    }
}

// ---------------------------------------------------------------------------
// COLLADA DAE Parser
// ---------------------------------------------------------------------------

data class BakedMario64World(
    val vertices: List<Int>,
    val triangles: List<Int>,
    val colors: List<Int>,
    val materialModes: List<Int>,
    val vertexCount: Int,
    val triangleCount: Int,
    val hasUVs: Boolean,
    val textures: List<BakedMario64Texture>,
    val collision: BakedCollisionWorld
)

data class BakedCollisionWorld(
    val vertices: List<Int>,
    val triangles: List<Int>,
    val cellOffsets: List<Int>,
    val cellTriangleIndices: List<Int>,
    val vertexCount: Int,
    val triangleCount: Int,
    val floorTriangleCount: Int,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
    val gridWidth: Int,
    val gridDepth: Int,
    val cellSize: Int
)

data class BakedMario64Texture(
    val materialIndex: Int,
    val filename: String,
    val width: Int,
    val height: Int,
    val format: BakedTextureFormat,
    val materialMode: BakedMaterialMode,
    val wrapS: BakedTextureWrap,
    val wrapT: BakedTextureWrap,
    val texelData: ShortArray
)

enum class BakedTextureFormat(val cName: String) {
    RGBA16("KENGINE_WORLD_TEXTURE_RGBA16"),
    IA16("KENGINE_WORLD_TEXTURE_IA16")
}

enum class BakedMaterialMode(val value: Int, val cName: String) {
    OPAQUE(0, "KENGINE_WORLD_MATERIAL_OPAQUE"),
    MASKED(1, "KENGINE_WORLD_MATERIAL_MASKED"),
    TRANSLUCENT_DECAL(2, "KENGINE_WORLD_MATERIAL_TRANSLUCENT_DECAL")
}

enum class BakedTextureWrap(val cName: String) {
    REPEAT("KENGINE_WORLD_TEXTURE_REPEAT"),
    CLAMP_TO_EDGE("KENGINE_WORLD_TEXTURE_CLAMP_TO_EDGE"),
    MIRRORED_REPEAT("KENGINE_WORLD_TEXTURE_MIRRORED_REPEAT")
}

data class BakedTextureWrapModes(
    val s: BakedTextureWrap = BakedTextureWrap.REPEAT,
    val t: BakedTextureWrap = BakedTextureWrap.REPEAT
) {
    companion object {
        fun clampBoth() = BakedTextureWrapModes(
            BakedTextureWrap.CLAMP_TO_EDGE,
            BakedTextureWrap.CLAMP_TO_EDGE
        )

        fun clampS() = BakedTextureWrapModes(s = BakedTextureWrap.CLAMP_TO_EDGE)
    }
}

private val NS = "http://www.collada.org/2005/11/COLLADASchema"

data class DaeVertexKey(
    val xBits: Long,
    val yBits: Long,
    val zBits: Long,
    val uBits: Long,
    val vBits: Long
)

data class DaeTriangleKey(
    val materialIndex: Int,
    val vertex0: Int,
    val vertex1: Int,
    val vertex2: Int
)

data class BakedVertexKey(
    val x: Int,
    val y: Int,
    val z: Int,
    val u: Int,
    val v: Int
)

data class BakedPositionKey(val x: Int, val y: Int, val z: Int)

data class CollisionTriangleKey(val vertex0: Int, val vertex1: Int, val vertex2: Int)

fun parseDaeWorld(
    daeFile: File,
    textureDir: File,
    targetSize: Double = 8192.0,
    placeOnGround: Boolean = false,
    mergeEquivalentMaterials: Boolean = false,
    textureWrapModes: Map<String, BakedTextureWrapModes> = emptyMap()
): BakedMario64World {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    val doc = factory.newDocumentBuilder().parse(daeFile)

    val images = parseDaeImages(doc)
    val effects = parseDaeEffects(doc)
    val materials = parseDaeMaterials(doc)
    val geometryMaterialMap = parseDaeGeometryMaterialMap(doc)
    val materialColors = resolveMaterialColors(materials, effects, images, textureDir)
    val materialTextures = resolveMaterialTextureFiles(materials, effects, images)

    val deduplicatedVertices = mutableListOf<DoubleArray>()
    val vertexMap = linkedMapOf<DaeVertexKey, Int>()
    val colors = mutableListOf<Int>()
    val materialIndexes = linkedMapOf<String, Int>()
    val materialSourceIds = linkedMapOf<String, String>()
    val allTriangles = mutableListOf<IntArray>()
    val triangleKeys = linkedSetOf<DaeTriangleKey>()

    fun materialIndex(matId: String, color: Int): Int {
        val textureFilename = materialTextures[matId]
        val materialKey = if (mergeEquivalentMaterials) {
            textureFilename?.let { "texture:$it" } ?: "color:$color"
        } else {
            matId
        }
        return materialIndexes.getOrPut(materialKey) {
            materialSourceIds[materialKey] = matId
            colors += color
            colors.lastIndex
        }
    }

    fun deduplicateVertex(px: Double, py: Double, pz: Double, u: Double, v: Double): Int {
        fun normalizedBits(value: Double): Long = if (value == 0.0) 0.0.toBits() else value.toBits()
        val key = DaeVertexKey(
            normalizedBits(px),
            normalizedBits(py),
            normalizedBits(pz),
            normalizedBits(u),
            normalizedBits(v)
        )
        val existing = vertexMap[key]
        if (existing != null) return existing
        val idx = deduplicatedVertices.size
        deduplicatedVertices += doubleArrayOf(px, py, pz, u, v)
        vertexMap[key] = idx
        return idx
    }

    fun addVertex(posIndex: Int, uvIndex: Int, positions: DoubleArray, uvs: DoubleArray): Int {
        val px = if (posIndex * 3 + 2 < positions.size) positions[posIndex * 3] else 0.0
        val py = if (posIndex * 3 + 2 < positions.size) positions[posIndex * 3 + 1] else 0.0
        val pz = if (posIndex * 3 + 2 < positions.size) positions[posIndex * 3 + 2] else 0.0
        val u = if (uvs.isNotEmpty() && uvIndex * 2 + 1 < uvs.size) uvs[uvIndex * 2] else 0.0
        val v = if (uvs.isNotEmpty() && uvIndex * 2 + 1 < uvs.size) uvs[uvIndex * 2 + 1] else 0.0
        return deduplicateVertex(px, py, pz, u, v)
    }

    fun addTriangle(a: Int, b: Int, c: Int, materialIndex: Int) {
        if (a == b || b == c || c == a) return

        // Cyclic rotations preserve winding, so reversed two-sided faces remain distinct.
        val key = when {
            a <= b && a <= c -> DaeTriangleKey(materialIndex, a, b, c)
            b <= a && b <= c -> DaeTriangleKey(materialIndex, b, c, a)
            else -> DaeTriangleKey(materialIndex, c, a, b)
        }
        if (triangleKeys.add(key)) {
            allTriangles += intArrayOf(a, b, c, materialIndex)
        }
    }

    fun parsePolylistInputs(parent: Element): Triple<Int, Int, Int> {
        val inputs = parent.getElementsByTagNameNS(NS, "input")
        var stride = 0
        var vertexOffset = 0
        var texcoordOffset = -1
        for (ii in 0 until inputs.length) {
            val input = inputs.item(ii) as? Element ?: continue
            val offset = input.getAttribute("offset")?.toIntOrNull() ?: 0
            if (offset + 1 > stride) stride = offset + 1
            when (input.getAttribute("semantic")) {
                "VERTEX" -> vertexOffset = offset
                "TEXCOORD" -> texcoordOffset = offset
            }
        }
        if (stride == 0) stride = 1
        return Triple(stride, vertexOffset, texcoordOffset)
    }

    fun resolveUVSourceId(parent: Element): String {
        val inputs = parent.getElementsByTagNameNS(NS, "input")
        for (ii in 0 until inputs.length) {
            val input = inputs.item(ii) as? Element ?: continue
            if (input.getAttribute("semantic") == "TEXCOORD") {
                return input.getAttribute("source")?.removePrefix("#") ?: ""
            }
        }
        return ""
    }

    val geometries = doc.getElementsByTagNameNS(NS, "geometry")
    for (gi in 0 until geometries.length) {
        val geom = geometries.item(gi) as? Element ?: continue
        val geomId = geom.getAttribute("id") ?: ""
        val mesh = childElement(geom, "mesh") ?: continue

        val matId = geometryMaterialMap[geomId] ?: ""
        val geomColor = materialColors[matId] ?: rgba(180, 180, 180)
        val ci = materialIndex(matId, geomColor)

        val positionSourceId = resolvePositionSourceId(mesh)
        val positions = parseFloatArray(mesh, positionSourceId)
        if (positions.isEmpty()) continue

        val polylists = mesh.getElementsByTagNameNS(NS, "polylist")
        for (pi in 0 until polylists.length) {
            val polylist = polylists.item(pi) as? Element ?: continue
            val (stride, vertOff, texOff) = parsePolylistInputs(polylist)
            val uvSourceId = resolveUVSourceId(polylist)
            val uvs = if (uvSourceId.isNotEmpty()) parseFloatArray(mesh, uvSourceId) else doubleArrayOf()

            val vcountText = childElement(polylist, "vcount")?.textContent?.trim() ?: continue
            val pText = childElement(polylist, "p")?.textContent?.trim() ?: continue
            val vcounts = vcountText.split(Regex("\\s+")).map { it.toInt() }
            val pValues = pText.split(Regex("\\s+")).map { it.toInt() }

            var pIndex = 0
            for (faceVertexCount in vcounts) {
                if (faceVertexCount < 3) { pIndex += faceVertexCount * stride; continue }
                val faceVerts = mutableListOf<Int>()
                for (fvi in 0 until faceVertexCount) {
                    val posIdx = pValues[pIndex + vertOff]
                    val uvIdx = if (texOff >= 0) pValues[pIndex + texOff] else 0
                    faceVerts += addVertex(posIdx, uvIdx, positions, uvs)
                    pIndex += stride
                }
                for (ti in 1 until faceVerts.size - 1) {
                    val a = faceVerts[0]; val b = faceVerts[ti]; val c = faceVerts[ti + 1]
                    addTriangle(a, b, c, ci)
                }
            }
        }

        val triangleLists = mesh.getElementsByTagNameNS(NS, "triangles")
        for (ti in 0 until triangleLists.length) {
            val triElem = triangleLists.item(ti) as? Element ?: continue
            val (stride, vertOff, texOff) = parsePolylistInputs(triElem)
            val uvSourceId = resolveUVSourceId(triElem)
            val uvs = if (uvSourceId.isNotEmpty()) parseFloatArray(mesh, uvSourceId) else doubleArrayOf()

            val count = triElem.getAttribute("count")?.toIntOrNull() ?: 0
            val pText = childElement(triElem, "p")?.textContent?.trim() ?: continue
            val pValues = pText.split(Regex("\\s+")).map { it.toInt() }

            for (tri in 0 until count) {
                val base = tri * 3 * stride
                val verts = (0 until 3).map { vi ->
                    val posIdx = pValues[base + vi * stride + vertOff]
                    val uvIdx = if (texOff >= 0) pValues[base + vi * stride + texOff] else 0
                    addVertex(posIdx, uvIdx, positions, uvs)
                }
                addTriangle(verts[0], verts[1], verts[2], ci)
            }
        }
    }

    require(allTriangles.isNotEmpty()) { "DAE model has no faces ($daeFile)" }

    val minX = deduplicatedVertices.minOf { it[0] }
    val maxX = deduplicatedVertices.maxOf { it[0] }
    val minY = deduplicatedVertices.minOf { it[1] }
    val maxY = deduplicatedVertices.maxOf { it[1] }
    val minZ = deduplicatedVertices.minOf { it[2] }
    val maxZ = deduplicatedVertices.maxOf { it[2] }
    val centerX = (minX + maxX) / 2.0
    val centerZ = (minZ + maxZ) / 2.0
    val maxExtent = maxOf(maxX - minX, maxY - minY, maxZ - minZ).coerceAtLeast(0.0001)
    val scale = targetSize / maxExtent
    val yOrigin = if (placeOnGround) minY else 0.0

    val bakedVertexRows = deduplicatedVertices.map { v ->
        val u16 = (v[3] * 1024.0).roundToInt()
        val v16 = ((1.0 - v[4]) * 1024.0).roundToInt()
        listOf(
            ((v[0] - centerX) * scale).roundToInt(),
            ((v[1] - yOrigin) * scale).roundToInt(),
            ((v[2] - centerZ) * scale).roundToInt(),
            u16,
            v16
        )
    }

    val compactVertexRows = mutableListOf<List<Int>>()
    val compactVertexMap = linkedMapOf<BakedVertexKey, Int>()
    val oldToCompactVertex = IntArray(bakedVertexRows.size)
    for ((oldIndex, vertex) in bakedVertexRows.withIndex()) {
        val key = BakedVertexKey(vertex[0], vertex[1], vertex[2], vertex[3], vertex[4])
        oldToCompactVertex[oldIndex] = compactVertexMap.getOrPut(key) {
            compactVertexRows += vertex
            compactVertexRows.lastIndex
        }
    }

    val compactTriangles = mutableListOf<IntArray>()
    val compactTriangleKeys = linkedSetOf<DaeTriangleKey>()
    for (triangle in allTriangles) {
        val a = oldToCompactVertex[triangle[0]]
        val b = oldToCompactVertex[triangle[1]]
        val c = oldToCompactVertex[triangle[2]]
        val materialIndex = triangle[3]
        if (a == b || b == c || c == a) continue
        val key = when {
            a <= b && a <= c -> DaeTriangleKey(materialIndex, a, b, c)
            b <= a && b <= c -> DaeTriangleKey(materialIndex, b, c, a)
            else -> DaeTriangleKey(materialIndex, c, a, b)
        }
        if (compactTriangleKeys.add(key)) {
            compactTriangles += intArrayOf(a, b, c, materialIndex)
        }
    }

    val bakedVertices = compactVertexRows.flatten()

    val textures = mutableListOf<BakedMario64Texture>()
    for ((materialKey, matIdx) in materialIndexes) {
        val matId = materialSourceIds[materialKey] ?: materialKey
        val texFilename = materialTextures[matId]
        if (texFilename != null) {
            val texFile = File(textureDir, texFilename)
            if (texFile.exists()) {
                val img = ImageIO.read(texFile)
                if (img != null) {
                    var hasZeroAlpha = false
                    var hasPartialAlpha = false
                    for (py in 0 until img.height) {
                        for (px in 0 until img.width) {
                            val alpha = (img.getRGB(px, py) ushr 24) and 0xFF
                            if (alpha == 0) hasZeroAlpha = true
                            else if (alpha < 255) hasPartialAlpha = true
                        }
                    }
                    val materialMode = when {
                        hasPartialAlpha -> BakedMaterialMode.TRANSLUCENT_DECAL
                        hasZeroAlpha -> BakedMaterialMode.MASKED
                        else -> BakedMaterialMode.OPAQUE
                    }
                    val textureFormat = if (materialMode == BakedMaterialMode.TRANSLUCENT_DECAL) {
                        BakedTextureFormat.IA16
                    } else {
                        BakedTextureFormat.RGBA16
                    }
                    // RGBA16 and IA16 both use two bytes per texel. Keep each
                    // baked texture at or below 32x32 so one upload always fits
                    // comfortably inside the N64's 4 KiB TMEM.
                    val bakedWidth = minOf(img.width, 32)
                    val bakedHeight = minOf(img.height, 32)
                    val texels = ShortArray(bakedWidth * bakedHeight)
                    for (py in 0 until bakedHeight) {
                        for (px in 0 until bakedWidth) {
                            val sourceX = px * img.width / bakedWidth
                            val sourceY = py * img.height / bakedHeight
                            val pixel = img.getRGB(sourceX, sourceY)
                            val red = (pixel ushr 16) and 0xFF
                            val green = (pixel ushr 8) and 0xFF
                            val blue = pixel and 0xFF
                            val alpha = (pixel ushr 24) and 0xFF
                            val texel = if (textureFormat == BakedTextureFormat.IA16) {
                                val intensity = (red * 77 + green * 150 + blue * 29 + 128) shr 8
                                (intensity shl 8) or alpha
                            } else {
                                ((red shr 3) shl 11) or
                                    ((green shr 3) shl 6) or
                                    ((blue shr 3) shl 1) or
                                    if (alpha > 127) 1 else 0
                            }
                            texels[py * bakedWidth + px] = texel.toShort()
                        }
                    }
                    textures += BakedMario64Texture(
                        matIdx,
                        texFilename,
                        bakedWidth,
                        bakedHeight,
                        textureFormat,
                        materialMode,
                        textureWrapModes[texFilename]?.s ?: BakedTextureWrap.REPEAT,
                        textureWrapModes[texFilename]?.t ?: BakedTextureWrap.REPEAT,
                        texels
                    )
                }
            }
        }
    }

    val bakedTriangles = compactTriangles.flatMap { it.toList() }
    val materialModes = MutableList(colors.size) { BakedMaterialMode.OPAQUE.value }
    for (texture in textures) {
        materialModes[texture.materialIndex] = texture.materialMode.value
    }
    val collision = buildCollisionWorld(compactVertexRows, compactTriangles, materialModes)

    return BakedMario64World(
        vertices = bakedVertices,
        triangles = bakedTriangles,
        colors = colors,
        materialModes = materialModes,
        vertexCount = compactVertexRows.size,
        triangleCount = compactTriangles.size,
        hasUVs = true,
        textures = textures,
        collision = collision
    )
}

fun buildCollisionWorld(
    renderVertices: List<List<Int>>,
    renderTriangles: List<IntArray>,
    materialModes: List<Int>
): BakedCollisionWorld {
    val collisionVertices = mutableListOf<List<Int>>()
    val positionIndexes = linkedMapOf<BakedPositionKey, Int>()
    fun collisionVertexIndex(renderIndex: Int): Int {
        val vertex = renderVertices[renderIndex]
        val key = BakedPositionKey(vertex[0], vertex[1], vertex[2])
        return positionIndexes.getOrPut(key) {
            collisionVertices += listOf(key.x, key.y, key.z)
            collisionVertices.lastIndex
        }
    }

    val collisionTriangles = mutableListOf<IntArray>()
    val triangleKeys = linkedSetOf<CollisionTriangleKey>()
    var floorTriangleCount = 0
    for (renderTriangle in renderTriangles) {
        val materialIndex = renderTriangle[3]
        if (materialIndex in materialModes.indices &&
            materialModes[materialIndex] == BakedMaterialMode.TRANSLUCENT_DECAL.value
        ) {
            continue
        }
        val a = collisionVertexIndex(renderTriangle[0])
        val b = collisionVertexIndex(renderTriangle[1])
        val c = collisionVertexIndex(renderTriangle[2])
        if (a == b || b == c || c == a) continue

        val key = when {
            a <= b && a <= c -> CollisionTriangleKey(a, b, c)
            b <= a && b <= c -> CollisionTriangleKey(b, c, a)
            else -> CollisionTriangleKey(c, a, b)
        }
        if (!triangleKeys.add(key)) continue

        val av = collisionVertices[a]
        val bv = collisionVertices[b]
        val cv = collisionVertices[c]
        val abx = (bv[0] - av[0]).toLong()
        val aby = (bv[1] - av[1]).toLong()
        val abz = (bv[2] - av[2]).toLong()
        val acx = (cv[0] - av[0]).toLong()
        val acy = (cv[1] - av[1]).toLong()
        val acz = (cv[2] - av[2]).toLong()
        val normalX = aby * acz - abz * acy
        val normalY = abz * acx - abx * acz
        val normalZ = abx * acy - aby * acx
        val normalLength = sqrt(
            normalX.toDouble() * normalX.toDouble() +
                normalY.toDouble() * normalY.toDouble() +
                normalZ.toDouble() * normalZ.toDouble()
        )
        if (normalLength == 0.0) continue

        val isFloor = abs(normalY.toDouble()) / normalLength >= 0.55
        val flags = if (isFloor) 1 else 0
        val unitNormalX = (normalX.toDouble() / normalLength * 1024.0).roundToInt()
        val unitNormalY = (normalY.toDouble() / normalLength * 1024.0).roundToInt()
        val unitNormalZ = (normalZ.toDouble() / normalLength * 1024.0).roundToInt()
        val originOffset = -(
            unitNormalX * av[0] +
                unitNormalY * av[1] +
                unitNormalZ * av[2]
            )
        if (isFloor) floorTriangleCount += 1
        collisionTriangles += intArrayOf(
            a,
            b,
            c,
            flags,
            unitNormalX,
            unitNormalY,
            unitNormalZ,
            originOffset
        )
    }

    val minX = collisionVertices.minOf { it[0] }
    val minY = collisionVertices.minOf { it[1] }
    val minZ = collisionVertices.minOf { it[2] }
    val maxX = collisionVertices.maxOf { it[0] }
    val maxY = collisionVertices.maxOf { it[1] }
    val maxZ = collisionVertices.maxOf { it[2] }
    val gridWidth = 16
    val gridDepth = 16
    val cellSize = 512
    val cellTriangles = Array(gridWidth * gridDepth) { mutableListOf<Int>() }

    fun gridCoordinate(value: Int, minimum: Int, size: Int): Int {
        val coordinate = (value - minimum) / cellSize
        return coordinate.coerceIn(0, size - 1)
    }

    for ((triangleIndex, triangle) in collisionTriangles.withIndex()) {
        val av = collisionVertices[triangle[0]]
        val bv = collisionVertices[triangle[1]]
        val cv = collisionVertices[triangle[2]]
        val triangleMinX = minOf(av[0], bv[0], cv[0])
        val triangleMaxX = maxOf(av[0], bv[0], cv[0])
        val triangleMinZ = minOf(av[2], bv[2], cv[2])
        val triangleMaxZ = maxOf(av[2], bv[2], cv[2])
        val minCellX = gridCoordinate(triangleMinX, minX, gridWidth)
        val maxCellX = gridCoordinate(triangleMaxX, minX, gridWidth)
        val minCellZ = gridCoordinate(triangleMinZ, minZ, gridDepth)
        val maxCellZ = gridCoordinate(triangleMaxZ, minZ, gridDepth)
        for (cellZ in minCellZ..maxCellZ) {
            for (cellX in minCellX..maxCellX) {
                cellTriangles[cellZ * gridWidth + cellX] += triangleIndex
            }
        }
    }

    val cellOffsets = MutableList(cellTriangles.size + 1) { 0 }
    val cellTriangleIndices = mutableListOf<Int>()
    for ((cellIndex, references) in cellTriangles.withIndex()) {
        cellOffsets[cellIndex] = cellTriangleIndices.size
        cellTriangleIndices += references
    }
    cellOffsets[cellTriangles.size] = cellTriangleIndices.size

    return BakedCollisionWorld(
        vertices = collisionVertices.flatten(),
        triangles = collisionTriangles.flatMap { it.toList() },
        cellOffsets = cellOffsets,
        cellTriangleIndices = cellTriangleIndices,
        vertexCount = collisionVertices.size,
        triangleCount = collisionTriangles.size,
        floorTriangleCount = floorTriangleCount,
        minX = minX,
        minY = minY,
        minZ = minZ,
        maxX = maxX,
        maxY = maxY,
        maxZ = maxZ,
        gridWidth = gridWidth,
        gridDepth = gridDepth,
        cellSize = cellSize
    )
}

fun parseDaeImages(doc: Document): Map<String, String> {
    val images = linkedMapOf<String, String>()
    val nodes = doc.getElementsByTagNameNS(NS, "image")
    for (i in 0 until nodes.length) {
        val elem = nodes.item(i) as? Element ?: continue
        val id = elem.getAttribute("id") ?: continue
        val initFrom = childElement(elem, "init_from")?.textContent?.trim() ?: continue
        images[id] = initFrom
    }
    return images
}

fun parseDaeEffects(doc: Document): Map<String, String> {
    val effects = linkedMapOf<String, String>()
    val nodes = doc.getElementsByTagNameNS(NS, "effect")
    for (i in 0 until nodes.length) {
        val elem = nodes.item(i) as? Element ?: continue
        val effectId = elem.getAttribute("id") ?: continue
        val initFroms = elem.getElementsByTagNameNS(NS, "init_from")
        if (initFroms.length > 0) {
            val imageRef = (initFroms.item(0) as? Element)?.textContent?.trim() ?: continue
            effects[effectId] = imageRef
        }
    }
    return effects
}

fun parseDaeMaterials(doc: Document): Map<String, String> {
    val materials = linkedMapOf<String, String>()
    val nodes = doc.getElementsByTagNameNS(NS, "material")
    for (i in 0 until nodes.length) {
        val elem = nodes.item(i) as? Element ?: continue
        val matId = elem.getAttribute("id") ?: continue
        val instEffect = childElement(elem, "instance_effect") ?: continue
        val url = instEffect.getAttribute("url") ?: continue
        val effectId = url.removePrefix("#")
        materials[matId] = effectId
    }
    return materials
}

fun resolveMaterialColors(
    materials: Map<String, String>,
    effects: Map<String, String>,
    images: Map<String, String>,
    textureDir: File
): Map<String, Int> {
    val result = linkedMapOf<String, Int>()
    for ((matId, effectId) in materials) {
        val imageId = effects[effectId]
        if (imageId == null) {
            result[matId] = rgba(160, 160, 160)
            continue
        }
        val filename = images[imageId]
        if (filename == null) {
            result[matId] = rgba(160, 160, 160)
            continue
        }
        val imageFile = File(textureDir, filename)
        if (!imageFile.exists()) {
            result[matId] = rgba(160, 160, 160)
            continue
        }
        result[matId] = averageImageColor(imageFile)
    }
    return result
}

fun resolveMaterialTextureFiles(
    materials: Map<String, String>,
    effects: Map<String, String>,
    images: Map<String, String>
): Map<String, String> {
    val result = linkedMapOf<String, String>()
    for ((matId, effectId) in materials) {
        val imageId = effects[effectId] ?: continue
        val filename = images[imageId] ?: continue
        result[matId] = filename
    }
    return result
}

fun averageImageColor(file: File): Int {
    val image = ImageIO.read(file) ?: return rgba(160, 160, 160)
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val pixel = image.getRGB(x, y)
            val alpha = (pixel ushr 24) and 0xff
            if (alpha < 32) continue
            rSum += (pixel ushr 16) and 0xff
            gSum += (pixel ushr 8) and 0xff
            bSum += pixel and 0xff
            count += 1
        }
    }
    if (count == 0) return rgba(160, 160, 160)
    return rgba((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
}

fun resolvePositionSourceId(mesh: Element): String {
    val vertices = childElement(mesh, "vertices") ?: return ""
    val children = vertices.childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child is Element && child.localName == "input" && child.getAttribute("semantic") == "POSITION") {
            return child.getAttribute("source")?.removePrefix("#") ?: ""
        }
    }
    return ""
}

fun parseFloatArray(mesh: Element, sourceId: String): DoubleArray {
    if (sourceId.isEmpty()) return doubleArrayOf()
    val children = mesh.childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child is Element && child.localName == "source" && child.getAttribute("id") == sourceId) {
            val floatArray = childElement(child, "float_array") ?: return doubleArrayOf()
            val text = floatArray.textContent?.trim() ?: return doubleArrayOf()
            return text.split(Regex("\\s+")).map { it.toDouble() }.toDoubleArray()
        }
    }
    return doubleArrayOf()
}

fun parseDaeGeometryMaterialMap(doc: Document): Map<String, String> {
    val map = linkedMapOf<String, String>()
    val nodes = doc.getElementsByTagNameNS(NS, "instance_geometry")
    for (i in 0 until nodes.length) {
        val ig = nodes.item(i) as? Element ?: continue
        val geomId = (ig.getAttribute("url") ?: "").removePrefix("#")
        if (geomId.isEmpty()) continue
        val bindMaterials = ig.getElementsByTagNameNS(NS, "instance_material")
        if (bindMaterials.length > 0) {
            val bind = bindMaterials.item(0) as? Element ?: continue
            val target = (bind.getAttribute("target") ?: "").removePrefix("#")
            if (target.isNotEmpty()) {
                map[geomId] = target
            }
        }
    }
    return map
}

fun childElement(parent: Element, localName: String): Element? {
    val children = parent.childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child is Element && child.localName == localName) return child
    }
    return null
}

// ---------------------------------------------------------------------------
// Code generation
// ---------------------------------------------------------------------------

fun renderMario64ModelAssets(
    battlefield: BakedMario64World,
    mario: BakedMario64World,
    marioPoseCount: Int,
    staticMario: BakedMario64World
): String {
    return buildString {
        appendLine("package mario64")
        appendLine()
        appendLine("// Generated by :games:mario-n64:generateMario64ModelAssets")
        appendLine("// Source: Bob-Omb Battlefield (Area1.dae)")
        appendLine("// Render geometry lives only in the generated C world-mesh header. Keeping")
        appendLine("// a second Kotlin copy made the N64 linker retain hundreds of kilobytes of")
        appendLine("// unreachable software-renderer data. These constants preserve cheap JVM")
        appendLine("// validation without adding runtime arrays to the ROM.")
        appendLine("internal object Mario64ModelAssets {")
        appendLine("    const val BATTLEFIELD_NAME = \"Bob-Omb Battlefield\"")
        appendLine("    const val BATTLEFIELD_VERTEX_COUNT = ${battlefield.vertexCount}")
        appendLine("    const val BATTLEFIELD_TRIANGLE_COUNT = ${battlefield.triangleCount}")
        appendLine("    const val BATTLEFIELD_MATERIAL_COUNT = ${battlefield.colors.size}")
        appendLine("    const val BATTLEFIELD_TEXTURE_COUNT = ${battlefield.textures.size}")
        appendLine("    const val BATTLEFIELD_OPAQUE_MATERIAL_COUNT = ${battlefield.materialModes.count { it == BakedMaterialMode.OPAQUE.value }}")
        appendLine("    const val BATTLEFIELD_MASKED_MATERIAL_COUNT = ${battlefield.materialModes.count { it == BakedMaterialMode.MASKED.value }}")
        appendLine("    const val BATTLEFIELD_TRANSLUCENT_DECAL_MATERIAL_COUNT = ${battlefield.materialModes.count { it == BakedMaterialMode.TRANSLUCENT_DECAL.value }}")
        appendLine("    const val MARIO_VERTEX_COUNT = ${mario.vertexCount}")
        appendLine("    const val MARIO_TRIANGLE_COUNT = ${mario.triangleCount}")
        appendLine("    const val MARIO_MATERIAL_COUNT = ${mario.colors.size}")
        appendLine("    const val MARIO_TEXTURE_COUNT = ${mario.textures.size}")
        appendLine("    const val MARIO_POSE_COUNT = $marioPoseCount")
        appendLine("    const val MARIO_STATIC_CLAMPED_TEXTURE_COUNT = ${staticMario.textures.count { it.wrapS == BakedTextureWrap.CLAMP_TO_EDGE || it.wrapT == BakedTextureWrap.CLAMP_TO_EDGE }}")
        appendLine("}")
    }
}

fun renderMario64CollisionAssets(collision: BakedCollisionWorld): String {
    return buildString {
        appendLine("package mario64")
        appendLine()
        appendLine("// Generated by :games:mario-n64:generateMario64ModelAssets")
        appendLine("// Source: Bob-Omb Battlefield (Area1.dae)")
        appendLine("// ${collision.vertexCount} vertices, ${collision.triangleCount} triangles (${collision.floorTriangleCount} walkable), ${collision.cellTriangleIndices.size} grid references")
        appendLine("object Mario64CollisionAssets {")
        appendLine("    val battlefield = StaticTriangleGrid3D(")
        appendLine("        vertices = battlefieldCollisionVertices(),")
        appendLine("        triangles = battlefieldCollisionTriangles(),")
        appendLine("        cellOffsets = battlefieldCollisionCellOffsets(),")
        appendLine("        cellTriangleIndices = battlefieldCollisionCellTriangleIndices(),")
        appendLine("        metadata = intArrayOf(")
        appendLine("            ${collision.minX}, ${collision.minY}, ${collision.minZ},")
        appendLine("            ${collision.maxX}, ${collision.maxY}, ${collision.maxZ},")
        appendLine("            ${collision.gridWidth}, ${collision.gridDepth}, ${collision.cellSize}")
        appendLine("        )")
        appendLine("    )")
        appendLine()
        appendChunkedIntArrayBuilder("battlefieldCollisionVertices", collision.vertices, "    ")
        appendLine()
        appendChunkedIntArrayBuilder("battlefieldCollisionTriangles", collision.triangles, "    ")
        appendLine()
        appendChunkedIntArrayBuilder("battlefieldCollisionCellOffsets", collision.cellOffsets, "    ")
        appendLine()
        appendChunkedIntArrayBuilder(
            "battlefieldCollisionCellTriangleIndices",
            collision.cellTriangleIndices,
            "    "
        )
        appendLine("}")
    }
}

fun StringBuilder.appendChunkedIntArrayBuilder(
    name: String,
    values: List<Int>,
    indent: String,
    chunkSize: Int = 256
) {
    val chunks = values.chunked(chunkSize)
    appendLine("${indent}private fun $name(): IntArray {")
    appendLine("${indent}    val values = IntArray(${values.size})")
    chunks.indices.forEach { chunkIndex ->
        appendLine("${indent}    ${name}Chunk$chunkIndex(values)")
    }
    appendLine("${indent}    return values")
    appendLine("$indent}")

    chunks.forEachIndexed { chunkIndex, chunk ->
        appendLine()
        appendLine("${indent}private fun ${name}Chunk$chunkIndex(values: IntArray) {")
        val start = chunkIndex * chunkSize
        chunk.forEachIndexed { offset, value ->
            appendLine("${indent}    values[${start + offset}] = $value")
        }
        appendLine("$indent}")
    }
}

fun renderMario64MeshC(
    battlefield: BakedMario64World,
    marioPoses: List<Pair<String, BakedMario64World>>
): String {
    require(battlefield.colors.size <= 32) { "Battlefield exceeds the renderer's 32-material limit" }
    require(marioPoses.isNotEmpty()) { "Mario must have at least one baked pose" }
    require(marioPoses.map { it.first }.distinct().size == marioPoses.size) { "Mario pose names must be unique" }
    val marioMaterialSource = marioPoses.first()
    marioPoses.forEach { (name, mario) ->
        require(mario.colors.size <= 32) { "$name exceeds the renderer's 32-material limit" }
        require(haveEquivalentMaterials(marioMaterialSource.second, mario)) {
            "$name must share the ${marioMaterialSource.first} material and texture layout"
        }
    }
    val battlefieldMeshId = stableAssetId("mesh:battlefield")
    return buildString {
        appendLine("// Generated by :games:mario-n64:generateMario64ModelAssets")
        appendLine("// Sources: Bob-Omb Battlefield (Area1.dae), baked Mario64Animated poses")
        appendLine("#ifndef KENGINE_N64_WORLD_MESH_H")
        appendLine("#define KENGINE_N64_WORLD_MESH_H")
        appendLine()
        appendLine("#include \"kengine_n64_world_mesh_types.h\"")
        appendLine()
        appendWorldMeshC("battlefield", "BATTLEFIELD", battlefieldMeshId, battlefield)
        marioPoses.forEachIndexed { index, (assetName, mario) ->
            val symbol = assetName.replace('-', '_')
            val macro = assetName.replace('-', '_').uppercase()
            appendWorldMeshC(
                symbol,
                macro,
                stableAssetId("mesh:$assetName"),
                mario,
                sharedMaterialSymbol = if (index == 0) null else marioMaterialSource.first.replace('-', '_')
            )
        }

        appendLine("static const KengineWorldMesh kengine_world_meshes[] = {")
        appendWorldMeshCInitializer("battlefield", "BATTLEFIELD", battlefieldMeshId)
        marioPoses.forEachIndexed { index, (assetName, _) ->
            val symbol = assetName.replace('-', '_')
            val macro = assetName.replace('-', '_').uppercase()
            appendWorldMeshCInitializer(
                symbol,
                macro,
                stableAssetId("mesh:$assetName"),
                materialSymbol = if (index == 0) symbol else marioMaterialSource.first.replace('-', '_')
            )
        }
        appendLine("};")
        appendLine("#define KENGINE_WORLD_MESH_COUNT ${marioPoses.size + 1}")
        appendLine()
        appendLine("static const KengineWorldMesh* kengine_find_world_mesh(int mesh_id) {")
        appendLine("    for (int i = 0; i < KENGINE_WORLD_MESH_COUNT; i++) {")
        appendLine("        if (kengine_world_meshes[i].mesh_id == mesh_id) return &kengine_world_meshes[i];")
        appendLine("    }")
        appendLine("    return 0;")
        appendLine("}")
        appendLine()
        appendLine("#endif")
    }
}

fun StringBuilder.appendWorldMeshC(
    symbol: String,
    macro: String,
    meshId: Int,
    model: BakedMario64World,
    sharedMaterialSymbol: String? = null
) {
    appendLine("#define KENGINE_WORLD_MESH_${macro}_ID $meshId")
    appendLine("#define KENGINE_WORLD_MESH_${macro}_VERTEX_COUNT ${model.vertexCount}")
    appendLine("#define KENGINE_WORLD_MESH_${macro}_TRIANGLE_COUNT ${model.triangleCount}")
    appendLine("#define KENGINE_WORLD_MESH_${macro}_COLOR_COUNT ${model.colors.size}")
    appendLine("#define KENGINE_WORLD_MESH_${macro}_VERTEX_STRIDE 5")
    appendLine("#define KENGINE_WORLD_MESH_${macro}_TEXTURE_COUNT ${model.textures.size}")
    appendLine()
    appendCIntArray("kengine_world_mesh_${symbol}_vertices", model.vertices)
    appendLine()
    appendCIntArray("kengine_world_mesh_${symbol}_triangles", model.triangles)
    appendLine()
    if (sharedMaterialSymbol != null) {
        appendLine("// Material colors and textures are shared with $sharedMaterialSymbol.")
        appendLine()
        return
    }
    appendCIntArray("kengine_world_mesh_${symbol}_colors", model.colors)
    appendLine()

    for ((index, texture) in model.textures.withIndex()) {
        appendLine("// Texture $index: ${texture.filename} (${texture.width}x${texture.height})")
        append("static const uint16_t kengine_world_mesh_${symbol}_tex_${index}[] = {\n")
        texture.texelData.toList().chunked(16).forEach { chunk ->
            appendLine(
                "    ${chunk.joinToString(", ") { "0x${(it.toInt() and 0xFFFF).toString(16).padStart(4, '0')}" }},"
            )
        }
        appendLine("};")
        appendLine()
    }

    if (model.textures.isNotEmpty()) {
        appendLine("static const KengineWorldTexture kengine_world_mesh_${symbol}_textures[] = {")
        for ((index, texture) in model.textures.withIndex()) {
            appendLine(
                "    { ${texture.materialIndex}, ${texture.width}, ${texture.height}, " +
                    "${texture.format.cName}, ${texture.materialMode.cName}, " +
                    "${texture.wrapS.cName}, ${texture.wrapT.cName}, " +
                    "kengine_world_mesh_${symbol}_tex_$index },"
            )
        }
        appendLine("};")
    } else {
        appendLine("static const KengineWorldTexture* kengine_world_mesh_${symbol}_textures = 0;")
    }
    appendLine()
}

fun StringBuilder.appendWorldMeshCInitializer(
    symbol: String,
    macro: String,
    meshId: Int,
    materialSymbol: String = symbol
) {
    appendLine("    {")
    appendLine("        $meshId,")
    appendLine("        KENGINE_WORLD_MESH_${macro}_VERTEX_COUNT,")
    appendLine("        KENGINE_WORLD_MESH_${macro}_TRIANGLE_COUNT,")
    appendLine("        KENGINE_WORLD_MESH_${macro}_COLOR_COUNT,")
    appendLine("        KENGINE_WORLD_MESH_${macro}_VERTEX_STRIDE,")
    appendLine("        KENGINE_WORLD_MESH_${macro}_TEXTURE_COUNT,")
    appendLine("        kengine_world_mesh_${symbol}_vertices,")
    appendLine("        kengine_world_mesh_${symbol}_triangles,")
    appendLine("        kengine_world_mesh_${materialSymbol}_colors,")
    appendLine("        kengine_world_mesh_${materialSymbol}_textures")
    appendLine("    },")
}

fun haveEquivalentMaterials(left: BakedMario64World, right: BakedMario64World): Boolean {
    if (left.colors != right.colors || left.materialModes != right.materialModes) return false
    if (left.textures.size != right.textures.size) return false
    return left.textures.indices.all { index ->
        val a = left.textures[index]
        val b = right.textures[index]
        a.materialIndex == b.materialIndex &&
            a.filename == b.filename &&
            a.width == b.width &&
            a.height == b.height &&
            a.format == b.format &&
            a.materialMode == b.materialMode &&
            a.wrapS == b.wrapS &&
            a.wrapT == b.wrapT &&
            a.texelData.contentEquals(b.texelData)
    }
}

fun StringBuilder.appendCIntArray(name: String, values: List<Int>) {
    appendLine("static const int ${name}[] = {")
    values.chunked(12).forEach { chunk ->
        appendLine("    ${chunk.joinToString(", ")},")
    }
    appendLine("};")
}

fun stableAssetId(value: String): Int {
    var hash = -0x7ee3623b
    for (char in value) {
        hash = hash xor char.code
        hash *= 0x01000193
    }
    return if (hash == 0) 1 else hash
}

fun writeTextIfChanged(file: File, text: String) {
    if (!file.exists() || file.readText() != text) {
        file.writeText(text)
    }
}

fun rgba(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
    fun clamp(v: Int) = v.coerceIn(0, 255)
    return clamp(red) or (clamp(green) shl 8) or (clamp(blue) shl 16) or (clamp(alpha) shl 24)
}
