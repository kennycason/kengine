import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sqrt

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.n64-game")
}

group = "kengine.n64.demo"
version = "1.0.0"

data class N64DemoObjModelSpec(
    val propertyName: String,
    val displayName: String,
    val objFile: File,
    val mtlFile: File
)

data class N64DemoObjEdge(
    val start: Int,
    val end: Int,
    val colorIndex: Int,
    val firstTriangle: Int,
    val secondTriangle: Int,
    val lengthSquared: Int,
    val kind: N64DemoObjEdgeKind,
    val sharpnessScore: Int
)

enum class N64DemoObjEdgeKind(val priority: Int) {
    BOUNDARY(0),
    MATERIAL_SEAM(1),
    STRONG_CREASE(2),
    FALLBACK(3)
}

data class N64DemoObjTriangle(
    val a: Int,
    val b: Int,
    val c: Int,
    val colorIndex: Int
)

data class N64DemoObjTriangleNormal(
    val triangleIndex: Int,
    val triangle: N64DemoObjTriangle,
    val nx: Long,
    val ny: Long,
    val nz: Long
)

val n64DemoModelSpecs = listOf(
    N64DemoObjModelSpec(
        propertyName = "craftRacer",
        displayName = "Craft Racer",
        objFile = file("assets/models/kenney-space-kit/craft_racer.obj"),
        mtlFile = file("assets/models/kenney-space-kit/craft_racer.mtl")
    ),
    N64DemoObjModelSpec(
        propertyName = "meteorDetailed",
        displayName = "Meteor Detailed",
        objFile = file("assets/models/kenney-space-kit/meteor_detailed.obj"),
        mtlFile = file("assets/models/kenney-space-kit/meteor_detailed.mtl")
    )
)

val n64StrongCreaseCosine = 0.72

val n64DemoAssetPreflightSpecs = listOf(
    N64DemoAssetPreflightSpec(
        displayName = "N64 Demo / Craft Racer",
        assetFile = file("assets/models/kenney-space-kit/craft_racer.obj"),
        sourceNote = "Kenney Space Kit, CC0",
        includeInDefaultRom = true
    ),
    N64DemoAssetPreflightSpec(
        displayName = "N64 Demo / Meteor Detailed",
        assetFile = file("assets/models/kenney-space-kit/meteor_detailed.obj"),
        sourceNote = "Kenney Space Kit, CC0",
        includeInDefaultRom = true
    ),
    N64DemoAssetPreflightSpec(
        displayName = "Mario 3D / Bob-Omb Battlefield",
        assetFile = file("../mario-3d/assets/models/Super Mario 64 Bob-Omb Battlefield.glb"),
        sourceNote = "local compatibility benchmark; not included in the N64 demo ROM"
    ),
    N64DemoAssetPreflightSpec(
        displayName = "Mario 3D / Mario Static Model",
        assetFile = file("../mario-3d/assets/models/Mario 64 Model.glb"),
        sourceNote = "local compatibility benchmark; not included in the N64 demo ROM"
    ),
    N64DemoAssetPreflightSpec(
        displayName = "Metroid 3D / Samus OBJ",
        assetFile = file("../kengine-3d-demos/assets/models/metroid3d/Samus Super Smash Bros N64/samus.obj"),
        sourceNote = "local compatibility benchmark; not included in the N64 demo ROM"
    )
)

val generateN64DemoModelAssets by tasks.registering {
    group = "n64"
    description = "Bakes N64 demo OBJ/MTL model sources into compact common Kotlin arrays."

    n64DemoModelSpecs.forEach { spec ->
        inputs.file(spec.objFile)
        inputs.file(spec.mtlFile)
    }
    val outputFile = layout.projectDirectory.file("src/commonMain/kotlin/n64demo/N64DemoModelAssets.kt")
    outputs.file(outputFile)

    doLast {
        val output = outputFile.asFile
        output.parentFile.mkdirs()
        writeTextIfChanged(output, renderN64DemoModelAssets(n64DemoModelSpecs))
    }
}

