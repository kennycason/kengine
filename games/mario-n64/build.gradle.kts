import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import kotlin.math.roundToInt

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.n64-game")
}

group = "kengine.n64.mario"
version = "1.0.0"

val daeFile = file("assets/models/bob-omb-battlefield/Area1.dae")
val textureDir = file("assets/models/bob-omb-battlefield")

val generateMario64ModelAssets by tasks.registering {
    group = "n64"
    description = "Parses Bob-Omb Battlefield COLLADA DAE and bakes into Kotlin + C mesh data."

    inputs.file(daeFile)
    inputs.dir(textureDir)
    val kotlinOutputFile = layout.projectDirectory.file("src/commonMain/kotlin/mario64/Mario64ModelAssets.kt")
    val cOutputFile = layout.projectDirectory.file("src/main/c/kengine_n64_world_mesh.h")
    outputs.file(kotlinOutputFile)
    outputs.file(cOutputFile)

    doLast {
        val model = parseDaeWorld(daeFile, textureDir)

        val kotlinOut = kotlinOutputFile.asFile
        kotlinOut.parentFile.mkdirs()
        writeTextIfChanged(kotlinOut, renderMario64ModelAssets(model))

        val cOut = cOutputFile.asFile
        cOut.parentFile.mkdirs()
        writeTextIfChanged(cOut, renderMario64MeshC(model))

        println("Mario64 world: ${model.vertexCount} vertices, ${model.triangleCount} triangles, ${model.colors.size} materials")
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
    val vertexCount: Int,
    val triangleCount: Int
)

private val NS = "http://www.collada.org/2005/11/COLLADASchema"

fun parseDaeWorld(daeFile: File, textureDir: File): BakedMario64World {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    val doc = factory.newDocumentBuilder().parse(daeFile)

    val images = parseDaeImages(doc)
    val effects = parseDaeEffects(doc)
    val materials = parseDaeMaterials(doc)
    val geometryMaterialMap = parseDaeGeometryMaterialMap(doc)

    val materialColors = resolveMaterialColors(materials, effects, images, textureDir)

    val allVertices = mutableListOf<DoubleArray>()
    val colors = mutableListOf<Int>()
    val colorIndexes = linkedMapOf<Int, Int>()
    val allTriangles = mutableListOf<IntArray>()

    fun colorIndex(color: Int): Int {
        return colorIndexes.getOrPut(color) {
            colors += color
            colors.lastIndex
        }
    }

    val geometries = doc.getElementsByTagNameNS(NS, "geometry")
    for (gi in 0 until geometries.length) {
        val geom = geometries.item(gi) as? Element ?: continue
        val geomId = geom.getAttribute("id") ?: ""
        val mesh = childElement(geom, "mesh") ?: continue

        val matId = geometryMaterialMap[geomId] ?: ""
        val geomColor = materialColors[matId] ?: rgba(180, 180, 180)

        val positionSourceId = resolvePositionSourceId(mesh)
        val positionFloats = parseFloatArray(mesh, positionSourceId)
        if (positionFloats.isEmpty()) continue

        val vertexOffset = allVertices.size
        for (vi in positionFloats.indices step 3) {
            allVertices += doubleArrayOf(
                positionFloats[vi],
                positionFloats[vi + 1],
                positionFloats[vi + 2]
            )
        }

        val polylists = mesh.getElementsByTagNameNS(NS, "polylist")
        for (pi in 0 until polylists.length) {
            val polylist = polylists.item(pi) as? Element ?: continue
            val ci = colorIndex(geomColor)

            val inputs = polylist.getElementsByTagNameNS(NS, "input")
            var stride = 0
            var vertexInputOffset = 0
            for (ii in 0 until inputs.length) {
                val input = inputs.item(ii) as? Element ?: continue
                val offset = input.getAttribute("offset")?.toIntOrNull() ?: 0
                if (offset + 1 > stride) stride = offset + 1
                if (input.getAttribute("semantic") == "VERTEX") {
                    vertexInputOffset = offset
                }
            }
            if (stride == 0) stride = 1

            val vcountText = childElement(polylist, "vcount")?.textContent?.trim() ?: continue
            val pText = childElement(polylist, "p")?.textContent?.trim() ?: continue
            val vcounts = vcountText.split(Regex("\\s+")).map { it.toInt() }
            val pValues = pText.split(Regex("\\s+")).map { it.toInt() }

            var pIndex = 0
            for (faceVertexCount in vcounts) {
                if (faceVertexCount < 3) {
                    pIndex += faceVertexCount * stride
                    continue
                }
                val faceIndices = mutableListOf<Int>()
                for (fvi in 0 until faceVertexCount) {
                    val globalIndex = pValues[pIndex + vertexInputOffset] + vertexOffset
                    faceIndices += globalIndex
                    pIndex += stride
                }
                for (ti in 1 until faceIndices.size - 1) {
                    val a = faceIndices[0]
                    val b = faceIndices[ti]
                    val c = faceIndices[ti + 1]
                    if (a != b && b != c && c != a) {
                        allTriangles += intArrayOf(a, b, c, ci)
                    }
                }
            }
        }

        val triangleLists = mesh.getElementsByTagNameNS(NS, "triangles")
        for (ti in 0 until triangleLists.length) {
            val trianglesElem = triangleLists.item(ti) as? Element ?: continue
            val ci = colorIndex(geomColor)

            val inputs = trianglesElem.getElementsByTagNameNS(NS, "input")
            var stride = 0
            var vertexInputOffset = 0
            for (ii in 0 until inputs.length) {
                val input = inputs.item(ii) as? Element ?: continue
                val offset = input.getAttribute("offset")?.toIntOrNull() ?: 0
                if (offset + 1 > stride) stride = offset + 1
                if (input.getAttribute("semantic") == "VERTEX") {
                    vertexInputOffset = offset
                }
            }
            if (stride == 0) stride = 1

            val count = trianglesElem.getAttribute("count")?.toIntOrNull() ?: 0
            val pText = childElement(trianglesElem, "p")?.textContent?.trim() ?: continue
            val pValues = pText.split(Regex("\\s+")).map { it.toInt() }

            for (tri in 0 until count) {
                val base = tri * 3 * stride
                val a = pValues[base + vertexInputOffset] + vertexOffset
                val b = pValues[base + stride + vertexInputOffset] + vertexOffset
                val c = pValues[base + 2 * stride + vertexInputOffset] + vertexOffset
                if (a != b && b != c && c != a) {
                    allTriangles += intArrayOf(a, b, c, ci)
                }
            }
        }
    }

    require(allTriangles.isNotEmpty()) { "DAE model has no faces ($daeFile): parsed ${geometries.length} geometries, ${allVertices.size} vertices" }

    val minX = allVertices.minOf { it[0] }
    val maxX = allVertices.maxOf { it[0] }
    val minY = allVertices.minOf { it[1] }
    val maxY = allVertices.maxOf { it[1] }
    val minZ = allVertices.minOf { it[2] }
    val maxZ = allVertices.maxOf { it[2] }
    val centerX = (minX + maxX) / 2.0
    val centerZ = (minZ + maxZ) / 2.0
    val maxExtent = maxOf(maxX - minX, maxY - minY, maxZ - minZ).coerceAtLeast(0.0001)
    val worldSize = 8192.0
    val scale = worldSize / maxExtent

    val bakedVertices = allVertices.flatMap { v ->
        listOf(
            ((v[0] - centerX) * scale).roundToInt(),
            (v[1] * scale).roundToInt(),
            ((v[2] - centerZ) * scale).roundToInt()
        )
    }

    val bakedTriangles = allTriangles.flatMap { it.toList() }

    return BakedMario64World(
        vertices = bakedVertices,
        triangles = bakedTriangles,
        colors = colors,
        vertexCount = allVertices.size,
        triangleCount = allTriangles.size
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

fun renderMario64ModelAssets(model: BakedMario64World): String {
    return buildString {
        appendLine("package mario64")
        appendLine()
        appendLine("// Generated by :games:mario-n64:generateMario64ModelAssets")
        appendLine("// Source: Bob-Omb Battlefield (Area1.dae)")
        appendLine("object Mario64ModelAssets {")
        appendLine("    val battlefield = Mario64BakedWorld(")
        appendLine("        name = \"Bob-Omb Battlefield\",")
        appendIntArray("vertices", model.vertices, "        ")
        appendLine(",")
        appendIntArray("triangles", model.triangles, "        ")
        appendLine(",")
        appendIntArray("colors", model.colors, "        ")
        appendLine()
        appendLine("    )")
        appendLine("}")
    }
}

fun StringBuilder.appendIntArray(name: String, values: List<Int>, indent: String) {
    appendLine("${indent}$name = intArrayOf(")
    values.chunked(12).forEachIndexed { index, chunk ->
        val suffix = if (index == (values.size - 1) / 12) "" else ","
        appendLine("$indent    ${chunk.joinToString(", ")}$suffix")
    }
    append("$indent)")
}

fun renderMario64MeshC(model: BakedMario64World): String {
    val meshId = stableAssetId("mesh:battlefield")
    return buildString {
        appendLine("// Generated by :games:mario-n64:generateMario64ModelAssets")
        appendLine("// Source: Bob-Omb Battlefield (Area1.dae)")
        appendLine("#ifndef KENGINE_N64_WORLD_MESH_H")
        appendLine("#define KENGINE_N64_WORLD_MESH_H")
        appendLine()
        appendLine("#define KENGINE_WORLD_MESH_BATTLEFIELD_ID $meshId")
        appendLine("#define KENGINE_WORLD_MESH_BATTLEFIELD_VERTEX_COUNT ${model.vertexCount}")
        appendLine("#define KENGINE_WORLD_MESH_BATTLEFIELD_TRIANGLE_COUNT ${model.triangleCount}")
        appendLine("#define KENGINE_WORLD_MESH_BATTLEFIELD_COLOR_COUNT ${model.colors.size}")
        appendLine()
        appendCIntArray("kengine_world_mesh_battlefield_vertices", model.vertices)
        appendLine()
        appendCIntArray("kengine_world_mesh_battlefield_triangles", model.triangles)
        appendLine()
        appendCIntArray("kengine_world_mesh_battlefield_colors", model.colors)
        appendLine()
        appendLine("typedef struct {")
        appendLine("    int mesh_id;")
        appendLine("    int vertex_count;")
        appendLine("    int triangle_count;")
        appendLine("    int color_count;")
        appendLine("    const int* vertices;")
        appendLine("    const int* triangles;")
        appendLine("    const int* colors;")
        appendLine("} KengineWorldMesh;")
        appendLine()
        appendLine("static const KengineWorldMesh kengine_world_meshes[] = {")
        appendLine("    {")
        appendLine("        $meshId,")
        appendLine("        KENGINE_WORLD_MESH_BATTLEFIELD_VERTEX_COUNT,")
        appendLine("        KENGINE_WORLD_MESH_BATTLEFIELD_TRIANGLE_COUNT,")
        appendLine("        KENGINE_WORLD_MESH_BATTLEFIELD_COLOR_COUNT,")
        appendLine("        kengine_world_mesh_battlefield_vertices,")
        appendLine("        kengine_world_mesh_battlefield_triangles,")
        appendLine("        kengine_world_mesh_battlefield_colors")
        appendLine("    }")
        appendLine("};")
        appendLine("#define KENGINE_WORLD_MESH_COUNT 1")
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
