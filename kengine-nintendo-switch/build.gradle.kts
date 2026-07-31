import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.util.Properties
import javax.imageio.ImageIO

plugins {
    base
}

group = "kengine.nintendo.switch"
version = "0.1.0"

val switchOutputDir = layout.buildDirectory.dir("switch")
val cOnlyObject = switchOutputDir.map { it.file("obj/main_c_only.o") }
val switchCOnlyElf = switchOutputDir.map { it.file("kengine-nintendo-switch-c-only.elf") }
val switchNacp = switchOutputDir.map { it.file("kengine-nintendo-switch.nacp") }
val switchCOnlyNro = switchOutputDir.map { it.file("kengine-nintendo-switch-c-only.nro") }

val kengineKotlinLocalProperties = Properties().apply {
    val localProperties = rootProject.file("kengine-kotlin/local.properties")
    if (localProperties.isFile) {
        localProperties.inputStream().use(::load)
    }
}

fun localProperty(name: String): String? {
    return kengineKotlinLocalProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }
}

data class ConfiguredKotlinNativeCompiler(
    val executable: File,
    val source: String
)

fun configuredValue(propertyName: String, defaultValue: String): String {
    providers.gradleProperty(propertyName).orNull?.let { return it }
    localProperty(propertyName)?.let { return it }
    return defaultValue
}

val kotlinTarget = providers.provider {
    configuredValue("kengine.switch.kotlinTarget", "linux_arm64")
}

val ffmpegExecutable = providers.gradleProperty("kengine.switch.ffmpeg")
    .orElse(providers.environmentVariable("FFMPEG"))
    .orElse("ffmpeg")

fun envOrDefault(name: String, defaultValue: String): String {
    return providers.environmentVariable(name).orElse(defaultValue).get()
}

fun configuredKotlincNative(): ConfiguredKotlinNativeCompiler {
    providers.gradleProperty("kengine.switch.kotlinNativeHome").orNull?.let {
        return ConfiguredKotlinNativeCompiler(
            file(it).resolve("bin/kotlinc-native"),
            "Gradle property kengine.switch.kotlinNativeHome"
        )
    }
    localProperty("kengine.switch.kotlinNativeHome")?.let {
        return ConfiguredKotlinNativeCompiler(
            file(it).resolve("bin/kotlinc-native"),
            "kengine-kotlin/local.properties kengine.switch.kotlinNativeHome"
        )
    }

    throw GradleException(
        "Switch Kotlin/Native home is not configured. Run ./kengine-kotlin/build-kotlin-native-dist.sh " +
            "or set -Pkengine.switch.kotlinNativeHome=/path/to/kotlin-native/dist."
    )
}

fun kotlincNative(): File {
    return configuredKotlincNative().executable
}

fun cinterop(): File {
    return kotlincNative().parentFile.resolve("cinterop")
}

fun kotlinNativeTargets(compiler: File): List<String> {
    if (!compiler.isFile) {
        return emptyList()
    }

    val output = try {
        val process = ProcessBuilder(compiler.absolutePath, "-list-targets")
            .redirectErrorStream(true)
            .start()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            return emptyList()
        }
        text
    } catch (_: Exception) {
        return emptyList()
    }

    return output.lineSequence()
        .map { it.trim().substringBefore(" ") }
        .filter { it.isNotEmpty() }
        .toList()
}

fun validateKotlinNativeTarget(compiler: File, target: String) {
    if (!compiler.canExecute()) {
        throw GradleException("Kotlin/Native compiler is not executable: ${compiler.absolutePath}")
    }

    val targets = kotlinNativeTargets(compiler)
    if (targets.isNotEmpty() && target !in targets) {
        throw GradleException(
            "Kotlin/Native compiler at ${compiler.absolutePath} does not list target '$target'. " +
                "Configure the Switch fork with -Pkengine.switch.kotlinNativeHome=/path/to/kotlin-native/dist. " +
                "Available targets include: ${targets.take(12).joinToString(", ")}"
        )
    }
}

fun devkitPro(): File {
    return file(envOrDefault("DEVKITPRO", "/opt/devkitpro"))
}

fun devkitA64(): File {
    providers.environmentVariable("DEVKITA64").orNull?.let { return file(it) }
    return devkitPro().resolve("devkitA64")
}

fun tool(path: File, setup: String): File {
    if (!path.exists()) {
        throw GradleException("$setup Missing: ${path.absolutePath}")
    }
    return path
}

fun aarch64Tool(name: String): File {
    return tool(
        devkitA64().resolve("bin/$name"),
        "Install devkitPro switch-dev or set DEVKITPRO/DEVKITA64."
    )
}

fun devkitTool(name: String): File {
    return tool(
        devkitPro().resolve("tools/bin/$name"),
        "Install devkitPro switch-tools or set DEVKITPRO."
    )
}

fun Exec.configureSwitchEnvironment() {
    val devkitProDir = devkitPro().absolutePath
    val devkitA64Dir = devkitA64().absolutePath

    environment("DEVKITPRO", devkitProDir)
    environment("DEVKITA64", devkitA64Dir)
    environment(
        "PATH",
        listOf(
            "$devkitA64Dir/bin",
            "$devkitProDir/tools/bin",
            "$devkitProDir/pacman/bin",
            System.getenv("PATH").orEmpty()
        ).joinToString(File.pathSeparator)
    )
}

fun libnxInclude(): File {
    return tool(devkitPro().resolve("libnx/include"), "Install devkitPro libnx.")
}

fun libnxLib(): File {
    return tool(devkitPro().resolve("libnx/lib"), "Install devkitPro libnx.")
}

fun switchSpecs(): File {
    return tool(devkitPro().resolve("libnx/switch.specs"), "Install devkitPro libnx.")
}

val switchCFlags = listOf(
    "-g",
    "-Wall",
    "-O2",
    "-ffunction-sections",
    "-fdata-sections",
    "-D__SWITCH__",
    "-march=armv8-a+crc+crypto",
    "-mtune=cortex-a57",
    "-mtp=soft",
    "-fPIE"
)