val preflightN64DemoAssets by tasks.registering {
    group = "n64"
    description = "Writes a hardware-budget report for N64 candidate OBJ/GLB assets."

    inputs.files(n64DemoAssetPreflightSpecs.map { it.assetFile }.filter { it.exists() })
    val reportFile = layout.buildDirectory.file("reports/n64-demo-assets/preflight.md")
    outputs.file(reportFile)

    doLast {
        val reports = n64DemoAssetPreflightSpecs.map { spec ->
            inspectN64DemoAssetBudget(spec, rootDir)
        }
        val output = reportFile.get().asFile
        output.parentFile.mkdirs()
        writeTextIfChanged(output, renderN64DemoAssetBudgetReport(reports))
        println("N64 asset preflight report: ${output.relativeTo(rootDir).invariantSeparatorsPath}")
    }
}

val downresN64DemoTextures by tasks.registering {
    group = "n64"
    description = "Creates N64-sized texture previews for candidate assets under build/."

    inputs.files(n64DemoAssetPreflightSpecs.map { it.assetFile }.filter { it.exists() })
    val outputDir = layout.buildDirectory.dir("n64-demo-assets/downres")
    outputs.dir(outputDir)

    doLast {
        val output = outputDir.get().asFile
        output.mkdirs()

        val reports = n64DemoAssetPreflightSpecs.map { spec ->
            inspectN64DemoAssetBudget(spec, rootDir)
        }
        writeN64DemoDownresTexturePreviews(reports, output, projectDir)
        println("N64 down-res texture previews: ${output.relativeTo(rootDir).invariantSeparatorsPath}")
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
    artifactBaseName.set("n64-demo")
    displayName.set("Kengine N64 Demo")
    mainClass.set("n64demo.N64DemoGame")

    sound("finish") {
        source.set(project.file("assets/sounds/finish.wav"))
    }

    sound("chord") {
        source.set(project.file("assets/sounds/chord.wav"))
    }
}

tasks.matching { task ->
    task.name.startsWith("compile") && task.name.contains("Kotlin")
}.configureEach {
    dependsOn(generateN64DemoModelAssets)
}

gradle.projectsEvaluated {
    rootProject.findProject(":kengine-n64")?.tasks?.matching { task ->
        task.name == "compileN64DemoKotlinStatic"
    }?.configureEach {
        dependsOn(generateN64DemoModelAssets)
    }
}

fun writeTextIfChanged(file: File, text: String) {
    if (!file.exists() || file.readText() != text) {
        file.writeText(text)
    }
}

fun renderN64DemoModelAssets(specs: List<N64DemoObjModelSpec>): String {
    val bakedModels = specs.map { spec ->
        val materials = parseN64DemoMtlColors(spec.mtlFile)
        val model = bakeN64DemoObj(spec, materials)
        spec to model
    }

    return buildString {
        appendLine("package n64demo")
        appendLine()
        appendLine("// Generated by :games:n64-demo:generateN64DemoModelAssets.")
        appendLine("// Source assets: Kenney Space Kit, CC0.")
        appendLine("object N64DemoModelAssets {")
        bakedModels.forEach { (spec, model) ->
            appendLine("    val ${spec.propertyName} = N64BakedWireModel3D(")
            appendLine("        name = ${spec.displayName.kotlinStringLiteral()},")
            appendLine("        source = ${spec.objFile.relativeTo(projectDir).invariantSeparatorsPath.kotlinStringLiteral()},")
            appendLine("        license = \"Kenney Space Kit, CC0\",")
            appendIntArray("vertices", model.vertices, "        ")
            appendLine(",")
            appendIntArray("edges", model.edges, "        ")
            appendLine(",")
            appendIntArray("triangles", model.triangles, "        ")
            appendLine(",")
            appendIntArray("colors", model.colors, "        ")
            appendLine()
            appendLine("    )")
            appendLine()
        }
        appendLine("    val all = arrayOf(${bakedModels.joinToString(", ") { it.first.propertyName }})")
        appendLine("}")
    }
}

data class BakedN64DemoObjModel(
    val vertices: List<Int>,
    val edges: List<Int>,
    val triangles: List<Int>,
    val colors: List<Int>
)

fun bakeN64DemoObj(
    spec: N64DemoObjModelSpec,
    materialColors: Map<String, Int>
): BakedN64DemoObjModel {
    val sourceVertices = mutableListOf<DoubleArray>()
    val colors = mutableListOf<Int>()
    val colorIndexes = linkedMapOf<Int, Int>()
    val sourceTriangles = mutableListOf<N64DemoObjTriangle>()
    var currentColor = materialColors.values.firstOrNull() ?: n64DemoRgba(224, 232, 255)

    fun colorIndex(color: Int): Int {
        return colorIndexes.getOrPut(color) {
            colors += color
            colors.lastIndex
        }
    }

    spec.objFile.forEachLine { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEachLine
        val parts = line.split(Regex("\\s+"))
        when (parts.firstOrNull()) {
            "v" -> {
                if (parts.size >= 4) {
                    sourceVertices += doubleArrayOf(parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
                }
            }
            "usemtl" -> {
                currentColor = materialColors[parts.getOrNull(1)] ?: currentColor
            }
            "f" -> {
                val face = parts.drop(1)
                    .mapNotNull { token -> parseN64DemoObjVertexIndex(token, sourceVertices.size) }
                if (face.size >= 3) {
                    val faceColorIndex = colorIndex(currentColor)
                    var triangleIndex = 1
                    while (triangleIndex < face.size - 1) {
                        val a = face[0]
                        val b = face[triangleIndex]
                        val c = face[triangleIndex + 1]
                        if (a != b && b != c && c != a) {
                            sourceTriangles += N64DemoObjTriangle(a, b, c, faceColorIndex)
                        }
                        triangleIndex += 1
                    }
                }
            }
        }
    }

    require(sourceTriangles.isNotEmpty()) {
        "OBJ model has no faces: ${spec.objFile}"
    }

    val minX = sourceVertices.minOf { it[0] }
    val maxX = sourceVertices.maxOf { it[0] }
    val minY = sourceVertices.minOf { it[1] }
    val maxY = sourceVertices.maxOf { it[1] }
    val minZ = sourceVertices.minOf { it[2] }
    val maxZ = sourceVertices.maxOf { it[2] }
    val centerX = (minX + maxX) / 2.0
    val centerY = (minY + maxY) / 2.0
    val centerZ = (minZ + maxZ) / 2.0
    val maxExtent = maxOf(maxX - minX, maxY - minY, maxZ - minZ).coerceAtLeast(0.0001)
    val scale = 448.0 / maxExtent

    val bakedVertices = sourceVertices.flatMap { vertex ->
        listOf(
            ((vertex[0] - centerX) * scale).roundToInt(),
            ((vertex[1] - centerY) * scale).roundToInt(),
            ((vertex[2] - centerZ) * scale).roundToInt()
        )
    }

    val triangleNormals = sourceTriangles.mapIndexed { triangleIndex, triangle ->
        val ab = triangle.a * 3
        val bb = triangle.b * 3
        val cb = triangle.c * 3
        val ax = bakedVertices[ab]
        val ay = bakedVertices[ab + 1]
        val az = bakedVertices[ab + 2]
        val bx = bakedVertices[bb]
        val by = bakedVertices[bb + 1]
        val bz = bakedVertices[bb + 2]
        val cx = bakedVertices[cb]
        val cy = bakedVertices[cb + 1]
        val cz = bakedVertices[cb + 2]
        val ux = (bx - ax).toLong()
        val uy = (by - ay).toLong()
        val uz = (bz - az).toLong()
        val vx = (cx - ax).toLong()
        val vy = (cy - ay).toLong()
        val vz = (cz - az).toLong()
        N64DemoObjTriangleNormal(
            triangleIndex = triangleIndex,
            triangle = triangle,
            nx = uy * vz - uz * vy,
            ny = uz * vx - ux * vz,
            nz = ux * vy - uy * vx
        )
    }

    val edgeUses = linkedMapOf<Pair<Int, Int>, MutableList<N64DemoObjTriangleNormal>>()
    triangleNormals.forEach { normal ->
        val triangle = normal.triangle
        addN64DemoEdgeUse(edgeUses, triangle.a, triangle.b, normal)
        addN64DemoEdgeUse(edgeUses, triangle.b, triangle.c, normal)
        addN64DemoEdgeUse(edgeUses, triangle.c, triangle.a, normal)
    }

    val featureEdges = edgeUses.mapNotNull { (key, adjacentTriangles) ->
        val edgeKind = classifyN64DemoEdge(adjacentTriangles)
        if (edgeKind == N64DemoObjEdgeKind.FALLBACK) return@mapNotNull null
        val startBase = key.first * 3
        val endBase = key.second * 3
        val dx = bakedVertices[startBase] - bakedVertices[endBase]
        val dy = bakedVertices[startBase + 1] - bakedVertices[endBase + 1]
        val dz = bakedVertices[startBase + 2] - bakedVertices[endBase + 2]
        val colorIndex = adjacentTriangles.firstOrNull()?.triangle?.colorIndex ?: 0
        N64DemoObjEdge(
            start = key.first,
            end = key.second,
            colorIndex = colorIndex,
            firstTriangle = adjacentTriangles.getOrNull(0)?.triangleIndex ?: -1,
            secondTriangle = adjacentTriangles.getOrNull(1)?.triangleIndex ?: -1,
            lengthSquared = dx * dx + dy * dy + dz * dz,
            kind = edgeKind,
            sharpnessScore = edgeSharpnessScore(adjacentTriangles)
        )
    }

    val edgesForOverlay = if (featureEdges.isNotEmpty()) {
        featureEdges
    } else {
        edgeUses.map { (key, adjacentTriangles) ->
            val startBase = key.first * 3
            val endBase = key.second * 3
            val dx = bakedVertices[startBase] - bakedVertices[endBase]
            val dy = bakedVertices[startBase + 1] - bakedVertices[endBase + 1]
            val dz = bakedVertices[startBase + 2] - bakedVertices[endBase + 2]
            val colorIndex = adjacentTriangles.firstOrNull()?.triangle?.colorIndex ?: 0
            N64DemoObjEdge(
                start = key.first,
                end = key.second,
                colorIndex = colorIndex,
                firstTriangle = adjacentTriangles.getOrNull(0)?.triangleIndex ?: -1,
                secondTriangle = adjacentTriangles.getOrNull(1)?.triangleIndex ?: -1,
                lengthSquared = dx * dx + dy * dy + dz * dz,
                kind = N64DemoObjEdgeKind.FALLBACK,
                sharpnessScore = 0
            )
        }
    }

    val sortedEdges = edgesForOverlay.sortedWith(
        compareBy<N64DemoObjEdge> { it.kind.priority }
            .thenByDescending { it.sharpnessScore }
            .thenBy { it.lengthSquared }
            .thenBy { it.start }
            .thenBy { it.end }
    )

    val bakedEdges = sortedEdges.flatMap { edge ->
        listOf(edge.start, edge.end, edge.colorIndex, edge.firstTriangle, edge.secondTriangle)
    }

    val bakedTriangles = sourceTriangles.flatMap { triangle ->
        listOf(triangle.a, triangle.b, triangle.c, triangle.colorIndex)
    }

    return BakedN64DemoObjModel(
        vertices = bakedVertices,
        edges = bakedEdges,
        triangles = bakedTriangles,
        colors = colors
    )
}

fun addN64DemoEdgeUse(
    edgeUses: MutableMap<Pair<Int, Int>, MutableList<N64DemoObjTriangleNormal>>,
    a: Int,
    b: Int,
    triangle: N64DemoObjTriangleNormal
) {
    if (a == b) return
    val key = if (a < b) a to b else b to a
    edgeUses.getOrPut(key) { mutableListOf() } += triangle
}

fun classifyN64DemoEdge(adjacentTriangles: List<N64DemoObjTriangleNormal>): N64DemoObjEdgeKind {
    if (adjacentTriangles.size != 2) {
        return N64DemoObjEdgeKind.BOUNDARY
    }

    val first = adjacentTriangles[0]
    val second = adjacentTriangles[1]
    if (first.triangle.colorIndex != second.triangle.colorIndex) {
        return N64DemoObjEdgeKind.MATERIAL_SEAM
    }

    val cosine = normalCosine(first, second)
    if (cosine == null) {
        return N64DemoObjEdgeKind.STRONG_CREASE
    }

    return if (cosine < n64StrongCreaseCosine) {
        N64DemoObjEdgeKind.STRONG_CREASE
    } else {
        N64DemoObjEdgeKind.FALLBACK
    }
}

fun edgeSharpnessScore(adjacentTriangles: List<N64DemoObjTriangleNormal>): Int {
    if (adjacentTriangles.size != 2) {
        return 1_000_000
    }
    val cosine = normalCosine(adjacentTriangles[0], adjacentTriangles[1]) ?: return 900_000
    return ((1.0 - cosine.coerceIn(-1.0, 1.0)) * 100_000.0).roundToInt()
}

fun normalCosine(first: N64DemoObjTriangleNormal, second: N64DemoObjTriangleNormal): Double? {
    val firstLength = normalLength(first)
    val secondLength = normalLength(second)
    if (firstLength <= 0.0001 || secondLength <= 0.0001) {
        return null
    }

    val dot = (first.nx * second.nx + first.ny * second.ny + first.nz * second.nz).toDouble()
    return dot / (firstLength * secondLength)
}

fun normalLength(normal: N64DemoObjTriangleNormal): Double {
    return sqrt((normal.nx * normal.nx + normal.ny * normal.ny + normal.nz * normal.nz).toDouble())
}

fun parseN64DemoObjVertexIndex(token: String, vertexCount: Int): Int? {
    val rawIndex = token.substringBefore('/').toIntOrNull() ?: return null
    val zeroBased = if (rawIndex > 0) rawIndex - 1 else vertexCount + rawIndex
    return zeroBased.takeIf { it in 0 until vertexCount }
}

fun parseN64DemoMtlColors(file: File): Map<String, Int> {
    val colors = linkedMapOf<String, Int>()
    var currentMaterial: String? = null
    file.forEachLine { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEachLine
        val parts = line.split(Regex("\\s+"))
        when (parts.firstOrNull()) {
            "newmtl" -> currentMaterial = parts.getOrNull(1)
            "Kd" -> {
                val material = currentMaterial ?: return@forEachLine
                if (parts.size >= 4) {
                    val red = (parts[1].toDouble() * 255.0).roundToInt()
                    val green = (parts[2].toDouble() * 255.0).roundToInt()
                    val blue = (parts[3].toDouble() * 255.0).roundToInt()
                    colors[material] = n64DemoRgba(red, green, blue)
                }
            }
        }
    }
    return colors
}

fun String.kotlinStringLiteral(): String {
    return buildString {
        append('"')
        this@kotlinStringLiteral.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
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

fun n64DemoRgba(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
    fun clamp(value: Int): Int {
        return when {
            value < 0 -> 0
            value > 255 -> 255
            else -> value
        }
    }
    return clamp(red) or
        (clamp(green) shl 8) or
        (clamp(blue) shl 16) or
        (clamp(alpha) shl 24)
}
