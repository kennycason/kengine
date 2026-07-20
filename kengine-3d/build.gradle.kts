import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    id("kengine.sdl-dylib")
}

val shaderSourceDir = layout.projectDirectory.dir("src/nativeMain/shaders")
val generatedShaderSourcesDir = layout.buildDirectory.dir("generated/kengine3dShaders/nativeMain/kotlin")
val compiledMetalShaderDir = layout.buildDirectory.dir("generated/kengine3dShaders/nativeMain/metallib")
val compiledSpirvShaderDir = layout.buildDirectory.dir("generated/kengine3dShaders/nativeMain/spirv")
val compiledDxilShaderDir = layout.buildDirectory.dir("generated/kengine3dShaders/nativeMain/dxil")

val shaderArtifactFormats = listOf(
    ShaderArtifactFormat(
        extension = "metallib",
        propertySuffix = "METALLIB",
        factoryName = "metallib",
        directory = compiledMetalShaderDir
    ),
    ShaderArtifactFormat(
        extension = "spv",
        propertySuffix = "SPIRV",
        factoryName = "spirv",
        directory = compiledSpirvShaderDir
    ),
    ShaderArtifactFormat(
        extension = "dxil",
        propertySuffix = "DXIL",
        factoryName = "dxil",
        directory = compiledDxilShaderDir
    )
)

val compileKengine3dMetalShaders by tasks.registering {
    val shaderInputs = shaderSourceDir
    val metallibOutputDir = compiledMetalShaderDir

    inputs.dir(shaderInputs)
    outputs.dir(metallibOutputDir)

    doLast {
        val outputRoot = metallibOutputDir.get().asFile
        outputRoot.deleteRecursively()
        outputRoot.mkdirs()

        if (!commandSucceeds("xcrun", "-f", "metal") || !commandSucceeds("xcrun", "-f", "metallib")) {
            logger.lifecycle("Skipping kengine-3d Metal shader compilation because xcrun metal/metallib is unavailable.")
            return@doLast
        }

        shaderInputs.asFile
            .walkTopDown()
            .filter { it.isFile && it.extension == "msl" }
            .sortedBy { it.relativeTo(shaderInputs.asFile).invariantSeparatorsPath }
            .forEach { shader ->
                val relativePath = shader.relativeTo(shaderInputs.asFile).invariantSeparatorsPath
                val artifactPath = relativePath.removeSuffix(".msl")
                val airFile = temporaryDir.resolve("$artifactPath.air")
                val metallibFile = outputRoot.resolve("$artifactPath.metallib")

                airFile.parentFile.mkdirs()
                metallibFile.parentFile.mkdirs()

                runCommand(
                    "xcrun",
                    "-sdk",
                    "macosx",
                    "metal",
                    "-x",
                    "metal",
                    "-c",
                    shader.absolutePath,
                    "-o",
                    airFile.absolutePath
                )
                runCommand(
                    "xcrun",
                    "-sdk",
                    "macosx",
                    "metallib",
                    airFile.absolutePath,
                    "-o",
                    metallibFile.absolutePath
                )
            }
    }
}

val compileKengine3dShaderArtifacts by tasks.registering {
    group = "kengine"
    description = "Compiles kengine-3d shader artifacts for available backends."

    outputs.dir(compiledSpirvShaderDir)
    outputs.dir(compiledDxilShaderDir)
    dependsOn(compileKengine3dMetalShaders)

    doLast {
        compiledSpirvShaderDir.get().asFile.mkdirs()
        compiledDxilShaderDir.get().asFile.mkdirs()
    }
}

val reportKengine3dShaderTools by tasks.registering {
    group = "kengine"
    description = "Reports optional shader compiler tools available to kengine-3d."

    doLast {
        logger.lifecycle("kengine-3d shader tools:")
        logger.lifecycle("  xcrun metal: ${toolStatus("xcrun", "-f", "metal")}")
        logger.lifecycle("  xcrun metallib: ${toolStatus("xcrun", "-f", "metallib")}")
        logger.lifecycle("  SDL_shadercross: ${toolStatus("SDL_shadercross")}")
        logger.lifecycle("  shadercross: ${toolStatus("shadercross")}")
        logger.lifecycle("  glslangValidator: ${toolStatus("glslangValidator")}")
        logger.lifecycle("  dxc: ${toolStatus("dxc")}")
    }
}

