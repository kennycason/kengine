import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.util.Properties
import javax.imageio.ImageIO

plugins {
    base
    idea
}

group = "kengine.nintendo.n64"
version = "0.1.0"

idea {
    module {
        sourceDirs.add(file("src/main/kotlin"))
        sourceDirs.add(file("src/main/c"))
    }
}

val isNintendo64Enabled = providers.gradleProperty("kengine.enableNintendo64")
    .map { it.toBoolean() }
    .orElse(false)
    .get()

val n64OutputDir = layout.buildDirectory.dir("n64")
val cOnlyObject = n64OutputDir.map { it.file("obj/main_c_only.o") }
val n64COnlyElf = n64OutputDir.map { it.file("kengine-n64-c-only.elf") }
val n64COnlyZ64 = n64OutputDir.map { it.file("kengine-n64-c-only.z64") }

val kengineKotlinLocalProperties: Properties by lazy {
    Properties().apply {
        val localProperties = rootProject.file("kengine-kotlin/local.properties")
        if (localProperties.isFile) {
            localProperties.inputStream().use(::load)
        }
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
    configuredValue("kengine.n64.kotlinTarget", "linux_mips32")
}

val ffmpegExecutable = providers.gradleProperty("kengine.n64.ffmpeg")
    .orElse(providers.environmentVariable("FFMPEG"))
    .orElse("ffmpeg")

fun envOrDefault(name: String, defaultValue: String): String {
    return providers.environmentVariable(name).orElse(defaultValue).get()
}

fun configuredKotlincNative(): ConfiguredKotlinNativeCompiler {
    providers.gradleProperty("kengine.n64.kotlinNativeHome").orNull?.let {
        return ConfiguredKotlinNativeCompiler(
            file(it).resolve("bin/kotlinc-native"),
            "Gradle property kengine.n64.kotlinNativeHome"
        )
    }
    localProperty("kengine.n64.kotlinNativeHome")?.let {
        return ConfiguredKotlinNativeCompiler(
            file(it).resolve("bin/kotlinc-native"),
            "kengine-kotlin/local.properties kengine.n64.kotlinNativeHome"
        )
    }

    throw GradleException(
        "N64 Kotlin/Native home is not configured. Set -Pkengine.n64.kotlinNativeHome=/path/to/kotlin-native/dist."
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
                "Configure the N64 fork with -Pkengine.n64.kotlinNativeHome=/path/to/kotlin-native/dist. " +
                "Available targets include: ${targets.take(12).joinToString(", ")}"
        )
    }
}

fun n64Toolchain(): File {
    val n64Inst = envOrDefault("N64_INST", "/opt/libdragon")
    return file(n64Inst)
}

fun konanLlvmDir(): File {
    val konanDir = file(System.getProperty("user.home")).resolve(".konan/dependencies")
    val candidates = konanDir.listFiles()?.filter {
        it.isDirectory && it.name.startsWith("llvm-") && it.name.contains("essentials")
    }?.sortedByDescending { it.name } ?: emptyList()

    return candidates.firstOrNull()
        ?: throw GradleException("No LLVM distribution found in ${konanDir.absolutePath}. Run kotlinc-native once to install.")
}

fun konanLlvmDevDir(): File {
    val konanDir = file(System.getProperty("user.home")).resolve(".konan/dependencies")
    val candidates = konanDir.listFiles()?.filter {
        it.isDirectory && it.name.startsWith("llvm-") && it.name.contains("-dev-")
    }?.sortedByDescending { it.name } ?: emptyList()

    return candidates.firstOrNull()
        ?: throw GradleException("No LLVM dev distribution found in ${konanDir.absolutePath}.")
}

fun llvmClang(): File {
    val llvmDir = konanLlvmDir()
    val clang = llvmDir.resolve("bin").listFiles()
        ?.filter { it.name.matches(Regex("clang-\\d+")) }
        ?.maxByOrNull { it.name }
        ?: throw GradleException("No clang binary found in ${llvmDir.resolve("bin")}")
    return clang
}

fun llvmAr(): File {
    return konanLlvmDir().resolve("bin/llvm-ar")
}

fun tool(path: File, setup: String): File {
    if (!path.exists()) {
        throw GradleException("$setup Missing: ${path.absolutePath}")
    }
    return path
}

fun mips64Tool(name: String): File {
    return n64Toolchain().resolve("bin/$name")
}

fun requireMips64Tool(name: String): File {
    return tool(
        mips64Tool(name),
        "Install libdragon toolchain or set N64_INST."
    )
}

fun Exec.configureN64Environment() {
    applyN64Environment(this::environment)
}

fun applyN64Environment(env: (String, Any) -> Unit) {
    val n64Inst = n64Toolchain().absolutePath

    env("N64_INST", n64Inst)
    env(
        "PATH",
        listOf(
            "$n64Inst/bin",
            System.getenv("PATH").orEmpty()
        ).joinToString(File.pathSeparator)
    )
}

val n64CFlags = listOf(
    "-g",
    "-Wall",
    "-O2",
    "-ffunction-sections",
    "-fdata-sections",
    "-march=vr4300",
    "-mtune=vr4300",
    "-mabi=32",
    "-mfix4300"
)

fun disabledN64Message(): String {
    return "Nintendo 64 backend is disabled. Re-run with -Pkengine.enableNintendo64=true."
}

fun registerDisabledN64Task(name: String, taskDescription: String) {
    tasks.register(name) {
        group = "n64"
        description = "$taskDescription Requires -Pkengine.enableNintendo64=true."

        doFirst {
            throw GradleException(disabledN64Message())
        }
    }
}

fun registerDisabledN64BackendTasks() {
    registerDisabledN64Task(
        "n64ToolchainInfo",
        "Prints the experimental N64 toolchain paths used by this module."
    )
    registerDisabledN64Task(
        "validateN64KotlinToolchain",
        "Validates that the configured N64 Kotlin/Native compiler supports the selected target."
    )
    registerDisabledN64Task(
        "n64GameInfo",
        "Prints the Nintendo 64 game projects registered for this build."
    )
    registerDisabledN64Task(
        "buildN64GameZ64s",
        "Builds every registered Nintendo 64 game-facing Z64 ROM."
    )
    registerDisabledN64Task(
        "buildN64Z64",
        "Builds the default registered Nintendo 64 game Z64 ROM."
    )
}

fun registerN64ToolchainInfoTask() {
    tasks.register("n64ToolchainInfo") {
        group = "n64"
        description = "Prints the experimental N64 toolchain paths used by this module."

        doLast {
            val n64Inst = n64Toolchain()
            println("N64 toolchain (N64_INST): ${n64Inst.absolutePath} (${n64Inst.exists()})")
            println("mips64-elf-gcc: ${n64Inst.resolve("bin/mips64-elf-gcc").absolutePath}")
            println("n64tool: ${n64Inst.resolve("bin/n64tool").absolutePath}")
            println("chksum64: ${n64Inst.resolve("bin/chksum64").absolutePath}")

            try {
                val compilerConfig = configuredKotlincNative()
                val compiler = compilerConfig.executable
                val targets = kotlinNativeTargets(compiler)
                val target = kotlinTarget.get()
                println("Kotlin/Native compiler: ${compiler.absolutePath} (${compiler.exists()})")
                println("Kotlin/Native compiler source: ${compilerConfig.source}")
                println("Kotlin/Native target: $target")
                println("Kotlin/Native target supported: ${if (targets.isEmpty()) "(not probed)" else target in targets}")
            } catch (e: GradleException) {
                println("Kotlin/Native compiler: not configured (${e.message})")
            }

            println("Kengine Kotlin N64 fork: ${localProperty("kengine.n64.kotlinNativeHome") ?: "(not configured)"}")
        }
    }
}