val switchArchFlags = listOf(
    "-march=armv8-a+crc+crypto",
    "-mtune=cortex-a57",
    "-mtp=soft",
    "-fPIE"
)

tasks.register("switchToolchainInfo") {
    group = "switch"
    description = "Prints the experimental Switch toolchain paths used by this module."

    doLast {
        val compilerConfig = configuredKotlincNative()
        val compiler = compilerConfig.executable
        val targets = kotlinNativeTargets(compiler)
        val target = kotlinTarget.get()
        val devkitProDir = devkitPro()
        val devkitA64Dir = devkitA64()
        println("Kotlin/Native compiler: ${compiler.absolutePath} (${compiler.exists()})")
        println("Kotlin/Native compiler source: ${compilerConfig.source}")
        println("Kotlin/Native target: $target")
        println("Kotlin/Native target supported: ${if (targets.isEmpty()) "(not probed)" else target in targets}")
        println("Kengine Kotlin fork: ${localProperty("kengine.kotlin.repo") ?: "(not configured)"}")
        println("Switch Kotlin/Native home property: kengine.switch.kotlinNativeHome")
        println("DEVKITPRO: ${devkitProDir.absolutePath} (${devkitProDir.exists()})")
        println("DEVKITA64: ${devkitA64Dir.absolutePath} (${devkitA64Dir.exists()})")
        println("aarch64-none-elf-gcc: ${devkitA64Dir.resolve("bin/aarch64-none-elf-gcc").absolutePath}")
        println("nacptool: ${devkitProDir.resolve("tools/bin/nacptool").absolutePath}")
        println("elf2nro: ${devkitProDir.resolve("tools/bin/elf2nro").absolutePath}")
    }
}

tasks.register("validateSwitchKotlinToolchain") {
    group = "switch"
    description = "Validates that the configured Switch Kotlin/Native compiler supports the selected target."

    doLast {
        val compilerConfig = configuredKotlincNative()
        val compiler = tool(
            compilerConfig.executable,
            "Configure the Switch Kotlin/Native fork with kengine.switch.kotlinNativeHome."
        )
        val target = kotlinTarget.get()
        validateKotlinNativeTarget(compiler, target)
        println("Switch Kotlin/Native toolchain OK: $target via ${compiler.absolutePath}")
    }
}

data class SwitchGameRegistration(
    val gameProjectPath: String,
    val artifactBaseName: String,
    val displayName: String,
    val buildTaskName: String
)

data class SwitchSpriteAssetBuild(
    val asset: KengineNintendoSwitchSpriteAsset,
    val symbolName: String,
    val rawFile: Provider<RegularFile>,
    val objectFile: Provider<RegularFile>,
    val objectTaskName: String
)

data class SwitchSoundAssetBuild(
    val asset: KengineNintendoSwitchSoundAsset,
    val symbolName: String,
    val pcmFile: Provider<RegularFile>,
    val objectFile: Provider<RegularFile>,
    val objectTaskName: String
)

data class SwitchImageDimensions(
    val width: Int,
    val height: Int
)

fun switchGameExtension(project: Project): KengineNintendoSwitchGameExtension? {
    return project.extensions.findByName("kengineNintendoSwitch") as? KengineNintendoSwitchGameExtension
}

fun stableSpriteId(name: String): Int {
    return stableAssetId("sprite:", name)
}

fun stableSoundId(name: String): Int {
    return stableAssetId("sound:", name)
}

fun stableAssetId(prefix: String, name: String): Int {
    var hash = -0x7ee3623b
    for (char in "$prefix$name") {
        hash = hash xor char.code
        hash *= 0x01000193
    }
    return if (hash == 0) 1 else hash
}

fun cIdentifier(value: String): String {
    return value.map { char ->
        if ((char in 'a'..'z') || (char in 'A'..'Z') || (char in '0'..'9')) char else '_'
    }.joinToString("").trim('_').ifEmpty { "asset" }
}

fun switchImageDimensions(imageFile: File): SwitchImageDimensions {
    ImageIO.read(imageFile)?.let { image ->
        return SwitchImageDimensions(image.width, image.height)
    }

    if (imageFile.extension.equals("bmp", ignoreCase = true)) {
        val bytes = imageFile.readBytes()
        fun littleEndianInt(offset: Int): Int {
            return (bytes[offset].toInt() and 0xff) or
                ((bytes[offset + 1].toInt() and 0xff) shl 8) or
                ((bytes[offset + 2].toInt() and 0xff) shl 16) or
                ((bytes[offset + 3].toInt() and 0xff) shl 24)
        }

        if (bytes.size >= 26 && bytes[0] == 'B'.code.toByte() && bytes[1] == 'M'.code.toByte()) {
            val width = littleEndianInt(18)
            val rawHeight = littleEndianInt(22)
            val height = if (rawHeight < 0) -rawHeight else rawHeight
            if (width > 0 && height > 0) {
                return SwitchImageDimensions(width, height)
            }
        }
    }

    throw GradleException("Unable to read sprite asset dimensions: ${imageFile.absolutePath}")
}