val generateKengine3dShaderSources by tasks.registering {
    val shaderInputs = shaderSourceDir
    val kotlinOutputDir = generatedShaderSourcesDir

    inputs.dir(shaderInputs)
    shaderArtifactFormats.forEach { format ->
        inputs.dir(format.directory).optional()
    }
    outputs.dir(kotlinOutputDir)
    dependsOn(compileKengine3dShaderArtifacts)

    doLast {
        val shaders = shaderInputs.asFile
            .walkTopDown()
            .filter { it.isFile && it.extension == "msl" }
            .sortedBy { it.relativeTo(shaderInputs.asFile).invariantSeparatorsPath }
            .toList()
        val shaderPrograms = shaders
            .groupBy { shader ->
                shader.name
                    .removeSuffix(".vertex.msl")
                    .removeSuffix(".fragment.msl")
            }
            .toSortedMap()
        val outputFile = kotlinOutputDir.get().file("com/kengine/three/Kengine3DShaderSources.kt").asFile

        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            buildString {
                appendLine("package com.kengine.three")
                appendLine()
                appendLine("// Generated by :kengine-3d:generateKengine3dShaderSources.")
                appendLine("internal object Kengine3DShaderArtifacts {")
                val compiledArtifacts = shaderArtifactFormats.flatMap { format ->
                    format.artifacts()
                }
                compiledArtifacts.forEach { artifact ->
                    val byteLiteralLines = artifact.file.readBytes().toByteLiteralLines()
                    appendLine("    val ${artifact.propertyName}: ByteArray = byteArrayOf(")
                    byteLiteralLines.forEachIndexed { index, line ->
                        val suffix = if (index == byteLiteralLines.lastIndex) "" else ","
                        appendLine("        $line$suffix")
                    }
                    appendLine("    )")
                    appendLine()
                }
                appendLine("}")
                appendLine()
                appendLine("internal object Kengine3DShaderSources {")
                shaders.forEach { shader ->
                    val propertyName = shader.nameWithoutExtension
                        .replace(Regex("[^A-Za-z0-9]+"), "_")
                        .trim('_')
                        .uppercase() + "_MSL"
                    appendLine("    val $propertyName: String = \"\"\"")
                    appendLine(shader.readText().trimEnd())
                    appendLine("    \"\"\".trimIndent()")
                    appendLine()
                }
                appendLine("}")
                appendLine()
                appendLine("internal object Kengine3DShaderPrograms {")
                shaderPrograms.forEach { (programName, programShaders) ->
                    val vertexShader = programShaders.singleOrNull { it.name == "$programName.vertex.msl" }
                        ?: throw GradleException("Missing vertex shader for program '$programName'.")
                    val fragmentShader = programShaders.singleOrNull { it.name == "$programName.fragment.msl" }
                        ?: throw GradleException("Missing fragment shader for program '$programName'.")
                    val vertexSource = vertexShader.readText()
                    val fragmentSource = fragmentShader.readText()
                    val propertyName = programName.toShaderPropertyName()
                    val vertexArtifacts = shaderArtifactsFor(vertexShader, compiledArtifacts)
                    val fragmentArtifacts = shaderArtifactsFor(fragmentShader, compiledArtifacts)
                    appendLine("    val $propertyName = GpuShaderProgramSource3D(")
                    appendLine("        label = \"${programName.toShaderLabel()}\",")
                    appendLine("        vertex = GpuShaderStageSource3D(")
                    appendLine("            artifacts = listOf(")
                    vertexArtifacts.forEachIndexed { index, artifactExpression ->
                        val suffix = if (index == vertexArtifacts.lastIndex) "" else ","
                        appendLine("                $artifactExpression$suffix")
                    }
                    appendLine("            ),")
                    appendLine("            uniformBuffers = ${vertexSource.shaderBufferCount()}u")
                    appendLine("        ),")
                    appendLine("        fragment = GpuShaderStageSource3D(")
                    appendLine("            artifacts = listOf(")
                    fragmentArtifacts.forEachIndexed { index, artifactExpression ->
                        val suffix = if (index == fragmentArtifacts.lastIndex) "" else ","
                        appendLine("                $artifactExpression$suffix")
                    }
                    appendLine("            ),")
                    appendLine("            uniformBuffers = ${fragmentSource.shaderBufferCount()}u,")
                    appendLine("            samplers = ${fragmentSource.shaderSamplerCount()}u")
                    appendLine("        )")
                    appendLine("    )")
                    appendLine()
                }
                appendLine("}")
            }
        )
    }
}

data class ShaderArtifactFormat(
    val extension: String,
    val propertySuffix: String,
    val factoryName: String,
    val directory: Provider<Directory>
) {
    fun artifacts(): List<ShaderCompiledArtifact> {
        val root = directory.get().asFile
        if (!root.isDirectory) {
            return emptyList()
        }

        return root
            .walkTopDown()
            .filter { it.isFile && it.extension == extension }
            .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
            .map { artifact ->
                ShaderCompiledArtifact(
                    sourceStem = artifact.nameWithoutExtension,
                    propertyName = artifact.nameWithoutExtension.toShaderPropertyName() + "_$propertySuffix",
                    expression = "GpuShaderArtifact3D.$factoryName(" +
                        "Kengine3DShaderArtifacts.${artifact.nameWithoutExtension.toShaderPropertyName()}_$propertySuffix" +
                        ")",
                    file = artifact
                )
            }
            .toList()
    }
}