fun registerValidateN64KotlinToolchainTask() {
    tasks.register("validateN64KotlinToolchain") {
        group = "n64"
        description = "Validates that the configured N64 Kotlin/Native compiler supports the selected target."

        doLast {
            val compilerConfig = configuredKotlincNative()
            val compiler = tool(
                compilerConfig.executable,
                "Configure the N64 Kotlin/Native fork with kengine.n64.kotlinNativeHome."
            )
            val target = kotlinTarget.get()
            validateKotlinNativeTarget(compiler, target)
            println("N64 Kotlin/Native toolchain OK: $target via ${compiler.absolutePath}")
        }
    }
}

data class N64GameRegistration(
    val gameProjectPath: String,
    val artifactBaseName: String,
    val displayName: String,
    val buildTaskName: String
)

data class N64SpriteAssetBuild(
    val asset: KengineN64SpriteAsset,
    val symbolName: String,
    val rawFile: Provider<RegularFile>,
    val objectFile: Provider<RegularFile>,
    val objectTaskName: String
)

data class N64SoundAssetBuild(
    val asset: KengineN64SoundAsset,
    val assetKind: String,
    val stableIdPrefix: String,
    val symbolName: String,
    val pcmFile: Provider<RegularFile>,
    val objectFile: Provider<RegularFile>,
    val objectTaskName: String
)

data class N64ImageDimensions(
    val width: Int,
    val height: Int
)

fun n64GameExtension(project: Project): KengineN64GameExtension? {
    return project.extensions.findByName("kengineN64") as? KengineN64GameExtension
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

fun n64ImageDimensions(imageFile: File): N64ImageDimensions {
    ImageIO.read(imageFile)?.let { image ->
        return N64ImageDimensions(image.width, image.height)
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
                return N64ImageDimensions(width, height)
            }
        }
    }

    throw GradleException("Unable to read sprite asset dimensions: ${imageFile.absolutePath}")
}

fun spriteManifestInput(builds: List<N64SpriteAssetBuild>): List<String> {
    return builds.map { build ->
        val asset = build.asset
        listOf(
            asset.name,
            asset.id.get(),
            build.symbolName,
            asset.source.get().asFile.absolutePath,
            if (asset is KengineN64SpriteSheetAsset) asset.tileWidth.orNull?.toString().orEmpty() else "",
            if (asset is KengineN64SpriteSheetAsset) asset.tileHeight.orNull?.toString().orEmpty() else "",
            if (asset is KengineN64SpriteSheetAsset) asset.columns.orNull?.toString().orEmpty() else ""
        ).joinToString("|")
    }
}

fun soundManifestInput(builds: List<N64SoundAssetBuild>): List<String> {
    return builds.map { build ->
        val asset = build.asset
        listOf(
            asset.name,
            build.assetKind,
            build.stableIdPrefix,
            asset.id.get(),
            build.symbolName,
            asset.source.get().asFile.absolutePath
        ).joinToString("|")
    }
}

fun renderGameFactorySource(mainClass: String, artifactBaseName: String): String {
    val factoryClassName = mainClass.substringAfterLast(".")
    val factoryImport = if (mainClass.contains(".")) "import $mainClass\n" else ""

    return """
        package kengine.n64runtime

        import com.kengine.PortableGame
        $factoryImport
        fun createN64PortableGame(): PortableGame = $factoryClassName()
        fun n64GameName(): String = "$artifactBaseName"
    """.trimIndent() + "\n"
}

fun renderSpriteAssetHeader(): String {
    return """
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
        } KengineN64SpriteAsset;

        const KengineN64SpriteAsset* kengine_n64_find_sprite_asset(int sprite_id);
        int kengine_n64_sprite_asset_count(void);
    """.trimIndent() + "\n"
}

fun renderSpriteAssetSource(declarations: String, entries: String): String {
    return buildString {
        appendLine("#include \"kengine_n64_sprite_assets.h\"")
        appendLine()
        append(declarations)
        appendLine()
        appendLine("static const KengineN64SpriteAsset kengine_n64_sprite_assets[] = {")
        append(entries)
        appendLine("};")
        appendLine()
        appendLine("const KengineN64SpriteAsset* kengine_n64_find_sprite_asset(int sprite_id) {")
        appendLine("    int count = kengine_n64_sprite_asset_count();")
        appendLine("    for (int index = 0; index < count; ++index) {")
        appendLine("        if (kengine_n64_sprite_assets[index].sprite_id == sprite_id) {")
        appendLine("            return &kengine_n64_sprite_assets[index];")
        appendLine("        }")
        appendLine("    }")
        appendLine("    return 0;")
        appendLine("}")
        appendLine()
        appendLine("int kengine_n64_sprite_asset_count(void) {")
        appendLine("    return (int)(sizeof(kengine_n64_sprite_assets) / sizeof(kengine_n64_sprite_assets[0]));")
        appendLine("}")
    }
}

fun renderSoundAssetHeader(): String {
    return """
        #pragma once
        #include <stddef.h>

        typedef struct {
            int asset_id;
            const unsigned char* data_start;
            const unsigned char* data_end;
        } KengineN64SoundAsset;

        const KengineN64SoundAsset* kengine_n64_find_sound_asset(int asset_id);
        int kengine_n64_sound_asset_count(void);
    """.trimIndent() + "\n"
}

fun renderSoundAssetSource(declarations: String, entries: String): String {
    return buildString {
        appendLine("#include \"kengine_n64_sound_assets.h\"")
        appendLine()
        append(declarations)
        appendLine()
        appendLine("static const KengineN64SoundAsset kengine_n64_sound_assets[] = {")
        append(entries)
        appendLine("};")
        appendLine()
        appendLine("const KengineN64SoundAsset* kengine_n64_find_sound_asset(int asset_id) {")
        appendLine("    int count = kengine_n64_sound_asset_count();")
        appendLine("    for (int index = 0; index < count; ++index) {")
        appendLine("        if (kengine_n64_sound_assets[index].asset_id == asset_id) {")
        appendLine("            return &kengine_n64_sound_assets[index];")
        appendLine("        }")
        appendLine("    }")
        appendLine("    return 0;")
        appendLine("}")
        appendLine()
        appendLine("int kengine_n64_sound_asset_count(void) {")
        appendLine("    return (int)(sizeof(kengine_n64_sound_assets) / sizeof(kengine_n64_sound_assets[0]));")
        appendLine("}")
    }
}

fun ffmpegRgbaCommand(source: File, output: File): List<String> {
    return listOf(
        ffmpegExecutable.get(),
        "-hide_banner", "-loglevel", "error", "-y",
        "-i", source.absolutePath,
        "-f", "rawvideo", "-pix_fmt", "rgba",
        output.absolutePath
    )
}

fun ffmpegPcmCommand(source: File, output: File): List<String> {
    return listOf(
        ffmpegExecutable.get(),
        "-hide_banner", "-loglevel", "error", "-y",
        "-i", source.absolutePath,
        "-ac", "1",
        "-ar", "22050",
        "-f", "s16be",
        output.absolutePath
    )
}

fun objcopyBinaryCommand(source: File, output: File): List<String> {
    val objcopyTool = konanLlvmDevDir().resolve("bin/llvm-objcopy")
    return listOf(
        objcopyTool.absolutePath,
        "-I", "binary",
        "-O", "elf32-bigmips",
        "-B", "mips",
        source.name,
        output.absolutePath
    )
}