fun registerSwitchGameBuild(
    gameProject: Project,
    extension: KengineNintendoSwitchGameExtension
): SwitchGameRegistration {
    val artifactBaseName = extension.artifactBaseName.get()
    val displayName = extension.displayName.get()
    val author = extension.author.get()
    val gameVersion = extension.version.get()
    val mainClass = extension.mainClass.orNull
        ?: throw GradleException("${gameProject.path} must configure kengineNintendoSwitch.mainClass.")
    val taskPrefix = kengineNintendoSwitchTaskPrefix(artifactBaseName)
    val buildTaskName = extension.backendBuildTaskName.orNull
        ?: kengineNintendoSwitchBuildTaskName(artifactBaseName)
    val factoryClassName = mainClass.substringAfterLast(".")
    val factoryImport = if (mainClass.contains(".")) "import $mainClass\n" else ""

    val gameOutputDir = switchOutputDir.map { it.dir("games/$artifactBaseName") }
    val kotlinOutputBase = gameOutputDir.map { it.file("kotlin/kengine_switch_kotlin") }
    val kotlinApiHeader = gameOutputDir.map { it.file("kotlin/kengine_switch_kotlin_api.h") }
    val kotlinStaticLib = gameOutputDir.map { it.file("kotlin/libkengine_switch_kotlin.a") }
    val storageInteropBase = gameOutputDir.map { it.file("kotlin/kengine_switch_storage") }
    val storageInteropKlib = gameOutputDir.map { it.file("kotlin/kengine_switch_storage.klib") }
    val gameFactory = gameOutputDir.map { it.file("generated/KengineSwitchGameFactory.kt") }
    val cObject = gameOutputDir.map { it.file("obj/main.o") }
    val switchElf = gameOutputDir.map { it.file("$artifactBaseName.elf") }
    val switchNacp = gameOutputDir.map { it.file("$artifactBaseName.nacp") }
    val switchIcon = gameOutputDir.map { it.file("$artifactBaseName-icon.jpg") }
    val switchNro = gameOutputDir.map { it.file("$artifactBaseName.nro") }
    val spriteManifestHeader = gameOutputDir.map { it.file("generated/c/kengine_switch_sprite_assets.h") }
    val spriteManifestSource = gameOutputDir.map { it.file("generated/c/kengine_switch_sprite_assets.c") }
    val spriteManifestObject = gameOutputDir.map { it.file("obj/kengine_switch_sprite_assets.o") }

    val switchKotlinSources = project.fileTree("src/main/kotlin") {
        include("**/*.kt")
    }
    val kengineCoreSources = rootProject.fileTree("kengine-core/src/commonMain/kotlin") {
        include("**/*.kt")
    }
    val gameSourceProjects = (listOf(gameProject) + extension.gameSourceProjects)
        .distinctBy { it.path }
    val gameSourceTrees = gameSourceProjects.map { sourceProject ->
        sourceProject.fileTree("src/commonMain/kotlin") {
            include("**/*.kt")
        }
    }
    val portableAssetExtensions = gameSourceProjects.mapNotNull { sourceProject ->
        sourceProject.extensions.findByName("kenginePortableAssets") as? KenginePortableAssetsExtension
    }
    val generatedGameSourceDirs = portableAssetExtensions.mapNotNull { it.generatedSourceDir }
    val generatedAssetTaskPaths = portableAssetExtensions.mapNotNull { it.generateTaskPath }
    val kotlinSources = providers.provider {
        (switchKotlinSources.files +
            kengineCoreSources.files +
            gameSourceTrees.flatMap { it.files } +
            generatedGameSourceDirs.flatMap { generatedSourceDir ->
                generatedSourceDir.get().asFileTree.matching {
                    include("**/*.kt")
                }.files
            } +
            gameFactory.get().asFile).sortedBy { it.absolutePath }
    }

    val generateFactoryTaskName = "generate${taskPrefix}GameFactory"
    tasks.register(generateFactoryTaskName) {
        group = "switch"
        description = "Generates the Kotlin game factory for $displayName."

        inputs.property("artifactBaseName", artifactBaseName)
        inputs.property("mainClass", mainClass)
        outputs.file(gameFactory)

        doLast {
            val output = gameFactory.get().asFile
            output.parentFile.mkdirs()
            output.writeText(
                """
                package kengine.switchruntime

                import com.kengine.PortableGame
                $factoryImport
                fun createSwitchPortableGame(): PortableGame = $factoryClassName()
                fun switchGameName(): String = "$artifactBaseName"
                """.trimIndent()
            )
        }
    }

    val generateStorageInteropTaskName = "generate${taskPrefix}StorageInterop"
    tasks.register<Exec>(generateStorageInteropTaskName) {
        group = "switch"
        description = "Generates Kotlin/Native cinterop bindings for $displayName Switch storage callbacks."

        val storageHeader = file("src/main/c/kengine_switch_storage.h")
        inputs.file(storageHeader)
        inputs.property("kotlinTarget", kotlinTarget)
        inputs.property("cinterop", providers.provider { cinterop().absolutePath })
        outputs.file(storageInteropKlib)

        doFirst {
            val compiler = tool(
                kotlincNative(),
                "Configure the Switch Kotlin/Native fork with kengine.switch.kotlinNativeHome."
            )
            validateKotlinNativeTarget(compiler, kotlinTarget.get())
            val interop = tool(
                cinterop(),
                "Set -Pkengine.switch.kotlinNativeHome=/path/to/kotlin-native/dist with cinterop."
            )
            storageInteropBase.get().asFile.parentFile.mkdirs()
            commandLine(
                interop.absolutePath,
                "-target",
                kotlinTarget.get(),
                "-pkg",
                "kengine.switchhost",
                "-header",
                storageHeader.absolutePath,
                "-o",
                storageInteropBase.get().asFile.absolutePath
            )
        }
    }

    val compileKotlinTaskName = "compile${taskPrefix}KotlinStatic"
    tasks.register<Exec>(compileKotlinTaskName) {
        group = "switch"
        description = "Compiles $displayName, shared kengine core sources, and the Switch runtime as a Kotlin/Native static library."
        dependsOn(listOf(generateFactoryTaskName, generateStorageInteropTaskName) + generatedAssetTaskPaths)

        inputs.files(switchKotlinSources, kengineCoreSources)
        inputs.files(gameSourceTrees)
        inputs.file(storageInteropKlib)
        generatedGameSourceDirs.forEach { generatedSourceDir ->
            inputs.dir(generatedSourceDir)
                .optional()
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
        inputs.file(gameFactory)
        inputs.property("artifactBaseName", artifactBaseName)
        inputs.property("mainClass", mainClass)
        inputs.property("kotlinTarget", kotlinTarget)
        inputs.property("kotlincNative", providers.provider { kotlincNative().absolutePath })
        inputs.dir(providers.provider {
            kotlincNative().parentFile.parentFile.resolve("konan/targets/${kotlinTarget.get()}/native")
        })
            .optional()
            .withPathSensitivity(PathSensitivity.ABSOLUTE)
        inputs.dir(providers.provider {
            kotlincNative().parentFile.parentFile.resolve("konan/lib")
        })
            .optional()
            .withPathSensitivity(PathSensitivity.ABSOLUTE)
        inputs.file(rootProject.file("kengine-kotlin/local.properties"))
            .optional()
            .withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.files(kotlinApiHeader, kotlinStaticLib)

        doFirst {
            val compiler = tool(
                kotlincNative(),
                "Set -Pkengine.switch.kotlinNativeHome=/path/to/kotlin-native/dist."
            )
            validateKotlinNativeTarget(compiler, kotlinTarget.get())
            kotlinOutputBase.get().asFile.parentFile.mkdirs()
            commandLine(
                compiler.absolutePath,
                "-target",
                kotlinTarget.get(),
                "-produce",
                "static",
                "-library",
                storageInteropKlib.get().asFile.absolutePath,
                "-output",
                kotlinOutputBase.get().asFile.absolutePath
            )
            args(kotlinSources.get().map { it.absolutePath })
        }
    }

    val cDefineArgs = mutableListOf<String>()
    cDefineArgs += extension.cDefines.get().map { define ->
        if (define.startsWith("-D")) define else "-D$define"
    }

    val assetObjectFiles = mutableListOf<Provider<RegularFile>>()
    val assetObjectTaskNames = mutableListOf<String>()
    val mainGeneratedHeaderFiles = mutableListOf<Provider<RegularFile>>()
    val mainGeneratedHeaderTaskNames = mutableListOf<String>()

    val spriteAssetBuilds = extension.spriteAssets.mapIndexed { index, asset ->
        val symbolName = "${index}_${cIdentifier(asset.name)}"
        val assetTaskPrefix = "$taskPrefix${kengineNintendoSwitchTaskPrefix("${index}_${asset.name}")}"
        val rawSprite = gameOutputDir.map { it.file("sprites/$symbolName.rgba") }
        val spriteObject = gameOutputDir.map { it.file("obj/${symbolName}_rgba.o") }
        val convertTaskName = "convert${assetTaskPrefix}SpriteRgba"
        val objectTaskName = "compile${assetTaskPrefix}SpriteObject"
        val spriteSource = asset.source.orNull?.asFile
            ?: throw GradleException("${gameProject.path} sprite asset '${asset.name}' must configure source.")

        tasks.register<Exec>(convertTaskName) {
            group = "switch"
            description = "Converts sprite asset '${asset.name}' for $displayName to raw RGBA pixels."

            inputs.file(spriteSource)
                .withPathSensitivity(PathSensitivity.RELATIVE)
            inputs.files(asset.extraInputs)
            outputs.file(rawSprite)

            doFirst {
                if (!spriteSource.isFile) {
                    throw GradleException("Missing sprite asset '${asset.name}' for $displayName: ${spriteSource.absolutePath}")
                }

                val rawFile = rawSprite.get().asFile
                rawFile.parentFile.mkdirs()
                commandLine(
                    ffmpegExecutable.get(),
                    "-hide_banner",
                    "-loglevel",
                    "error",
                    "-y",
                    "-i",
                    spriteSource.absolutePath,
                    "-f",
                    "rawvideo",
                    "-pix_fmt",
                    "rgba",
                    rawFile.absolutePath
                )
            }
        }

        tasks.register<Exec>(objectTaskName) {
            group = "switch"
            description = "Embeds sprite asset '${asset.name}' for $displayName as a linkable object."
            dependsOn(convertTaskName)

            inputs.file(rawSprite)
            outputs.file(spriteObject)

            doFirst {
                configureSwitchEnvironment()

                val rawFile = rawSprite.get().asFile
                val objectFile = spriteObject.get().asFile
                objectFile.parentFile.mkdirs()
                workingDir(rawFile.parentFile)
                commandLine(
                    aarch64Tool("aarch64-none-elf-objcopy").absolutePath,
                    "-I",
                    "binary",
                    "-O",
                    "elf64-littleaarch64",
                    "-B",
                    "aarch64",
                    rawFile.name,
                    objectFile.absolutePath
                )
            }
        }

        SwitchSpriteAssetBuild(
            asset = asset,
            symbolName = symbolName,
            rawFile = rawSprite,
            objectFile = spriteObject,
            objectTaskName = objectTaskName
        )
    }

    if (spriteAssetBuilds.isNotEmpty()) {
        val generateSpriteManifestTaskName = "generate${taskPrefix}SpriteAssetManifest"
        val compileSpriteManifestTaskName = "compile${taskPrefix}SpriteAssetManifestObject"

        cDefineArgs += "-DKENGINE_SWITCH_SPRITE_ASSETS=1"
        assetObjectFiles += spriteManifestObject
        assetObjectFiles += spriteAssetBuilds.map { it.objectFile }
        assetObjectTaskNames += compileSpriteManifestTaskName
        assetObjectTaskNames += spriteAssetBuilds.map { it.objectTaskName }
        mainGeneratedHeaderFiles += spriteManifestHeader
        mainGeneratedHeaderTaskNames += generateSpriteManifestTaskName

        tasks.register(generateSpriteManifestTaskName) {
            group = "switch"
            description = "Generates the C sprite asset manifest for $displayName."

            inputs.files(spriteAssetBuilds.map { it.asset.source })
            inputs.property(
                "spriteAssets",
                spriteAssetBuilds.map { build ->
                    val asset = build.asset
                    listOf(
                        asset.name,
                        asset.id.get(),
                        asset is KengineNintendoSwitchSpriteSheetAsset,
                        (asset as? KengineNintendoSwitchSpriteSheetAsset)?.tileWidth?.orNull ?: 0,
                        (asset as? KengineNintendoSwitchSpriteSheetAsset)?.tileHeight?.orNull ?: 0,
                        (asset as? KengineNintendoSwitchSpriteSheetAsset)?.columns?.orNull ?: 0
                    ).joinToString(":")
                }
            )
            outputs.files(spriteManifestHeader, spriteManifestSource)

            doLast {
                val headerFile = spriteManifestHeader.get().asFile
                val sourceFile = spriteManifestSource.get().asFile
                headerFile.parentFile.mkdirs()
                sourceFile.parentFile.mkdirs()

                headerFile.writeText(
                    """
                    #pragma once
                    #include <stddef.h>

                    typedef struct {
                        int sprite_id;
                        int width;
                        int height;
                        int tile_width;
                        int tile_height;
                        int columns;
                        const unsigned char* data_start;
                        const unsigned char* data_end;
                    } KengineSwitchSpriteAsset;

                    const KengineSwitchSpriteAsset* kengine_switch_find_sprite_asset(int sprite_id);
                    int kengine_switch_sprite_asset_count(void);
                    """.trimIndent() + "\n"
                )

                val declarations = StringBuilder()
                val entries = StringBuilder()
                spriteAssetBuilds.forEach { build ->
                    val asset = build.asset
                    val imageFile = asset.source.get().asFile
                    val dimensions = switchImageDimensions(imageFile)
                    val tileWidth: Int
                    val tileHeight: Int
                    val columns: Int
                    if (asset is KengineNintendoSwitchSpriteSheetAsset) {
                        tileWidth = asset.tileWidth.orNull
                            ?: throw GradleException("Sprite sheet '${asset.name}' must configure tileWidth.")
                        tileHeight = asset.tileHeight.orNull
                            ?: throw GradleException("Sprite sheet '${asset.name}' must configure tileHeight.")
                        if (tileWidth <= 0 || tileHeight <= 0) {
                            throw GradleException("Sprite sheet '${asset.name}' tile dimensions must be positive.")
                        }
                        if (tileWidth > dimensions.width || tileHeight > dimensions.height) {
                            throw GradleException("Sprite sheet '${asset.name}' tile dimensions exceed image size ${dimensions.width}x${dimensions.height}.")
                        }
                        columns = asset.columns.orNull ?: (dimensions.width / tileWidth).coerceAtLeast(1)
                        if (columns <= 0 || columns * tileWidth > dimensions.width) {
                            throw GradleException("Sprite sheet '${asset.name}' columns must fit within image width ${dimensions.width}.")
                        }
                    } else {
                        tileWidth = 0
                        tileHeight = 0
                        columns = 0
                    }

                    declarations.appendLine("extern const unsigned char _binary_${build.symbolName}_rgba_start[];")
                    declarations.appendLine("extern const unsigned char _binary_${build.symbolName}_rgba_end[];")
                    entries.appendLine(
                        "    { ${stableSpriteId(asset.id.get())}, ${dimensions.width}, ${dimensions.height}, $tileWidth, $tileHeight, $columns, " +
                            "_binary_${build.symbolName}_rgba_start, _binary_${build.symbolName}_rgba_end },"
                    )
                }

                sourceFile.writeText(
                    buildString {
                        appendLine("#include \"kengine_switch_sprite_assets.h\"")
                        appendLine()
                        append(declarations)
                        appendLine()
                        appendLine("static const KengineSwitchSpriteAsset kengine_switch_sprite_assets[] = {")
                        append(entries)
                        appendLine("};")
                        appendLine()
                        appendLine("const KengineSwitchSpriteAsset* kengine_switch_find_sprite_asset(int sprite_id) {")
                        appendLine("    int count = kengine_switch_sprite_asset_count();")
                        appendLine("    for (int index = 0; index < count; ++index) {")
                        appendLine("        if (kengine_switch_sprite_assets[index].sprite_id == sprite_id) {")
                        appendLine("            return &kengine_switch_sprite_assets[index];")
                        appendLine("        }")
                        appendLine("    }")
                        appendLine("    return 0;")
                        appendLine("}")
                        appendLine()
                        appendLine("int kengine_switch_sprite_asset_count(void) {")
                        appendLine("    return (int)(sizeof(kengine_switch_sprite_assets) / sizeof(kengine_switch_sprite_assets[0]));")
                        appendLine("}")
                    }
                )
            }
        }

        tasks.register<Exec>(compileSpriteManifestTaskName) {
            group = "switch"
            description = "Compiles the C sprite asset manifest for $displayName."
            dependsOn(generateSpriteManifestTaskName)

            inputs.files(spriteManifestHeader, spriteManifestSource)
            outputs.file(spriteManifestObject)

            doFirst {
                configureSwitchEnvironment()
                spriteManifestObject.get().asFile.parentFile.mkdirs()
                commandLine(
                    aarch64Tool("aarch64-none-elf-gcc").absolutePath,
                    *switchCFlags.toTypedArray(),
                    "-I${spriteManifestHeader.get().asFile.parentFile.absolutePath}",
                    "-c",
                    spriteManifestSource.get().asFile.absolutePath,
                    "-o",
                    spriteManifestObject.get().asFile.absolutePath
                )
            }
        }
    }

    val soundAssetBuilds = extension.soundAssets.mapIndexed { index, asset ->
        val symbolName = "sound_${index}_${cIdentifier(asset.name)}"
        val assetTaskPrefix = "$taskPrefix${kengineNintendoSwitchTaskPrefix("${index}_${asset.name}")}"
        val soundPcm = gameOutputDir.map { it.file("audio/sfx/$symbolName.pcm") }
        val soundObject = gameOutputDir.map { it.file("obj/${symbolName}_pcm.o") }
        val convertTaskName = "convert${assetTaskPrefix}SoundPcm"
        val objectTaskName = "compile${assetTaskPrefix}SoundObject"
        val soundSource = asset.source.orNull?.asFile
            ?: throw GradleException("${gameProject.path} sound asset '${asset.name}' must configure source.")

        tasks.register<Exec>(convertTaskName) {
            group = "switch"
            description = "Converts sound asset '${asset.name}' for $displayName to raw PCM."

            inputs.file(soundSource)
                .withPathSensitivity(PathSensitivity.RELATIVE)
            inputs.files(asset.extraInputs)
            outputs.file(soundPcm)

            doFirst {
                if (!soundSource.isFile) {
                    throw GradleException("Missing sound asset '${asset.name}' for $displayName: ${soundSource.absolutePath}")
                }

                val pcmFile = soundPcm.get().asFile
                pcmFile.parentFile.mkdirs()
                commandLine(
                    ffmpegExecutable.get(),
                    "-hide_banner",
                    "-loglevel",
                    "error",
                    "-y",
                    "-i",
                    soundSource.absolutePath,
                    "-ac",
                    "2",
                    "-ar",
                    "48000",
                    "-f",
                    "s16le",
                    pcmFile.absolutePath
                )
            }
        }

        tasks.register<Exec>(objectTaskName) {
            group = "switch"
            description = "Embeds sound asset '${asset.name}' for $displayName as a linkable object."
            dependsOn(convertTaskName)

            inputs.file(soundPcm)
            outputs.file(soundObject)

            doFirst {
                configureSwitchEnvironment()

                val pcmFile = soundPcm.get().asFile
                val objectFile = soundObject.get().asFile
                objectFile.parentFile.mkdirs()
                workingDir(pcmFile.parentFile)
                commandLine(
                    aarch64Tool("aarch64-none-elf-objcopy").absolutePath,
                    "-I",
                    "binary",
                    "-O",
                    "elf64-littleaarch64",
                    "-B",
                    "aarch64",
                    pcmFile.name,
                    objectFile.absolutePath
                )
            }
        }

        SwitchSoundAssetBuild(
            asset = asset,
            symbolName = symbolName,
            pcmFile = soundPcm,
            objectFile = soundObject,
            objectTaskName = objectTaskName
        )
    }

    if (soundAssetBuilds.isNotEmpty()) {
        val soundManifestHeader = gameOutputDir.map { it.file("generated/c/kengine_switch_sound_assets.h") }
        val soundManifestSource = gameOutputDir.map { it.file("generated/c/kengine_switch_sound_assets.c") }
        val soundManifestObject = gameOutputDir.map { it.file("obj/kengine_switch_sound_assets.o") }
        val generateSoundManifestTaskName = "generate${taskPrefix}SoundAssetManifest"
        val compileSoundManifestTaskName = "compile${taskPrefix}SoundAssetManifestObject"

        cDefineArgs += "-DKENGINE_SWITCH_SOUND_ASSETS=1"
        assetObjectFiles += soundManifestObject
        assetObjectFiles += soundAssetBuilds.map { it.objectFile }
        assetObjectTaskNames += compileSoundManifestTaskName
        assetObjectTaskNames += soundAssetBuilds.map { it.objectTaskName }
        mainGeneratedHeaderFiles += soundManifestHeader
        mainGeneratedHeaderTaskNames += generateSoundManifestTaskName

        tasks.register(generateSoundManifestTaskName) {
            group = "switch"
            description = "Generates the C sound asset manifest for $displayName."

            inputs.files(soundAssetBuilds.map { it.asset.source })
            inputs.property(
                "soundAssets",
                soundAssetBuilds.joinToString("|") { build ->
                    listOf(build.asset.name, build.asset.id.get()).joinToString(":")
                }
            )
            outputs.files(soundManifestHeader, soundManifestSource)

            doLast {
                val headerFile = soundManifestHeader.get().asFile
                val sourceFile = soundManifestSource.get().asFile
                headerFile.parentFile.mkdirs()
                sourceFile.parentFile.mkdirs()

                headerFile.writeText(
                    """
                    #pragma once
                    #include <stddef.h>

                    typedef struct {
                        int asset_id;
                        const unsigned char* data_start;
                        const unsigned char* data_end;
                    } KengineSwitchSoundAsset;

                    const KengineSwitchSoundAsset* kengine_switch_find_sound_asset(int asset_id);
                    int kengine_switch_sound_asset_count(void);
                    """.trimIndent() + "\n"
                )

                val declarations = StringBuilder()
                val entries = StringBuilder()
                soundAssetBuilds.forEach { build ->
                    declarations.appendLine("extern const unsigned char _binary_${build.symbolName}_pcm_start[];")
                    declarations.appendLine("extern const unsigned char _binary_${build.symbolName}_pcm_end[];")
                    entries.appendLine(
                        "    { ${stableSoundId(build.asset.id.get())}, " +
                            "_binary_${build.symbolName}_pcm_start, _binary_${build.symbolName}_pcm_end },"
                    )
                }

                sourceFile.writeText(
                    buildString {
                        appendLine("#include \"kengine_switch_sound_assets.h\"")
                        appendLine()
                        append(declarations)
                        appendLine()
                        appendLine("static const KengineSwitchSoundAsset kengine_switch_sound_assets[] = {")
                        append(entries)
                        appendLine("};")
                        appendLine()
                        appendLine("const KengineSwitchSoundAsset* kengine_switch_find_sound_asset(int asset_id) {")
                        appendLine("    int count = kengine_switch_sound_asset_count();")
                        appendLine("    for (int index = 0; index < count; ++index) {")
                        appendLine("        if (kengine_switch_sound_assets[index].asset_id == asset_id) {")
                        appendLine("            return &kengine_switch_sound_assets[index];")
                        appendLine("        }")
                        appendLine("    }")
                        appendLine("    return 0;")
                        appendLine("}")
                        appendLine()
                        appendLine("int kengine_switch_sound_asset_count(void) {")
                        appendLine("    return (int)(sizeof(kengine_switch_sound_assets) / sizeof(kengine_switch_sound_assets[0]));")
                        appendLine("}")
                    }
                )
            }
        }

        tasks.register<Exec>(compileSoundManifestTaskName) {
            group = "switch"
            description = "Compiles the C sound asset manifest for $displayName."
            dependsOn(generateSoundManifestTaskName)

            inputs.files(soundManifestHeader, soundManifestSource)
            outputs.file(soundManifestObject)

            doFirst {
                configureSwitchEnvironment()
                soundManifestObject.get().asFile.parentFile.mkdirs()
                commandLine(
                    aarch64Tool("aarch64-none-elf-gcc").absolutePath,
                    *switchCFlags.toTypedArray(),
                    "-I${soundManifestHeader.get().asFile.parentFile.absolutePath}",
                    "-c",
                    soundManifestSource.get().asFile.absolutePath,
                    "-o",
                    soundManifestObject.get().asFile.absolutePath
                )
            }
        }
    }

    extension.musicSource.orNull?.asFile?.let { musicSource ->
        val musicPcm = gameOutputDir.map { it.file("audio/music.pcm") }
        val musicObject = gameOutputDir.map { it.file("obj/music_pcm.o") }
        val convertTaskName = "convert${taskPrefix}MusicPcm"
        val objectTaskName = "compile${taskPrefix}MusicObject"

        cDefineArgs += "-DKENGINE_SWITCH_EMBEDDED_MUSIC=1"
        assetObjectFiles += musicObject
        assetObjectTaskNames += objectTaskName

        tasks.register<Exec>(convertTaskName) {
            group = "switch"
            description = "Converts the music track for $displayName to libnx-compatible raw PCM."

            inputs.file(musicSource)
                .withPathSensitivity(PathSensitivity.RELATIVE)
            outputs.file(musicPcm)

            doFirst {
                if (!musicSource.isFile) {
                    throw GradleException("Missing music source for $displayName: ${musicSource.absolutePath}")
                }

                val pcmFile = musicPcm.get().asFile
                pcmFile.parentFile.mkdirs()
                commandLine(
                    ffmpegExecutable.get(),
                    "-hide_banner",
                    "-loglevel",
                    "error",
                    "-y",
                    "-i",
                    musicSource.absolutePath,
                    "-ac",
                    "2",
                    "-ar",
                    "48000",
                    "-f",
                    "s16le",
                    pcmFile.absolutePath
                )
            }
        }

        tasks.register<Exec>(objectTaskName) {
            group = "switch"
            description = "Embeds the PCM music stream for $displayName as a linkable object."
            dependsOn(convertTaskName)

            inputs.file(musicPcm)
            outputs.file(musicObject)

            doFirst {
                configureSwitchEnvironment()

                val pcmFile = musicPcm.get().asFile
                val objectFile = musicObject.get().asFile
                objectFile.parentFile.mkdirs()
                workingDir(pcmFile.parentFile)
                commandLine(
                    aarch64Tool("aarch64-none-elf-objcopy").absolutePath,
                    "-I",
                    "binary",
                    "-O",
                    "elf64-littleaarch64",
                    "-B",
                    "aarch64",
                    pcmFile.name,
                    objectFile.absolutePath
                )
            }
        }
    }

    val compileMainTaskName = "compile${taskPrefix}Main"
    tasks.register<Exec>(compileMainTaskName) {
        group = "switch"
        description = "Compiles the libnx C shell that calls the generated $displayName Kotlin/Native API."
        dependsOn(listOf(compileKotlinTaskName) + mainGeneratedHeaderTaskNames)

        inputs.file("src/main/c/main.c")
        inputs.file(kotlinApiHeader)
        inputs.files(mainGeneratedHeaderFiles)
        outputs.file(cObject)

        doFirst {
            configureSwitchEnvironment()
            cObject.get().asFile.parentFile.mkdirs()
            val generatedIncludeArgs = mainGeneratedHeaderFiles.map {
                "-I${it.get().asFile.parentFile.absolutePath}"
            }
            commandLine(
                aarch64Tool("aarch64-none-elf-gcc").absolutePath,
                *switchCFlags.toTypedArray(),
                *cDefineArgs.toTypedArray(),
                "-I${libnxInclude().absolutePath}",
                "-I${kotlinApiHeader.get().asFile.parentFile.absolutePath}",
                *generatedIncludeArgs.toTypedArray(),
                "-c",
                file("src/main/c/main.c").absolutePath,
                "-o",
                cObject.get().asFile.absolutePath
            )
        }
    }

    val linkTaskName = "link${taskPrefix}Elf"
    tasks.register<Exec>(linkTaskName) {
        group = "switch"
        description = "Links the $displayName ELF with the Kotlin/Native static library."
        dependsOn(listOf(compileMainTaskName) + assetObjectTaskNames)

        inputs.files(listOf(cObject, kotlinStaticLib) + assetObjectFiles)
        outputs.file(switchElf)

        doFirst {
            configureSwitchEnvironment()
            commandLine(
                aarch64Tool("aarch64-none-elf-gcc").absolutePath,
                "-specs=${switchSpecs().absolutePath}",
                "-g",
                *switchArchFlags.toTypedArray(),
                "-Wl,--gc-sections",
                cObject.get().asFile.absolutePath,
                *assetObjectFiles.map { it.get().asFile.absolutePath }.toTypedArray(),
                kotlinStaticLib.get().asFile.absolutePath,
                "-L${libnxLib().absolutePath}",
                "-lstdc++",
                "-lm",
                "-lnx",
                "-o",
                switchElf.get().asFile.absolutePath
            )
        }
    }

    val createNacpTaskName = "create${taskPrefix}Nacp"
    tasks.register<Exec>(createNacpTaskName) {
        group = "switch"
        description = "Creates NACP metadata for $displayName."

        inputs.property("displayName", displayName)
        inputs.property("author", author)
        inputs.property("version", gameVersion)
        outputs.file(switchNacp)

        doFirst {
            configureSwitchEnvironment()
            switchNacp.get().asFile.parentFile.mkdirs()
            commandLine(
                devkitTool("nacptool").absolutePath,
                "--create",
                displayName,
                author,
                gameVersion,
                switchNacp.get().asFile.absolutePath
            )
        }
    }

    val convertIconTaskName = extension.iconSource.orNull?.asFile?.let { iconSource ->
        val taskName = "convert${taskPrefix}IconJpeg"
        tasks.register<Exec>(taskName) {
            group = "switch"
            description = "Converts the $displayName icon to the NRO icon JPEG format."

            inputs.file(iconSource)
                .withPathSensitivity(PathSensitivity.RELATIVE)
            outputs.file(switchIcon)

            doFirst {
                if (!iconSource.isFile) {
                    throw GradleException("Missing icon source for $displayName: ${iconSource.absolutePath}")
                }

                val iconFile = switchIcon.get().asFile
                iconFile.parentFile.mkdirs()
                commandLine(
                    ffmpegExecutable.get(),
                    "-hide_banner",
                    "-loglevel",
                    "error",
                    "-y",
                    "-i",
                    iconSource.absolutePath,
                    "-vf",
                    "scale=256:256:force_original_aspect_ratio=increase,crop=256:256,format=yuvj420p",
                    "-frames:v",
                    "1",
                    iconFile.absolutePath
                )
            }
        }
        taskName
    }

    val packageTaskName = "package${taskPrefix}Nro"
    tasks.register<Exec>(packageTaskName) {
        group = "switch"
        description = "Packages the $displayName ELF as an NRO."
        dependsOn(listOf(linkTaskName, createNacpTaskName) + listOfNotNull(convertIconTaskName))

        inputs.files(switchElf, switchNacp)
        convertIconTaskName?.let {
            inputs.file(switchIcon)
        }
        outputs.file(switchNro)

        doFirst {
            configureSwitchEnvironment()
            val packageArgs = mutableListOf(
                devkitTool("elf2nro").absolutePath,
                switchElf.get().asFile.absolutePath,
                switchNro.get().asFile.absolutePath,
                "--nacp=${switchNacp.get().asFile.absolutePath}"
            )
            if (convertIconTaskName != null) {
                packageArgs += "--icon=${switchIcon.get().asFile.absolutePath}"
            }
            commandLine(packageArgs)
        }
    }

    tasks.register(buildTaskName) {
        group = "switch"
        description = "Builds the experimental $displayName NRO."
        dependsOn(packageTaskName)
    }

    return SwitchGameRegistration(
        gameProjectPath = gameProject.path,
        artifactBaseName = artifactBaseName,
        displayName = displayName,
        buildTaskName = buildTaskName
    )
}

tasks.register<Exec>("compileSwitchMainCOnly") {
    group = "switch"
    description = "Compiles the libnx hello-world C shell without Kotlin linkage."

    inputs.file("src/main/c/main.c")
    outputs.file(cOnlyObject)

    doFirst {
        configureSwitchEnvironment()
        cOnlyObject.get().asFile.parentFile.mkdirs()
        commandLine(
            aarch64Tool("aarch64-none-elf-gcc").absolutePath,
            *switchCFlags.toTypedArray(),
            "-DKENGINE_SWITCH_C_ONLY=1",
            "-I${libnxInclude().absolutePath}",
            "-c",
            file("src/main/c/main.c").absolutePath,
            "-o",
            cOnlyObject.get().asFile.absolutePath
        )
    }
}

tasks.register<Exec>("linkSwitchCOnlyElf") {
    group = "switch"
    description = "Links the C-only libnx hello-world ELF."
    dependsOn("compileSwitchMainCOnly")

    inputs.file(cOnlyObject)
    outputs.file(switchCOnlyElf)

    doFirst {
        configureSwitchEnvironment()
        commandLine(
            aarch64Tool("aarch64-none-elf-gcc").absolutePath,
            "-specs=${switchSpecs().absolutePath}",
            "-g",
            *switchArchFlags.toTypedArray(),
            "-Wl,--gc-sections",
            cOnlyObject.get().asFile.absolutePath,
            "-L${libnxLib().absolutePath}",
            "-lnx",
            "-o",
            switchCOnlyElf.get().asFile.absolutePath
        )
    }
}

tasks.register<Exec>("createSwitchNacp") {
    group = "switch"
    description = "Creates NACP metadata for the Switch prototype."

    outputs.file(switchNacp)

    doFirst {
        configureSwitchEnvironment()
        switchNacp.get().asFile.parentFile.mkdirs()
        commandLine(
            devkitTool("nacptool").absolutePath,
            "--create",
            "Kengine Nintendo Switch",
            "kengine",
            version.toString(),
            switchNacp.get().asFile.absolutePath
        )
    }
}

tasks.register<Exec>("packageSwitchCOnlyNro") {
    group = "switch"
    description = "Packages the C-only libnx hello-world ELF as an NRO."
    dependsOn("linkSwitchCOnlyElf", "createSwitchNacp")

    inputs.files(switchCOnlyElf, switchNacp)
    outputs.file(switchCOnlyNro)

    doFirst {
        configureSwitchEnvironment()
        commandLine(
            devkitTool("elf2nro").absolutePath,
            switchCOnlyElf.get().asFile.absolutePath,
            switchCOnlyNro.get().asFile.absolutePath,
            "--nacp=${switchNacp.get().asFile.absolutePath}"
        )
    }
}

tasks.register("buildSwitchCOnlyNro") {
    group = "switch"
    description = "Builds the C-only libnx hello-world NRO."
    dependsOn("packageSwitchCOnlyNro")
}

gradle.projectsEvaluated {
    val registrations = rootProject.allprojects
        .mapNotNull { gameProject ->
            switchGameExtension(gameProject)?.let { extension ->
                registerSwitchGameBuild(gameProject, extension)
            }
        }
        .sortedBy { it.artifactBaseName }

    tasks.register("switchGameInfo") {
        group = "switch"
        description = "Prints the Nintendo Switch game projects registered for this build."

        doLast {
            if (registrations.isEmpty()) {
                println("No kengine.nintendo-switch-game projects are registered.")
            } else {
                registrations.forEach { registration ->
                    println("${registration.artifactBaseName}: ${registration.displayName} -> :kengine-nintendo-switch:${registration.buildTaskName}")
                }
            }
        }
    }

    tasks.register("buildSwitchGameNros") {
        group = "switch"
        description = "Builds every registered Nintendo Switch game-facing NRO."
        dependsOn(registrations.map { "${it.gameProjectPath}:buildSwitchNro" })
    }

    val defaultRegistration = registrations.firstOrNull { it.artifactBaseName == "nintendo-switch-demo" }
        ?: registrations.firstOrNull()

    tasks.register("buildSwitchNro") {
        group = "switch"
        description = "Builds the default registered Nintendo Switch game NRO."

        if (defaultRegistration == null) {
            doFirst {
                throw GradleException("No kengine.nintendo-switch-game projects are registered.")
            }
        } else {
            dependsOn(defaultRegistration.buildTaskName)
        }
    }
}
