import org.gradle.api.GradleException
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.util.Properties

plugins {
    base
}

group = "kengine.nintendo.switch"
version = "0.1.0"

val switchOutputDir = layout.buildDirectory.dir("switch")
val kotlinOutputBase = switchOutputDir.map { it.file("kotlin/kengine_switch_kotlin") }
val kotlinApiHeader = switchOutputDir.map { it.file("kotlin/kengine_switch_kotlin_api.h") }
val kotlinStaticLib = switchOutputDir.map { it.file("kotlin/libkengine_switch_kotlin.a") }
val cObject = switchOutputDir.map { it.file("obj/main.o") }
val cOnlyObject = switchOutputDir.map { it.file("obj/main_c_only.o") }
val switchElf = switchOutputDir.map { it.file("kengine-nintendo-switch.elf") }
val switchCOnlyElf = switchOutputDir.map { it.file("kengine-nintendo-switch-c-only.elf") }
val switchNacp = switchOutputDir.map { it.file("kengine-nintendo-switch.nacp") }
val switchNro = switchOutputDir.map { it.file("kengine-nintendo-switch.nro") }
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

fun configuredValue(propertyName: String, environmentName: String, defaultValue: String): String {
    providers.gradleProperty(propertyName).orNull?.let { return it }
    providers.environmentVariable(environmentName).orNull?.let { return it }
    localProperty(propertyName)?.let { return it }
    return defaultValue
}

val kotlinTarget = providers.provider {
    configuredValue("kengine.switch.kotlinTarget", "KENGINE_SWITCH_KOTLIN_TARGET", "linux_arm64")
}

fun envOrDefault(name: String, defaultValue: String): String {
    return providers.environmentVariable(name).orElse(defaultValue).get()
}

fun kotlincNative(): File {
    providers.gradleProperty("kengine.switch.kotlincNative").orNull?.let { return file(it) }
    providers.environmentVariable("KOTLINC_NATIVE").orNull?.let { return file(it) }
    localProperty("kengine.switch.kotlincNative")?.let { return file(it) }

    providers.gradleProperty("kengine.kotlin.nativeHome").orNull?.let {
        return file(it).resolve("bin/kotlinc-native")
    }
    providers.environmentVariable("KENGINE_KOTLIN_NATIVE_HOME").orNull?.let {
        return file(it).resolve("bin/kotlinc-native")
    }
    localProperty("kengine.kotlin.nativeHome")?.let {
        return file(it).resolve("bin/kotlinc-native")
    }

    val osName = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    val host = when {
        osName == "Mac OS X" && arch == "aarch64" -> "macos-aarch64"
        osName == "Mac OS X" -> "macos-x86_64"
        osName == "Linux" -> "linux-x86_64"
        osName.startsWith("Windows") -> "windows-x86_64"
        else -> throw GradleException("Unsupported Kotlin/Native host OS [$osName].")
    }

    return file("${System.getProperty("user.home")}/.konan/kotlin-native-prebuilt-$host-${libs.versions.kotlin.get()}/bin/kotlinc-native")
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
        val compiler = kotlincNative()
        val devkitProDir = devkitPro()
        val devkitA64Dir = devkitA64()
        println("Kotlin/Native compiler: ${compiler.absolutePath} (${compiler.exists()})")
        println("Kotlin/Native target probe: ${kotlinTarget.get()}")
        println("Kengine Kotlin fork: ${localProperty("kengine.kotlin.repo") ?: "(not configured)"}")
        println("DEVKITPRO: ${devkitProDir.absolutePath} (${devkitProDir.exists()})")
        println("DEVKITA64: ${devkitA64Dir.absolutePath} (${devkitA64Dir.exists()})")
        println("aarch64-none-elf-gcc: ${devkitA64Dir.resolve("bin/aarch64-none-elf-gcc").absolutePath}")
        println("nacptool: ${devkitProDir.resolve("tools/bin/nacptool").absolutePath}")
        println("elf2nro: ${devkitProDir.resolve("tools/bin/elf2nro").absolutePath}")
    }
}

tasks.register<Exec>("compileSwitchKotlinStatic") {
    group = "switch"
    description = "Compiles the Kotlin hello-world probe as a static library for the configured Switch Kotlin/Native target."

    val kotlinSources = fileTree("src/main/kotlin") {
        include("**/*.kt")
    }
    inputs.files(kotlinSources)
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
            "Set -Pkengine.switch.kotlincNative=/path/to/kotlinc-native or KOTLINC_NATIVE."
        )
        kotlinOutputBase.get().asFile.parentFile.mkdirs()
        commandLine(
            compiler.absolutePath,
            "-target",
            kotlinTarget.get(),
            "-produce",
            "static",
            "-output",
            kotlinOutputBase.get().asFile.absolutePath
        )
        args(kotlinSources.files.sortedBy { it.absolutePath }.map { it.absolutePath })
    }
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

tasks.register<Exec>("compileSwitchMain") {
    group = "switch"
    description = "Compiles the libnx C shell that calls the generated Kotlin/Native API."
    dependsOn("compileSwitchKotlinStatic")

    inputs.file("src/main/c/main.c")
    inputs.file(kotlinApiHeader)
    outputs.file(cObject)

    doFirst {
        configureSwitchEnvironment()
        cObject.get().asFile.parentFile.mkdirs()
        commandLine(
            aarch64Tool("aarch64-none-elf-gcc").absolutePath,
            *switchCFlags.toTypedArray(),
            "-I${libnxInclude().absolutePath}",
            "-I${kotlinApiHeader.get().asFile.parentFile.absolutePath}",
            "-c",
            file("src/main/c/main.c").absolutePath,
            "-o",
            cObject.get().asFile.absolutePath
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

tasks.register<Exec>("linkSwitchElf") {
    group = "switch"
    description = "Links the libnx hello-world ELF with the Kotlin/Native static library."
    dependsOn("compileSwitchMain")

    inputs.files(cObject, kotlinStaticLib)
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

tasks.register<Exec>("packageSwitchNro") {
    group = "switch"
    description = "Packages the Kotlin-linked libnx hello-world ELF as an NRO."
    dependsOn("linkSwitchElf", "createSwitchNacp")

    inputs.files(switchElf, switchNacp)
    outputs.file(switchNro)

    doFirst {
        configureSwitchEnvironment()
        commandLine(
            devkitTool("elf2nro").absolutePath,
            switchElf.get().asFile.absolutePath,
            switchNro.get().asFile.absolutePath,
            "--nacp=${switchNacp.get().asFile.absolutePath}"
        )
    }
}

tasks.register("buildSwitchCOnlyNro") {
    group = "switch"
    description = "Builds the C-only libnx hello-world NRO."
    dependsOn("packageSwitchCOnlyNro")
}

tasks.register("buildSwitchNro") {
    group = "switch"
    description = "Builds the experimental Kotlin-linked libnx hello-world NRO."
    dependsOn("packageSwitchNro")
}
