import org.gradle.api.Plugin
import org.gradle.api.Project
//import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.create
//import org.gradle.kotlin.dsl.dependencies
//import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
//import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class KengineNativePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Add configuration
        val config = project.extensions.create<KengineNativeConfig>("kengineNative")

        project.afterEvaluate {
            // Apply configuration for Kotlin Multiplatform
            project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
//                project.extensions.configure<KotlinMultiplatformExtension>("kotlin") {
//                    configureKotlinNative(this, config, project)
//                }
            }
        }
    }
//
//    private fun configureKotlinNative(
//        kotlin: KotlinMultiplatformExtension,
//        config: KengineNativeConfig,
//        project: Project
//    ) {
//        val hostOs = System.getProperty("os.name")
//        val isArm64 = System.getProperty("os.arch") == "aarch64"
//
//        // Detect native target
//        val nativeTarget = when {
//            hostOs == "Mac OS X" && isArm64 -> kotlin.macosArm64("native")
//            hostOs == "Mac OS X" && !isArm64 -> kotlin.macosX64("native")
//            hostOs == "Linux" && isArm64 -> kotlin.linuxArm64("native")
//            hostOs == "Linux" && !isArm64 -> kotlin.linuxX64("native")
//            else -> throw GradleException("Host OS [$hostOs] is not supported in Kotlin/Native.")
//        }
//
//        // Configure target
//        nativeTarget.configureTarget(config)
//
//        // Configure common source sets
//        kotlin.sourceSets.getByName("nativeMain") {
//            dependencies {
//                implementation(project(":kengine"))
//            }
//        }
//
//        kotlin.sourceSets.getByName("nativeTest") {
//            dependencies {
//                implementation(kotlin("test"))
//            }
//        }
//    }
//
//    private fun KotlinNativeTarget.configureTarget(config: KengineNativeConfig) {
//        binaries {
//            executable {
//                entryPoint = config.entryPoint
//                linkerOpts(buildLinkerOpts(config.modules))
//            }
//        }
//
//        compilations.all {
//            compileTaskProvider.configure {
//                compilerOptions {
//                    freeCompilerArgs.addAll(
//                        listOf(
//                            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
//                            "-opt-in=kotlin.ExperimentalStdlibApi",
//                            "-g",
//                            "-ea"
//                        )
//                    )
//                }
//            }
//        }
//    }

    private fun buildLinkerOpts(modules: Set<KengineModule>): List<String> {
        val baseOpts = listOf(
            "-L/usr/local/lib",
            "-L/opt/homebrew/lib",
            "-lSDL3"
        )

        val moduleOpts = modules.flatMap { module ->
            when (module) {
                KengineModule.GRAPHICS -> listOf("-lSDL3_image")
                KengineModule.SOUND -> listOf("-lSDL3_mixer")
                KengineModule.NET -> listOf("-lSDL3_net")
                KengineModule.TTF -> listOf("-lSDL3_ttf")
                KengineModule.PHYSICS -> listOf("-lchipmunk")
            }
        }

        val rpathOpts = listOf(
            "-Wl,-rpath,@executable_path/Frameworks",
            "-Wl,-rpath,/usr/local/lib",
            "-Wl,-rpath,/opt/homebrew/lib"
        )

        return baseOpts + moduleOpts + rpathOpts
    }
}