fun registerRgbaConversionTask(
    taskName: String,
    description: String,
    source: File,
    output: Provider<RegularFile>,
    extraInputs: ConfigurableFileCollection
) {
    tasks.register<Exec>(taskName) {
        group = "n64"
        this.description = description

        inputs.file(source)
        if (!extraInputs.isEmpty) {
            inputs.files(extraInputs)
        }
        outputs.file(output)

        doFirst {
            output.get().asFile.parentFile.mkdirs()
        }

        commandLine(ffmpegRgbaCommand(source, output.get().asFile))
    }
}

fun registerPcmConversionTask(
    taskName: String,
    description: String,
    source: File,
    output: Provider<RegularFile>,
    extraInputs: ConfigurableFileCollection
) {
    tasks.register<Exec>(taskName) {
        group = "n64"
        this.description = description

        inputs.file(source)
        if (!extraInputs.isEmpty) {
            inputs.files(extraInputs)
        }
        outputs.file(output)

        doFirst {
            output.get().asFile.parentFile.mkdirs()
        }

        commandLine(ffmpegPcmCommand(source, output.get().asFile))
    }
}

fun registerObjcopyTask(
    taskName: String,
    description: String,
    dependsOnTaskName: String,
    rawFile: Provider<RegularFile>,
    objectFile: Provider<RegularFile>
) {
    tasks.register<Exec>(taskName) {
        group = "n64"
        this.description = description
        dependsOn(dependsOnTaskName)

        inputs.file(rawFile)
        outputs.file(objectFile)

        doFirst {
            objectFile.get().asFile.parentFile.mkdirs()
            workingDir(rawFile.get().asFile.parentFile)
            commandLine(objcopyBinaryCommand(rawFile.get().asFile, objectFile.get().asFile))
        }
    }
}

val n64DockerImage = providers.gradleProperty("kengine.n64.dockerImage")
    .orElse("ghcr.io/dragonminded/libdragon:latest")
    .get()

val n64DockerVolume = providers.gradleProperty("kengine.n64.dockerVolume")
    .orElse("kengine-n64-toolchain")
    .get()

fun registerDockerToolchainTask() {
    tasks.register("ensureN64DockerToolchain") {
        group = "n64"
        description = "Ensures the libdragon Docker toolchain volume is populated."

        outputs.upToDateWhen {
            val result = try {
                val process = ProcessBuilder(
                    "docker", "volume", "inspect", n64DockerVolume
                ).redirectErrorStream(true).start()
                process.inputStream.bufferedReader().readText()
                process.waitFor() == 0
            } catch (_: Exception) { false }
            result
        }

        doLast {
            exec {
                commandLine("docker", "pull", "--platform", "linux/amd64", n64DockerImage)
            }
            exec {
                commandLine("docker", "volume", "create", n64DockerVolume)
            }
            exec {
                commandLine(
                    "docker", "run", "--rm", "--platform", "linux/amd64",
                    "-v", "$n64DockerVolume:/n64_toolchain",
                    n64DockerImage,
                    "make", "-C", "/n64_inst", "install", "tools-install",
                    "DESTDIR=/n64_toolchain", "-j4"
                )
                isIgnoreExitValue = true
            }
            exec {
                commandLine(
                    "docker", "run", "--rm", "--platform", "linux/amd64",
                    "-v", "$n64DockerVolume:/n64_toolchain",
                    n64DockerImage,
                    "bash", "-c",
                    "git clone --depth 1 -b unstable https://github.com/DragonMinded/libdragon.git /tmp/libdragon && " +
                    "cd /tmp/libdragon && N64_INST=/n64_toolchain make -j4 install && " +
                    "N64_INST=/n64_toolchain make tools-install -j4"
                )
                isIgnoreExitValue = true
            }
        }
    }
}

