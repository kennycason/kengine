import java.util.Properties

plugins {
    base
}

group = "kengine.kotlin"
version = libs.versions.kotlin.get()

val localPropertiesFile = layout.projectDirectory.file("local.properties")

fun localProperties(): Properties {
    return Properties().apply {
        val file = localPropertiesFile.asFile
        if (file.isFile) {
            file.inputStream().use(::load)
        }
    }
}

fun localProperty(name: String): String? {
    return localProperties().getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }
}

fun defaultCheckoutDir(): File {
    return rootProject.layout.projectDirectory.asFile.parentFile.resolve("kengine-kotlin-nintendo-switch")
}

tasks.register("kotlinForkInfo") {
    group = "kengine kotlin"
    description = "Prints the local Kotlin compiler fork configuration used by kengine-nintendo-switch."

    doLast {
        val repo = localProperty("kengine.kotlin.repo")?.let(::file) ?: defaultCheckoutDir()
        val nativeHome = localProperty("kengine.switch.kotlinNativeHome")?.let(::file)
            ?: repo.resolve("kotlin-native/dist")
        val kotlincNative = nativeHome.resolve("bin/kotlinc-native")

        println("Kengine Kotlin version: ${libs.versions.kotlin.get()}")
        println("Kotlin fork repo: ${repo.absolutePath} (${repo.resolve(".git").isDirectory})")
        println("Kotlin/Native home: ${nativeHome.absolutePath} (${nativeHome.isDirectory})")
        println("Switch Kotlin/Native home property: kengine.switch.kotlinNativeHome")
        println("kotlinc-native: ${kotlincNative.absolutePath} (${kotlincNative.exists()})")
        println("Switch target override: ${localProperty("kengine.switch.kotlinTarget") ?: "(not configured)"}")
        println("Local properties: ${localPropertiesFile.asFile.absolutePath} (${localPropertiesFile.asFile.isFile})")
    }
}

tasks.register<Exec>("setupKotlinFork") {
    group = "kengine kotlin"
    description = "Clones or updates the local Kotlin fork checkout for Switch target work."

    commandLine("bash", layout.projectDirectory.file("setup-kotlin-fork.sh").asFile.absolutePath)
}

tasks.register<Exec>("buildKotlinNativeDist") {
    group = "kengine kotlin"
    description = "Builds the Kotlin/Native distribution from the configured local Kotlin fork."

    commandLine("bash", layout.projectDirectory.file("build-kotlin-native-dist.sh").asFile.absolutePath)
}

fun defaultN64CheckoutDir(): File {
    return rootProject.layout.projectDirectory.asFile.parentFile.resolve("kengine-kotlin-n64")
}

tasks.register("kotlinN64ForkInfo") {
    group = "kengine kotlin"
    description = "Prints the local Kotlin compiler fork configuration for kengine-n64."

    doLast {
        val n64Repo = localProperty("kengine.n64.kotlinRepo")?.let(::file) ?: defaultN64CheckoutDir()
        val n64NativeHome = localProperty("kengine.n64.kotlinNativeHome")?.let(::file)
            ?: n64Repo.resolve("kotlin-native/dist")
        val n64KotlincNative = n64NativeHome.resolve("bin/kotlinc-native")

        println("Kengine Kotlin version: ${libs.versions.kotlin.get()}")
        println("N64 Kotlin fork repo: ${n64Repo.absolutePath} (${n64Repo.resolve(".git").isDirectory})")
        println("N64 Kotlin/Native home: ${n64NativeHome.absolutePath} (${n64NativeHome.isDirectory})")
        println("N64 Kotlin/Native home property: kengine.n64.kotlinNativeHome")
        println("kotlinc-native: ${n64KotlincNative.absolutePath} (${n64KotlincNative.exists()})")
        println("N64 target override: ${localProperty("kengine.n64.kotlinTarget") ?: "(not configured)"}")
        println("Local properties: ${localPropertiesFile.asFile.absolutePath} (${localPropertiesFile.asFile.isFile})")
    }
}