data class ShaderCompiledArtifact(
    val sourceStem: String,
    val propertyName: String,
    val expression: String,
    val file: File
)

fun shaderArtifactsFor(shader: File, compiledArtifacts: List<ShaderCompiledArtifact>): List<String> {
    val sourceStem = shader.nameWithoutExtension
    val mslPropertyName = sourceStem.toShaderPropertyName() + "_MSL"
    val artifacts = compiledArtifacts
        .filter { it.sourceStem == sourceStem }
        .map { it.expression }
        .toMutableList()

    artifacts += "GpuShaderArtifact3D.mslSource(Kengine3DShaderSources.$mslPropertyName)"
    return artifacts
}

fun commandSucceeds(vararg command: String): Boolean {
    return try {
        ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
            .also { process -> process.inputStream.readBytes() }
            .waitFor() == 0
    } catch (_: Exception) {
        false
    }
}

fun commandOutput(vararg command: String): Pair<Int, String> {
    return try {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.readBytes().decodeToString().trim()
        process.waitFor() to output
    } catch (e: Exception) {
        -1 to (e.message ?: e::class.simpleName.orEmpty())
    }
}

fun toolStatus(vararg command: String): String {
    val (exitCode, output) = commandOutput(*command)
    return if (exitCode == 0 && output.isNotBlank()) {
        output
    } else if (exitCode == 0) {
        "available"
    } else {
        "missing"
    }
}

fun runCommand(vararg command: String) {
    val exitCode = ProcessBuilder(*command)
        .inheritIO()
        .start()
        .waitFor()
    if (exitCode != 0) {
        throw GradleException("Command failed with exit code $exitCode: ${command.joinToString(" ")}")
    }
}

fun ByteArray.toByteLiteralLines(bytesPerLine: Int = 16): List<String> {
    return asIterable()
        .chunked(bytesPerLine)
        .map { chunk -> chunk.joinToString(", ") { it.toString() } }
}

fun String.toShaderPropertyName(): String {
    return replace(Regex("[^A-Za-z0-9]+"), "_")
        .trim('_')
        .uppercase()
}

fun String.toShaderLabel(): String {
    val label = replace('-', ' ')
    return when (this) {
        "debug",
        "mesh",
        "primitive" -> label
        else -> "$label mesh"
    }
}

fun String.shaderBufferCount(): Int {
    return Regex("\\[\\[buffer\\((\\d+)\\)\\]\\]")
        .findAll(this)
        .map { it.groupValues[1] }
        .toSet()
        .count()
}

fun String.shaderSamplerCount(): Int {
    return Regex("\\[\\[sampler\\((\\d+)\\)\\]\\]")
        .findAll(this)
        .map { it.groupValues[1] }
        .toSet()
        .count()
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )
    }

    val publishAllNativeTargets = providers.gradleProperty("kengine.publish.allNativeTargets")
        .map(String::toBoolean)
        .getOrElse(false)
    val nativeTargets = if (publishAllNativeTargets) {
        listOf(macosArm64(), linuxX64(), mingwX64())
    } else {
        listOf(
            when (KengineHostTarget.name) {
                "macosArm64" -> macosArm64()
                "macosX64" -> macosX64()
                "linuxX64" -> linuxX64()
                "linuxArm64" -> linuxArm64()
                "mingwX64" -> mingwX64()
                else -> throw GradleException("Host target [${KengineHostTarget.name}] is not supported.")
            }
        )
    }

    sourceSets.maybeCreate("nativeMain").dependsOn(sourceSets.getByName("commonMain"))
    sourceSets.maybeCreate("nativeTest").dependsOn(sourceSets.getByName("commonTest"))
    sourceSets.getByName("nativeMain").kotlin.srcDir(generatedShaderSourcesDir)
    nativeTargets.forEach { nativeTarget ->
        sourceSets.getByName("${nativeTarget.name}Main").dependsOn(sourceSets.getByName("nativeMain"))
        sourceSets.getByName("${nativeTarget.name}Test").dependsOn(sourceSets.getByName("nativeTest"))
    }

    nativeTargets.forEach { nativeTarget ->
        nativeTarget.apply {
            binaries.all {
                linkerOpts(PlatformConfig.sharedLibLinkerOpts("SDL3", "SDL3_image"))
            }

            compilations["main"].compileTaskProvider.configure {
                kotlinOptions {
                    freeCompilerArgs += listOf(
                        "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                        "-opt-in=kotlin.ExperimentalStdlibApi"
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kengine"))
                implementation(project(":kengine-reactive"))
                api(libs.kotlinxSerializationJson)
                api(libs.kotlinxCoroutinesCore)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":kengine-test"))
            }
        }
    }
}

tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn(generateKengine3dShaderSources)
}