fun registerGameBuildTasks(
    gameProject: Project,
    extension: KengineN64GameExtension
): N64GameRegistration {
    val artifactBaseName = extension.artifactBaseName.get()
    val displayName = extension.displayName.get()
    val mainClass = extension.mainClass.get()
    val taskPrefix = kengineN64TaskPrefix(artifactBaseName)
    val buildTaskName = kengineN64BuildTaskName(artifactBaseName)
    val gameOutputDir = n64OutputDir.map { it.dir("games/$artifactBaseName") }

    val kotlinSources = mutableListOf<File>()
    kotlinSources.add(file("src/main/kotlin"))
    val gameSourceProjects = (listOf(gameProject) + extension.gameSourceProjects)
        .distinctBy { it.path }
    gameSourceProjects.forEach { sourceProject ->
        val commonSrc = sourceProject.file("src/commonMain/kotlin")
        if (commonSrc.isDirectory) {
            kotlinSources.add(commonSrc)
        }
    }
    val portableAssetExtensions = gameSourceProjects.mapNotNull { sourceProject ->
        sourceProject.extensions.findByName("kenginePortableAssets") as? KenginePortableAssetsExtension
    }
    val generatedGameSourceDirs = portableAssetExtensions.mapNotNull { it.generatedSourceDir }
    val generatedAssetTaskPaths = portableAssetExtensions.mapNotNull { it.generateTaskPath }

    val generatedDir = gameOutputDir.map { it.dir("generated") }
    val gameFactoryFile = generatedDir.map { it.file("KengineN64GameFactory.kt") }
    val storageDefFile = generatedDir.map { it.file("kengine_n64_storage.def") }

    tasks.register("generate${taskPrefix}GameFactory") {
        group = "n64"
        description = "Generates the N64 game factory source for $artifactBaseName."

        inputs.property("mainClass", mainClass)
        inputs.property("artifactBaseName", artifactBaseName)
        outputs.file(gameFactoryFile)

        doLast {
            val factorySource = renderGameFactorySource(mainClass, artifactBaseName)
            gameFactoryFile.get().asFile.apply {
                parentFile.mkdirs()
                writeText(factorySource)
            }
        }
    }

    tasks.register("generate${taskPrefix}StorageInterop") {
        group = "n64"
        description = "Generates the storage cinterop definition for $artifactBaseName."

        inputs.file(file("src/main/c/kengine_n64_storage.h"))
        outputs.file(storageDefFile)

        doLast {
            storageDefFile.get().asFile.apply {
                parentFile.mkdirs()
                writeText(
                    """
                    package = kengine.n64host
                    headers = kengine_n64_storage.h
                    headerFilter = kengine_n64_storage.h
                    compilerOpts = -I${file("src/main/c").absolutePath}
                    """.trimIndent() + "\n"
                )
            }
        }
    }

    val spriteBuilds = mutableListOf<N64SpriteAssetBuild>()
    extension.spriteAssets.forEach { spriteAsset ->
        val assetName = spriteAsset.name
        val symbolName = "kengine_n64_sprite_${cIdentifier(assetName)}"
        val rawFile = gameOutputDir.map { it.file("assets/sprites/$symbolName.rgba") }
        val objectFile = gameOutputDir.map { it.file("assets/sprites/$symbolName.o") }
        val convertTaskName = "convert${taskPrefix}Sprite${cIdentifier(assetName).replaceFirstChar { it.uppercase() }}"
        val objcopyTaskName = "objcopy${taskPrefix}Sprite${cIdentifier(assetName).replaceFirstChar { it.uppercase() }}"

        registerRgbaConversionTask(
            convertTaskName,
            "Converts sprite $assetName to raw RGBA for $artifactBaseName.",
            spriteAsset.source.get().asFile,
            rawFile,
            spriteAsset.extraInputs
        )

        registerObjcopyTask(
            objcopyTaskName,
            "Converts sprite $assetName RGBA to MIPS object for $artifactBaseName.",
            convertTaskName,
            rawFile,
            objectFile
        )

        spriteBuilds += N64SpriteAssetBuild(spriteAsset, symbolName, rawFile, objectFile, objcopyTaskName)
    }

    val soundBuilds = mutableListOf<N64SoundAssetBuild>()

    fun registerAudioAssetBuild(audioAsset: KengineN64SoundAsset, assetKind: String, stableIdPrefix: String) {
        val assetName = audioAsset.name
        val assetKindTaskName = assetKind.replaceFirstChar { it.uppercase() }
        val assetTaskName = cIdentifier(assetName).replaceFirstChar { it.uppercase() }
        val symbolName = "kengine_n64_${assetKind}_${cIdentifier(assetName)}"
        val pcmFile = gameOutputDir.map { it.file("assets/sounds/$symbolName.pcm") }
        val objectFile = gameOutputDir.map { it.file("assets/sounds/$symbolName.o") }
        val convertTaskName = "convert${taskPrefix}${assetKindTaskName}${assetTaskName}"
        val objcopyTaskName = "objcopy${taskPrefix}${assetKindTaskName}${assetTaskName}"

        registerPcmConversionTask(
            convertTaskName,
            "Converts $assetKind $assetName to PCM for $artifactBaseName.",
            audioAsset.source.get().asFile,
            pcmFile,
            audioAsset.extraInputs
        )

        registerObjcopyTask(
            objcopyTaskName,
            "Converts $assetKind $assetName PCM to MIPS object for $artifactBaseName.",
            convertTaskName,
            pcmFile,
            objectFile
        )

        soundBuilds += N64SoundAssetBuild(
            audioAsset,
            assetKind,
            stableIdPrefix,
            symbolName,
            pcmFile,
            objectFile,
            objcopyTaskName
        )
    }

    extension.soundAssets.forEach { soundAsset ->
        registerAudioAssetBuild(soundAsset, "sound", "sound:")
    }

    extension.musicAssets.forEach { musicAsset ->
        registerAudioAssetBuild(musicAsset, "music", "music:")
    }

    if (spriteBuilds.isNotEmpty()) {
        val spriteManifestHeader = gameOutputDir.map { it.file("generated/kengine_n64_sprite_assets.h") }
        val spriteManifestSource = gameOutputDir.map { it.file("generated/kengine_n64_sprite_assets.c") }
        val spriteManifestObject = gameOutputDir.map { it.file("obj/kengine_n64_sprite_assets.o") }

        tasks.register("generate${taskPrefix}SpriteAssetManifest") {
            group = "n64"
            description = "Generates the sprite asset manifest for $artifactBaseName."

            spriteBuilds.forEach { dependsOn(it.objectTaskName) }
            inputs.property("spriteAssets", spriteManifestInput(spriteBuilds))
            outputs.file(spriteManifestHeader)
            outputs.file(spriteManifestSource)

            doLast {
                spriteManifestHeader.get().asFile.apply {
                    parentFile.mkdirs()
                    writeText(renderSpriteAssetHeader())
                }

                val declarations = StringBuilder()
                val entries = StringBuilder()

                spriteBuilds.forEach { build ->
                    val sym = build.symbolName
                    declarations.appendLine("extern const unsigned char _binary_${sym.replace('-', '_')}_rgba_start[];")
                    declarations.appendLine("extern const unsigned char _binary_${sym.replace('-', '_')}_rgba_end[];")

                    val asset = build.asset
                    val sourceFile = asset.source.get().asFile
                    val dim = n64ImageDimensions(sourceFile)
                    val tileW = if (asset is KengineN64SpriteSheetAsset) asset.tileWidth.getOrElse(dim.width) else dim.width
                    val tileH = if (asset is KengineN64SpriteSheetAsset) asset.tileHeight.getOrElse(dim.height) else dim.height
                    val cols = if (asset is KengineN64SpriteSheetAsset) asset.columns.getOrElse(dim.width / tileW) else 1
                    val spriteId = stableSpriteId(asset.id.get())

                    entries.appendLine("    { $spriteId, ${dim.width}, ${dim.height}, $tileW, $tileH, $cols, _binary_${sym.replace('-', '_')}_rgba_start, _binary_${sym.replace('-', '_')}_rgba_end },")
                }

                spriteManifestSource.get().asFile.apply {
                    parentFile.mkdirs()
                    writeText(renderSpriteAssetSource(declarations.toString(), entries.toString()))
                }
            }
        }

        tasks.register<Exec>("compile${taskPrefix}SpriteAssetManifest") {
            group = "n64"
            description = "Compiles the sprite asset manifest for $artifactBaseName."
            dependsOn("generate${taskPrefix}SpriteAssetManifest")

            inputs.file(spriteManifestSource)
            outputs.file(spriteManifestObject)

            doFirst {
                spriteManifestObject.get().asFile.parentFile.mkdirs()
                configureN64Environment()
                val n64Inst = n64Toolchain()
                commandLine(
                    buildList {
                        add(mips64Tool("mips64-elf-gcc").absolutePath)
                        addAll(n64CFlags)
                        add("-I${gameOutputDir.get().dir("generated").asFile.absolutePath}")
                        add("-I${n64Inst.resolve("mips64-elf/include")}")
                        add("-c")
                        add(spriteManifestSource.get().asFile.absolutePath)
                        add("-o")
                        add(spriteManifestObject.get().asFile.absolutePath)
                    }
                )
            }
        }
    }

    if (soundBuilds.isNotEmpty()) {
        val soundManifestHeader = gameOutputDir.map { it.file("generated/kengine_n64_sound_assets.h") }
        val soundManifestSource = gameOutputDir.map { it.file("generated/kengine_n64_sound_assets.c") }
        val soundManifestObject = gameOutputDir.map { it.file("obj/kengine_n64_sound_assets.o") }

        tasks.register("generate${taskPrefix}SoundAssetManifest") {
            group = "n64"
            description = "Generates the sound asset manifest for $artifactBaseName."

            soundBuilds.forEach { dependsOn(it.objectTaskName) }
            inputs.property("soundAssets", soundManifestInput(soundBuilds))
            outputs.file(soundManifestHeader)
            outputs.file(soundManifestSource)

            doLast {
                soundManifestHeader.get().asFile.apply {
                    parentFile.mkdirs()
                    writeText(renderSoundAssetHeader())
                }

                val declarations = StringBuilder()
                val entries = StringBuilder()

                soundBuilds.forEach { build ->
                    val sym = build.symbolName
                    declarations.appendLine("extern const unsigned char _binary_${sym.replace('-', '_')}_pcm_start[];")
                    declarations.appendLine("extern const unsigned char _binary_${sym.replace('-', '_')}_pcm_end[];")

                    val audioId = stableAssetId(build.stableIdPrefix, build.asset.id.get())
                    entries.appendLine("    { $audioId, _binary_${sym.replace('-', '_')}_pcm_start, _binary_${sym.replace('-', '_')}_pcm_end },")
                }

                soundManifestSource.get().asFile.apply {
                    parentFile.mkdirs()
                    writeText(renderSoundAssetSource(declarations.toString(), entries.toString()))
                }
            }
        }

        tasks.register<Exec>("compile${taskPrefix}SoundAssetManifest") {
            group = "n64"
            description = "Compiles the sound asset manifest for $artifactBaseName."
            dependsOn("generate${taskPrefix}SoundAssetManifest")

            inputs.file(soundManifestSource)
            outputs.file(soundManifestObject)

            doFirst {
                soundManifestObject.get().asFile.parentFile.mkdirs()
                configureN64Environment()
                val n64Inst = n64Toolchain()
                commandLine(
                    buildList {
                        add(mips64Tool("mips64-elf-gcc").absolutePath)
                        addAll(n64CFlags)
                        add("-I${gameOutputDir.get().dir("generated").asFile.absolutePath}")
                        add("-I${n64Inst.resolve("mips64-elf/include")}")
                        add("-c")
                        add(soundManifestSource.get().asFile.absolutePath)
                        add("-o")
                        add(soundManifestObject.get().asFile.absolutePath)
                    }
                )
            }
        }
    }

    val kotlinOutputBaseName = artifactBaseName.replace('-', '_')
    val kotlinStaticLib = gameOutputDir.map { it.file("lib/lib${kotlinOutputBaseName}.a") }
    val kotlinApiHeader = gameOutputDir.map { it.file("lib/${kotlinOutputBaseName}_api.h") }
    val mathKlib = gameOutputDir.map { it.file("klib/kengine_math.klib") }
    val coreKlib = gameOutputDir.map { it.file("klib/kengine_core.klib") }

    val mathProject = rootProject.findProject(":kengine-math")
    val mathSrcDir = mathProject?.file("src/commonMain/kotlin")
    val coreProject = rootProject.findProject(":kengine-core")
    val coreSrcDir = coreProject?.file("src/commonMain/kotlin")

    if (mathSrcDir != null && mathSrcDir.isDirectory) {
        tasks.register<Exec>("compile${taskPrefix}MathKlib") {
            group = "n64"
            description = "Compiles the kengine-math klib for $artifactBaseName."

            inputs.dir(mathSrcDir).withPathSensitivity(PathSensitivity.RELATIVE)
            outputs.file(mathKlib)

            doFirst {
                mathKlib.get().asFile.parentFile.mkdirs()
                val target = kotlinTarget.get()
                commandLine(
                    buildList {
                        add(kotlincNative().absolutePath)
                        add("-target")
                        add(target)
                        add("-produce")
                        add("library")
                        add("-output")
                        add(mathKlib.get().asFile.absolutePath)
                        mathSrcDir.walkTopDown()
                            .filter { it.isFile && it.extension == "kt" }
                            .forEach { add(it.absolutePath) }
                    }
                )
            }
        }
    }

    if (coreSrcDir != null && coreSrcDir.isDirectory) {
        tasks.register<Exec>("compile${taskPrefix}CoreKlib") {
            group = "n64"
            description = "Compiles the kengine-core klib for $artifactBaseName."
            if (mathSrcDir != null && mathSrcDir.isDirectory) {
                dependsOn("compile${taskPrefix}MathKlib")
            }

            inputs.dir(coreSrcDir).withPathSensitivity(PathSensitivity.RELATIVE)
            if (mathSrcDir != null && mathSrcDir.isDirectory) {
                inputs.file(mathKlib)
            }
            outputs.file(coreKlib)

            doFirst {
                coreKlib.get().asFile.parentFile.mkdirs()
                val target = kotlinTarget.get()
                commandLine(
                    buildList {
                        add(kotlincNative().absolutePath)
                        add("-target")
                        add(target)
                        add("-produce")
                        add("library")
                        add("-output")
                        add(coreKlib.get().asFile.absolutePath)
                        if (mathKlib.get().asFile.exists()) {
                            add("-library")
                            add(mathKlib.get().asFile.absolutePath)
                        }
                        coreSrcDir.walkTopDown()
                            .filter { it.isFile && it.extension == "kt" }
                            .forEach { add(it.absolutePath) }
                    }
                )
            }
        }
    }

    val storageCinteropKlib = gameOutputDir.map { it.file("klib/kengine_n64_storage.klib") }

    tasks.register<Exec>("compile${taskPrefix}StorageCinterop") {
        group = "n64"
        description = "Builds the N64 storage cinterop klib for $artifactBaseName."
        dependsOn("generate${taskPrefix}StorageInterop")

        inputs.file(storageDefFile)
        inputs.file(file("src/main/c/kengine_n64_storage.h"))
        outputs.file(storageCinteropKlib)

        doFirst {
            storageCinteropKlib.get().asFile.parentFile.mkdirs()
            val target = kotlinTarget.get()
            commandLine(
                buildList {
                    add(cinterop().absolutePath)
                    add("-def")
                    add(storageDefFile.get().asFile.absolutePath)
                    add("-target")
                    add(target)
                    add("-o")
                    add(storageCinteropKlib.get().asFile.absolutePath)
                }
            )
        }
    }

    tasks.register<Exec>("compile${taskPrefix}KotlinStatic") {
        group = "n64"
        description = "Compiles the N64 Kotlin static library for $artifactBaseName."
        dependsOn("generate${taskPrefix}GameFactory")
        dependsOn("compile${taskPrefix}StorageCinterop")
        dependsOn(generatedAssetTaskPaths)
        if (mathSrcDir != null && mathSrcDir.isDirectory) {
            dependsOn("compile${taskPrefix}MathKlib")
        }
        if (coreSrcDir != null && coreSrcDir.isDirectory) {
            dependsOn("compile${taskPrefix}CoreKlib")
        }

        kotlinSources.forEach { sourceDir ->
            inputs.dir(sourceDir).withPathSensitivity(PathSensitivity.RELATIVE)
        }
        generatedGameSourceDirs.forEach { generatedSourceDir ->
            inputs.dir(generatedSourceDir)
                .optional()
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
        inputs.file(gameFactoryFile)
        inputs.file(storageCinteropKlib)
        if (mathSrcDir != null && mathSrcDir.isDirectory) {
            inputs.file(mathKlib)
        }
        if (coreSrcDir != null && coreSrcDir.isDirectory) {
            inputs.file(coreKlib)
        }
        outputs.file(kotlinStaticLib)
        outputs.file(kotlinApiHeader)

        doFirst {
            kotlinStaticLib.get().asFile.parentFile.mkdirs()
            val target = kotlinTarget.get()
            commandLine(
                buildList {
                    add(kotlincNative().absolutePath)
                    add("-target")
                    add(target)
                    add("-produce")
                        add("static")
                        add("-output")
                        add(kotlinStaticLib.get().asFile.parentFile.resolve(kotlinOutputBaseName).absolutePath)
                        if (mathKlib.get().asFile.exists()) {
                            add("-library")
                            add(mathKlib.get().asFile.absolutePath)
                        }
                        if (coreKlib.get().asFile.exists()) {
                            add("-library")
                            add(coreKlib.get().asFile.absolutePath)
                    }
                    add("-library")
                    add(storageCinteropKlib.get().asFile.absolutePath)
                    add("-Xbinary=gc=noop")
                    add("-Xbinary=gcSchedulerType=AGGRESSIVE")
                    kotlinSources.forEach { sourceDir ->
                        sourceDir.walkTopDown()
                            .filter { it.isFile && it.extension == "kt" }
                            .forEach { add(it.absolutePath) }
                    }
                    generatedGameSourceDirs.forEach { generatedSourceDir ->
                        val sourceDir = generatedSourceDir.get().asFile
                        if (sourceDir.isDirectory) {
                            sourceDir.walkTopDown()
                                .filter { it.isFile && it.extension == "kt" }
                                .forEach { add(it.absolutePath) }
                        }
                    }
                    add(gameFactoryFile.get().asFile.absolutePath)
                }
            )
        }
    }

    val o32StubSources = listOf(
        file("src/main/c/stubs/kotlin_stubs_o32.c"),
        file("src/main/c/stubs/kotlin_cxx_stubs_o32.cpp"),
        file("src/main/c/stubs/kotlin_stdlib_o32.cpp")
    )
    val o32StubObjects = gameOutputDir.map { dir ->
        listOf(
            dir.file("obj/kotlin_stubs_o32.o"),
            dir.file("obj/kotlin_cxx_stubs_o32.o"),
            dir.file("obj/kotlin_stdlib_o32.o")
        )
    }
    val rebuiltKotlinArchive = gameOutputDir.map { it.file("lib/libkengine_n64_kotlin_full.a") }
    val bridgeHeader = gameOutputDir.map { it.file("lib/kengine_n64_kotlin_bridge.h") }

    tasks.register("compile${taskPrefix}O32Stubs") {
        group = "n64"
        description = "Compiles O32 ABI stubs for $artifactBaseName with LLVM (for Kotlin↔libdragon ABI bridge)."
        dependsOn("compile${taskPrefix}KotlinStatic")

        o32StubSources.forEach { inputs.file(it) }
        outputs.files(o32StubObjects.get().map { it.asFile })

        doLast {
            o32StubObjects.get().first().asFile.parentFile.mkdirs()
            val clang = llvmClang()
            val baseFlags = listOf(
                "-target", "mips-unknown-elf",
                "-march=mips2", "-mabi=o32",
                "-c", "-O2",
                "-ffunction-sections", "-fdata-sections",
                "-fno-pic", "-ffreestanding"
            )
            val cxxFlags = baseFlags + listOf("-fno-exceptions", "-fno-rtti")

            o32StubSources.zip(o32StubObjects.get()) { src, obj ->
                val flags = if (src.extension == "c") baseFlags else {
                    listOf("-x", "c++") + cxxFlags
                }
                exec {
                    commandLine(buildList {
                        add(clang.absolutePath)
                        addAll(flags)
                        add("-o")
                        add(obj.asFile.absolutePath)
                        add(src.absolutePath)
                    })
                }
            }
        }
    }

    tasks.register("rebuild${taskPrefix}KotlinArchive") {
        group = "n64"
        description = "Rebuilds the Kotlin archive with O32 stubs for $artifactBaseName."
        dependsOn("compile${taskPrefix}O32Stubs")

        inputs.file(kotlinStaticLib)
        inputs.files(o32StubObjects.get().map { it.asFile })
        outputs.file(rebuiltKotlinArchive)

        doLast {
            rebuiltKotlinArchive.get().asFile.parentFile.mkdirs()
            val ar = llvmAr()
            val extractDir = temporaryDir.resolve("extract")
            extractDir.mkdirs()

            exec {
                workingDir(extractDir)
                commandLine(ar.absolutePath, "x", kotlinStaticLib.get().asFile.absolutePath)
            }

            val extractedObjects = extractDir.listFiles()?.filter { it.extension == "o" } ?: emptyList()

            exec {
                commandLine(buildList {
                    add(ar.absolutePath)
                    add("rcs")
                    add(rebuiltKotlinArchive.get().asFile.absolutePath)
                    extractedObjects.forEach { add(it.absolutePath) }
                    o32StubObjects.get().forEach { add(it.asFile.absolutePath) }
                })
            }
        }
    }

    tasks.register("generate${taskPrefix}BridgeHeader") {
        group = "n64"
        description = "Generates the C bridge header for $artifactBaseName."
        dependsOn("compile${taskPrefix}KotlinStatic")

        inputs.file(kotlinApiHeader)
        outputs.file(bridgeHeader)

        doLast {
            bridgeHeader.get().asFile.parentFile.mkdirs()
            val apiHeaderName = kotlinApiHeader.get().asFile.name
            bridgeHeader.get().asFile.writeText(
                buildString {
                    appendLine("#pragma once")
                    appendLine("#include \"$apiHeaderName\"")
                    appendLine()
                    appendLine("static inline ${kotlinOutputBaseName}_ExportedSymbols* _kengine_syms(void) {")
                    appendLine("    return ${kotlinOutputBaseName}_symbols();")
                    appendLine("}")
                    appendLine()
                    appendLine("static inline void kengine_n64_kotlin_DisposeString(const char* string) {")
                    appendLine("    if (string) {")
                    appendLine("        _kengine_syms()->DisposeString(string);")
                    appendLine("    }")
                    appendLine("}")
                    appendLine()
                    appendLine("static inline void kengine_n64_kotlin_kengineN64RuntimeStart() {")
                    appendLine("    const char* message = _kengine_syms()->kotlin.root.kengineN64RuntimeStart();")
                    appendLine("    kengine_n64_kotlin_DisposeString(message);")
                    appendLine("}")

                    val functions = listOf(
                        Triple("int", "kengineN64RuntimeUpdate", "int hostFrame, int inputMask"),
                        Triple("int", "kengineN64RuntimeAudio", "int hostFrame"),
                        Triple("int", "kengineN64RuntimeDraw", "int hostFrame, int screenWidth, int screenHeight"),
                        Triple("int", "kengineN64RuntimeStep", "int hostFrame, int inputMask, int screenWidth, int screenHeight"),
                        Triple("int", "kengineN64RuntimeCopyCommands", "void* destination, int maxCommands"),
                        Triple("int", "kengineN64RuntimeCopyAudioCommands", "void* destination, int maxCommands"),
                        Triple("int", "kengineN64RuntimeDroppedRenderCommands", ""),
                        Triple("int", "kengineN64RuntimeDroppedAudioCommands", ""),
                        Triple("const char*", "kengineN64RuntimeCommandText", "int commandIndex"),
                        Triple("const char*", "kengineN64RuntimeCleanup", ""),
                        Triple("const char*", "kengineN64RuntimeSnapshot", "")
                    )

                    for ((returnType, name, params) in functions) {
                        val argNames = params.split(",").map { it.trim().substringAfterLast(" ") }
                            .filter { it.isNotEmpty() }
                        val callArgs = argNames.joinToString(", ")
                        val returnStatement = if (returnType == "void") "" else "return "

                        appendLine()
                        appendLine("static inline $returnType kengine_n64_kotlin_$name($params) {")
                        appendLine("    ${returnStatement}_kengine_syms()->kotlin.root.$name($callArgs);")
                        appendLine("}")
                    }
                }
            )
        }
    }

    val dockerStagingDir = gameOutputDir.map { it.dir("docker-build") }

    tasks.register("stage${taskPrefix}DockerBuild") {
        group = "n64"
        description = "Stages files for Docker-based ROM build for $artifactBaseName."
        dependsOn("rebuild${taskPrefix}KotlinArchive")
        dependsOn("generate${taskPrefix}BridgeHeader")
        if (spriteBuilds.isNotEmpty()) {
            dependsOn("generate${taskPrefix}SpriteAssetManifest")
            spriteBuilds.forEach { dependsOn(it.objectTaskName) }
        }
        if (soundBuilds.isNotEmpty()) {
            dependsOn("generate${taskPrefix}SoundAssetManifest")
            soundBuilds.forEach { dependsOn(it.objectTaskName) }
        }

        inputs.file(rebuiltKotlinArchive)
        inputs.file(bridgeHeader)
        inputs.file(kotlinApiHeader)
        inputs.file(file("src/main/c/main.c"))
        inputs.file(file("src/main/c/kotlin_stubs.c"))
        outputs.dir(dockerStagingDir)

        doLast {
            val staging = dockerStagingDir.get().asFile
            staging.mkdirs()
            staging.resolve("src").mkdirs()
            staging.resolve("kotlin").mkdirs()
            staging.resolve("build").mkdirs()

            file("src/main/c/main.c").copyTo(staging.resolve("src/main.c"), overwrite = true)
            file("src/main/c/kotlin_stubs.c").copyTo(staging.resolve("src/kotlin_stubs.c"), overwrite = true)

            rebuiltKotlinArchive.get().asFile.copyTo(
                staging.resolve("kotlin/lib${kotlinOutputBaseName}.a"), overwrite = true
            )
            kotlinApiHeader.get().asFile.copyTo(
                staging.resolve("kotlin/${kotlinApiHeader.get().asFile.name}"), overwrite = true
            )
            bridgeHeader.get().asFile.copyTo(
                staging.resolve("kotlin/kengine_n64_kotlin_api.h"), overwrite = true
            )

            if (spriteBuilds.isNotEmpty()) {
                val spriteHeader = gameOutputDir.get().dir("generated").file("kengine_n64_sprite_assets.h").asFile
                val spriteSource = gameOutputDir.get().dir("generated").file("kengine_n64_sprite_assets.c").asFile
                if (spriteHeader.exists()) spriteHeader.copyTo(staging.resolve("src/kengine_n64_sprite_assets.h"), overwrite = true)
                if (spriteSource.exists()) spriteSource.copyTo(staging.resolve("src/kengine_n64_sprite_assets.c"), overwrite = true)
                spriteBuilds.forEach { build ->
                    val objFile = build.objectFile.get().asFile
                    if (objFile.exists()) objFile.copyTo(staging.resolve("assets/${objFile.name}"), overwrite = true)
                }
            }

            if (soundBuilds.isNotEmpty()) {
                val soundHeader = gameOutputDir.get().dir("generated").file("kengine_n64_sound_assets.h").asFile
                val soundSource = gameOutputDir.get().dir("generated").file("kengine_n64_sound_assets.c").asFile
                if (soundHeader.exists()) soundHeader.copyTo(staging.resolve("src/kengine_n64_sound_assets.h"), overwrite = true)
                if (soundSource.exists()) soundSource.copyTo(staging.resolve("src/kengine_n64_sound_assets.c"), overwrite = true)
                soundBuilds.forEach { build ->
                    val objFile = build.objectFile.get().asFile
                    if (objFile.exists()) objFile.copyTo(staging.resolve("assets/${objFile.name}"), overwrite = true)
                }
            }

            val worldMeshHeader = gameProject.file("src/main/c/kengine_n64_world_mesh.h")
            if (worldMeshHeader.exists()) {
                worldMeshHeader.copyTo(staging.resolve("src/kengine_n64_world_mesh.h"), overwrite = true)
            }

            val libName = "lib${kotlinOutputBaseName}"
            val spriteObjs = if (spriteBuilds.isNotEmpty()) {
                "\nOBJS += \$(BUILD_DIR)/kengine_n64_sprite_assets.o" +
                spriteBuilds.joinToString("") { "\nOBJS += assets/${it.objectFile.get().asFile.name}" }
            } else ""
            val soundObjs = if (soundBuilds.isNotEmpty()) {
                "\nOBJS += \$(BUILD_DIR)/kengine_n64_sound_assets.o" +
                soundBuilds.joinToString("") { "\nOBJS += assets/${it.objectFile.get().asFile.name}" }
            } else ""
            val spriteDefs = if (spriteBuilds.isNotEmpty()) "\nCFLAGS += -DKENGINE_N64_SPRITE_ASSETS=1" else ""
            val soundDefs = if (soundBuilds.isNotEmpty()) "\nCFLAGS += -DKENGINE_N64_SOUND_ASSETS=1" else ""
            val hasWorldMesh = gameProject.file("src/main/c/kengine_n64_world_mesh.h").exists()
            val worldMeshDefs = if (hasWorldMesh)
                "\nCFLAGS += -DKENGINE_N64_WORLD_MESH=1 -DKENGINE_N64_USE_RDPQ_RENDER=1 -DKENGINE_N64_USE_GL=1" else ""
            val glLibs = ""

            staging.resolve("Makefile").writeText(
                """
                |V=1
                |SOURCE_DIR=src
                |BUILD_DIR=build
                |include ${'$'}(N64_INST)/include/n64.mk
                |
                |all: $artifactBaseName.z64
                |.PHONY: all
                |
                |CFLAGS += -I${'$'}(CURDIR)/kotlin -I${'$'}(CURDIR)/src
                |LDFLAGS += --noinhibit-exec --no-warn-mismatch$spriteDefs$soundDefs$worldMeshDefs$glLibs
                |OBJS = ${'$'}(BUILD_DIR)/main.o ${'$'}(BUILD_DIR)/kotlin_stubs.o$spriteObjs$soundObjs
                |KOTLIN_LIB = ${'$'}(CURDIR)/kotlin/$libName.a
                |
                |$artifactBaseName.z64: N64_ROM_TITLE="$displayName"
                |
                |${'$'}(BUILD_DIR)/$artifactBaseName.elf: ${'$'}(OBJS) ${'$'}(KOTLIN_LIB)
                |
                |clean:
                |${"\t"}rm -f ${'$'}(BUILD_DIR)/* *.z64
                |.PHONY: clean
                |
                |-include ${'$'}(wildcard ${'$'}(BUILD_DIR)/*.d)
                """.trimMargin() + "\n"
            )
        }
    }

    val mainObject = gameOutputDir.map { it.file("obj/main.o") }

    tasks.register<Exec>("compile${taskPrefix}Main") {
        group = "n64"
        description = "Compiles the N64 C host main.c for $artifactBaseName."
        dependsOn("compile${taskPrefix}KotlinStatic")
        if (spriteBuilds.isNotEmpty()) {
            dependsOn("generate${taskPrefix}SpriteAssetManifest")
        }
        if (soundBuilds.isNotEmpty()) {
            dependsOn("generate${taskPrefix}SoundAssetManifest")
        }

        inputs.file(file("src/main/c/main.c"))
        inputs.file(kotlinApiHeader)
        outputs.file(mainObject)

        doFirst {
            mainObject.get().asFile.parentFile.mkdirs()
            configureN64Environment()
            val n64Inst = n64Toolchain()
            commandLine(
                buildList {
                    add(mips64Tool("mips64-elf-gcc").absolutePath)
                    addAll(n64CFlags)
                    add("-I${file("src/main/c")}")
                    add("-I${kotlinStaticLib.get().asFile.parentFile.absolutePath}")
                    add("-I${gameOutputDir.get().dir("generated").asFile.absolutePath}")
                    add("-I${n64Inst.resolve("mips64-elf/include")}")
                    if (spriteBuilds.isNotEmpty()) {
                        add("-DKENGINE_N64_SPRITE_ASSETS=1")
                    }
                    if (soundBuilds.isNotEmpty()) {
                        add("-DKENGINE_N64_SOUND_ASSETS=1")
                    }
                    add("-c")
                    add(file("src/main/c/main.c").absolutePath)
                    add("-o")
                    add(mainObject.get().asFile.absolutePath)
                }
            )
        }
    }

    val gameElf = gameOutputDir.map { it.file("$artifactBaseName.elf") }
    val gameZ64 = gameOutputDir.map { it.file("$artifactBaseName.z64") }

    tasks.register<Exec>("link${taskPrefix}Elf") {
        group = "n64"
        description = "Links the N64 ELF for $artifactBaseName."
        dependsOn("compile${taskPrefix}Main")
        if (spriteBuilds.isNotEmpty()) {
            dependsOn("compile${taskPrefix}SpriteAssetManifest")
            spriteBuilds.forEach { dependsOn(it.objectTaskName) }
        }
        if (soundBuilds.isNotEmpty()) {
            dependsOn("compile${taskPrefix}SoundAssetManifest")
            soundBuilds.forEach { dependsOn(it.objectTaskName) }
        }

        inputs.file(mainObject)
        inputs.file(kotlinStaticLib)
        outputs.file(gameElf)

        doFirst {
            gameElf.get().asFile.parentFile.mkdirs()
            configureN64Environment()
            val n64Inst = n64Toolchain()
            commandLine(
                buildList {
                    add(mips64Tool("mips64-elf-gcc").absolutePath)
                    add("-march=vr4300")
                    add("-mtune=vr4300")
                    add("-mabi=32")
                    add("-T${n64Inst.resolve("mips64-elf/lib/n64.ld")}")
                    add(mainObject.get().asFile.absolutePath)
                    if (spriteBuilds.isNotEmpty()) {
                        add(gameOutputDir.get().file("obj/kengine_n64_sprite_assets.o").asFile.absolutePath)
                        spriteBuilds.forEach {
                            add(it.objectFile.get().asFile.absolutePath)
                        }
                    }
                    if (soundBuilds.isNotEmpty()) {
                        add(gameOutputDir.get().file("obj/kengine_n64_sound_assets.o").asFile.absolutePath)
                        soundBuilds.forEach {
                            add(it.objectFile.get().asFile.absolutePath)
                        }
                    }
                    add(kotlinStaticLib.get().asFile.absolutePath)
                    add("-L${n64Inst.resolve("mips64-elf/lib")}")
                    add("-ldragon")
                    add("-lc")
                    add("-lm")
                    add("-ldragonsys")
                    add("-o")
                    add(gameElf.get().asFile.absolutePath)
                }
            )
        }
    }

    tasks.register<Exec>(buildTaskName) {
        group = "n64"
        description = "Builds the Nintendo 64 Z64 ROM for $artifactBaseName."
        dependsOn("link${taskPrefix}Elf")

        inputs.file(gameElf)
        outputs.file(gameZ64)

        doFirst {
            gameZ64.get().asFile.parentFile.mkdirs()
            configureN64Environment()
            val n64Inst = n64Toolchain()
            commandLine(
                buildList {
                    add(mips64Tool("n64tool").absolutePath)
                    add("--header")
                    add(n64Inst.resolve("mips64-elf/lib/header").absolutePath)
                    add("--title")
                    add(displayName)
                    add("-o")
                    add(gameZ64.get().asFile.absolutePath)
                    add(gameElf.get().asFile.absolutePath)
                }
            )
        }
    }

    tasks.named(buildTaskName) {
        doLast {
            exec {
                applyN64Environment(this::environment)
                commandLine(
                    mips64Tool("chksum64").absolutePath,
                    gameZ64.get().asFile.absolutePath
                )
            }
        }
    }

    val dockerGameZ64 = gameOutputDir.map { it.file("$artifactBaseName.z64") }
    val dockerBuildTaskName = kengineN64DockerBuildTaskName(artifactBaseName)

    tasks.register<Exec>(dockerBuildTaskName) {
        group = "n64"
        description = "Builds the N64 Z64 ROM for $artifactBaseName via Docker (recommended)."
        dependsOn("ensureN64DockerToolchain")
        dependsOn("stage${taskPrefix}DockerBuild")

        val staging = dockerStagingDir.get().asFile
        inputs.dir(dockerStagingDir)
            .withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.file(dockerGameZ64)

        doFirst {
            exec {
                commandLine(
                    "docker", "run", "--rm", "--platform", "linux/amd64",
                    "-v", "$n64DockerVolume:/n64_toolchain",
                    "-v", "${staging.absolutePath}:/build",
                    "-w", "/build",
                    "-e", "N64_INST=/n64_toolchain",
                    n64DockerImage,
                    "make", "clean"
                )
                isIgnoreExitValue = true
            }
        }

        commandLine(
            "docker", "run", "--rm", "--platform", "linux/amd64",
            "-v", "$n64DockerVolume:/n64_toolchain",
            "-v", "${staging.absolutePath}:/build",
            "-w", "/build",
            "-e", "N64_INST=/n64_toolchain",
            n64DockerImage,
            "make", "-j4"
        )

        doLast {
            val builtRom = staging.resolve("$artifactBaseName.z64")
            if (builtRom.exists()) {
                builtRom.copyTo(dockerGameZ64.get().asFile, overwrite = true)
            }
        }
    }

    tasks.register<Exec>("run${taskPrefix}") {
        group = "n64"
        description = "Builds and launches the $artifactBaseName ROM in ares."
        dependsOn(dockerBuildTaskName)

        doFirst {
            val rom = dockerGameZ64.get().asFile
            if (!rom.exists()) {
                throw GradleException("ROM not found: ${rom.absolutePath}. Run $dockerBuildTaskName first.")
            }
        }

        commandLine("open", "-a", "ares", dockerGameZ64.get().asFile.absolutePath)
    }

    return N64GameRegistration(
        gameProjectPath = gameProject.path,
        artifactBaseName = artifactBaseName,
        displayName = displayName,
        buildTaskName = buildTaskName
    )
}

fun registerGameDiscoveryTasks() {
    val registeredGames = mutableListOf<N64GameRegistration>()

    rootProject.allprojects.forEach { gameProject ->
        val extension = n64GameExtension(gameProject) ?: return@forEach
        if (!extension.mainClass.isPresent) return@forEach

        val registration = registerGameBuildTasks(gameProject, extension)
        registeredGames += registration
    }

    tasks.register("n64GameInfo") {
        group = "n64"
        description = "Prints the Nintendo 64 game projects registered for this build."

        doLast {
            if (registeredGames.isEmpty()) {
                println("No Nintendo 64 game projects registered.")
                return@doLast
            }
            registeredGames.forEach { game ->
                println("${game.gameProjectPath}: ${game.displayName} (${game.artifactBaseName}) -> ${game.buildTaskName}")
            }
        }
    }

    tasks.register("buildN64GameZ64s") {
        group = "n64"
        description = "Builds every registered Nintendo 64 game-facing Z64 ROM."

        registeredGames.forEach { game ->
            dependsOn(game.buildTaskName)
        }
    }

    if (registeredGames.size == 1) {
        tasks.register("buildN64Z64") {
            group = "n64"
            description = "Builds the default registered Nintendo 64 game Z64 ROM."
            dependsOn(registeredGames.first().buildTaskName)
        }
    } else if (registeredGames.size > 1) {
        tasks.register("buildN64Z64") {
            group = "n64"
            description = "Builds all registered Nintendo 64 game Z64 ROMs."
            dependsOn("buildN64GameZ64s")
        }
    }
}

if (isNintendo64Enabled) {
    registerN64ToolchainInfoTask()
    registerValidateN64KotlinToolchainTask()
    registerDockerToolchainTask()

    gradle.projectsEvaluated {
        registerGameDiscoveryTasks()
    }
} else {
    registerDisabledN64BackendTasks()
}
