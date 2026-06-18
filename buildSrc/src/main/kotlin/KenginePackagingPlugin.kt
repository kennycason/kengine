import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import java.io.File

class KenginePackagingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.afterEvaluate {
            val gameName = project.name
            val packagingDir = File(project.rootDir, "packaging")
            val distDir = File(project.buildDir, "dist")

            when {
                PlatformConfig.isMacOS -> registerMacOSTasks(project, gameName, packagingDir, distDir)
                PlatformConfig.isLinux -> registerLinuxTasks(project, gameName, packagingDir, distDir)
                PlatformConfig.isWindows -> registerWindowsTasks(project, gameName, packagingDir, distDir)
            }
        }
    }

    private fun registerMacOSTasks(
        project: Project,
        gameName: String,
        packagingDir: File,
        distDir: File
    ) {
        val appName = gameName.replaceFirstChar { it.uppercase() }
        val appDir = File(distDir, "$appName.app/Contents")

        project.tasks.register("packageMacApp") {
            group = "distribution"
            description = "Create a macOS .app bundle for $gameName"
            dependsOn("linkReleaseExecutable${KengineHostTarget.taskSuffix}")

            doLast {
                val macosDir = File(appDir, "MacOS")
                val resourcesDir = File(appDir, "Resources")
                val frameworksDir = File(appDir, "Frameworks")

                macosDir.mkdirs()
                resourcesDir.mkdirs()
                frameworksDir.mkdirs()

                val executableSrc = File(project.buildDir, KengineHostTarget.binPath("releaseExecutable", "$gameName.kexe"))
                val executableDst = File(macosDir, gameName)
                executableSrc.copyTo(executableDst, overwrite = true)
                executableDst.setExecutable(true)

                val assetsDir = File(project.projectDir, "assets")
                if (assetsDir.exists()) {
                    assetsDir.copyRecursively(File(resourcesDir, "assets"), overwrite = true)
                }

                val sdlDylibCopier = SdlDylibCopier(project)
                val searchPaths = listOf("/opt/homebrew/lib", "/usr/local/lib")
                val dylibs = listOf("SDL3", "SDL3_image", "SDL3_mixer", "SDL3_ttf", "SDL3_net")
                for (lib in dylibs) {
                    for (searchPath in searchPaths) {
                        val candidates = listOf("lib${lib}.0.dylib", "lib${lib}.dylib")
                        for (candidate in candidates) {
                            val src = File("$searchPath/$candidate")
                            if (src.exists()) {
                                src.copyTo(File(frameworksDir, candidate), overwrite = true)
                                break
                            }
                        }
                    }
                }
                val chipmunkSrc = File("/opt/homebrew/lib/libchipmunk.dylib")
                if (chipmunkSrc.exists()) {
                    chipmunkSrc.copyTo(File(frameworksDir, "libchipmunk.dylib"), overwrite = true)
                }

                val icnsFile = File(packagingDir, "kengine.icns")
                if (icnsFile.exists()) {
                    icnsFile.copyTo(File(resourcesDir, "kengine.icns"), overwrite = true)
                }

                val plist = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>$appName</string>
    <key>CFBundleDisplayName</key>
    <string>$appName</string>
    <key>CFBundleIdentifier</key>
    <string>com.kengine.$gameName</string>
    <key>CFBundleVersion</key>
    <string>${project.version}</string>
    <key>CFBundleShortVersionString</key>
    <string>${project.version}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleExecutable</key>
    <string>$gameName</string>
    <key>CFBundleIconFile</key>
    <string>kengine.icns</string>
    <key>LSMinimumSystemVersion</key>
    <string>12.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticGraphicsSwitching</key>
    <true/>
</dict>
</plist>"""
                File(appDir, "Info.plist").writeText(plist)

                println("Created $appName.app at ${distDir.absolutePath}")
            }
        }

        project.tasks.register<Exec>("fixupMacAppDylibs") {
            group = "distribution"
            description = "Fix dylib rpaths in the .app bundle so it is self-contained"
            dependsOn("packageMacApp")

            val frameworksDir = File(appDir, "Frameworks")
            val executablePath = File(appDir, "MacOS/$gameName")

            doFirst {
                val d = "$"
                val script = """
                    |#!/bin/bash
                    |set -e
                    |APP_EXEC='${executablePath.absolutePath}'
                    |FW_DIR='${frameworksDir.absolutePath}'
                    |
                    |install_name_tool -add_rpath @executable_path/../Frameworks "${d}APP_EXEC" 2>/dev/null || true
                    |
                    |for dylib in "${d}FW_DIR"/*.dylib; do
                    |  [ -f "${d}dylib" ] || continue
                    |  bn=${d}(basename "${d}dylib")
                    |  install_name_tool -id "@rpath/${d}bn" "${d}dylib" 2>/dev/null || true
                    |  for other in "${d}FW_DIR"/*.dylib; do
                    |    [ -f "${d}other" ] || continue
                    |    on=${d}(basename "${d}other")
                    |    for prefix in /opt/homebrew/lib /usr/local/lib /opt/homebrew/opt/*/lib; do
                    |      install_name_tool -change "${d}prefix/${d}on" "@rpath/${d}on" "${d}dylib" 2>/dev/null || true
                    |    done
                    |  done
                    |done
                    |
                    |for dylib in "${d}FW_DIR"/*.dylib; do
                    |  [ -f "${d}dylib" ] || continue
                    |  bn=${d}(basename "${d}dylib")
                    |  # Get all current references and fix any absolute paths
                    |  otool -L "${d}dylib" 2>/dev/null | grep -v ":" | awk '{print ${d}1}' | while read ref; do
                    |    case "${d}ref" in
                    |      /opt/homebrew/*|/usr/local/lib/*)
                    |        refbn=${d}(basename "${d}ref")
                    |        install_name_tool -change "${d}ref" "@rpath/${d}refbn" "${d}dylib" 2>/dev/null || true
                    |        ;;
                    |    esac
                    |  done
                    |done
                    |
                    |# Fix executable references
                    |otool -L "${d}APP_EXEC" 2>/dev/null | grep -v ":" | awk '{print ${d}1}' | while read ref; do
                    |  case "${d}ref" in
                    |    /opt/homebrew/*|/usr/local/lib/*)
                    |      refbn=${d}(basename "${d}ref")
                    |      install_name_tool -change "${d}ref" "@rpath/${d}refbn" "${d}APP_EXEC" 2>/dev/null || true
                    |      ;;
                    |  esac
                    |done
                    |
                    |echo 'Dylib fixup complete'
                """.trimMargin()
                val scriptFile = File(project.buildDir, "fixup_dylibs.sh")
                scriptFile.writeText(script)
                scriptFile.setExecutable(true)
            }

            commandLine("bash", File(project.buildDir, "fixup_dylibs.sh").absolutePath)
        }

        project.tasks.register("packageMac") {
            group = "distribution"
            description = "Create a complete, self-contained macOS .app bundle"
            dependsOn("fixupMacAppDylibs")

            doLast {
                println("macOS .app bundle ready: ${File(distDir, "${appName}.app").absolutePath}")
            }
        }
    }

    private fun registerLinuxTasks(
        project: Project,
        gameName: String,
        packagingDir: File,
        distDir: File
    ) {
        project.tasks.register("packageLinux") {
            group = "distribution"
            description = "Create a Linux distributable tarball for $gameName"
            dependsOn("linkReleaseExecutable${KengineHostTarget.taskSuffix}")

            doLast {
                val stageDir = File(distDir, "$gameName-linux")
                val binDir = File(stageDir, "bin")
                val libDir = File(stageDir, "lib")

                binDir.mkdirs()
                libDir.mkdirs()

                val executableSrc = File(project.buildDir, KengineHostTarget.binPath("releaseExecutable", "$gameName.kexe"))
                val executableDst = File(binDir, gameName)
                executableSrc.copyTo(executableDst, overwrite = true)
                executableDst.setExecutable(true)

                val assetsDir = File(project.projectDir, "assets")
                if (assetsDir.exists()) {
                    assetsDir.copyRecursively(File(binDir, "assets"), overwrite = true)
                }

                val soLibs = listOf("SDL3", "SDL3_image", "SDL3_mixer", "SDL3_ttf", "SDL3_net")
                val searchPaths = listOf("/usr/local/lib", "/usr/lib/x86_64-linux-gnu", "/usr/lib")
                for (lib in soLibs) {
                    for (searchPath in searchPaths) {
                        val candidates = listOf("lib${lib}.so.0", "lib${lib}.so")
                        for (candidate in candidates) {
                            val src = File("$searchPath/$candidate")
                            if (src.exists()) {
                                src.copyTo(File(libDir, candidate), overwrite = true)
                                break
                            }
                        }
                    }
                }

                val pngIcon = File(packagingDir, "kengine.png")
                if (pngIcon.exists()) {
                    pngIcon.copyTo(File(stageDir, "kengine.png"), overwrite = true)
                }

                val launchScript = """#!/bin/bash
SCRIPT_DIR="${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)"
export LD_LIBRARY_PATH="${'$'}SCRIPT_DIR/lib:${'$'}LD_LIBRARY_PATH"
exec "${'$'}SCRIPT_DIR/bin/$gameName" "${'$'}@"
"""
                val scriptFile = File(stageDir, "$gameName.sh")
                scriptFile.writeText(launchScript)
                scriptFile.setExecutable(true)

                val desktopEntry = """[Desktop Entry]
Name=${gameName.replaceFirstChar { it.uppercase() }}
Exec=$gameName.sh
Icon=kengine
Type=Application
Categories=Game;
"""
                File(stageDir, "$gameName.desktop").writeText(desktopEntry)

                println("Linux package staged at: ${stageDir.absolutePath}")
                println("Create tarball with: tar -czf $gameName-linux.tar.gz -C ${distDir.absolutePath} $gameName-linux")
            }
        }

        project.tasks.register<Exec>("packageLinuxTarball") {
            group = "distribution"
            description = "Create a Linux .tar.gz distributable"
            dependsOn("packageLinux")

            workingDir(distDir)
            commandLine("tar", "-czf", "$gameName-linux.tar.gz", "$gameName-linux")

            doLast {
                println("Linux tarball ready: ${File(distDir, "$gameName-linux.tar.gz").absolutePath}")
            }
        }
    }

    private fun registerWindowsTasks(
        project: Project,
        gameName: String,
        packagingDir: File,
        distDir: File
    ) {
        project.tasks.register("packageWindows") {
            group = "distribution"
            description = "Create a Windows distributable directory for $gameName"
            dependsOn("linkReleaseExecutable${KengineHostTarget.taskSuffix}")

            doLast {
                val stageDir = File(distDir, "$gameName-windows")
                stageDir.mkdirs()

                val executableSrc = File(project.buildDir, KengineHostTarget.binPath("releaseExecutable", "$gameName.exe"))
                val executableDst = File(stageDir, "$gameName.exe")
                executableSrc.copyTo(executableDst, overwrite = true)

                val assetsDir = File(project.projectDir, "assets")
                if (assetsDir.exists()) {
                    assetsDir.copyRecursively(File(stageDir, "assets"), overwrite = true)
                }

                val msys2Root = System.getenv("MSYS2_ROOT") ?: "C:/msys64"
                val dllSearchPaths = listOf("$msys2Root/mingw64/bin", "$msys2Root/mingw64/lib")
                val dlls = listOf("SDL3", "SDL3_image", "SDL3_mixer", "SDL3_ttf", "SDL3_net")
                for (lib in dlls) {
                    for (searchPath in dllSearchPaths) {
                        val candidates = listOf("${lib}.dll", "lib${lib}.dll")
                        for (candidate in candidates) {
                            val src = File("$searchPath/$candidate")
                            if (src.exists()) {
                                src.copyTo(File(stageDir, candidate), overwrite = true)
                                break
                            }
                        }
                    }
                }

                val pngIcon = File(packagingDir, "kengine.png")
                if (pngIcon.exists()) {
                    pngIcon.copyTo(File(stageDir, "kengine.png"), overwrite = true)
                }

                println("Windows package staged at: ${stageDir.absolutePath}")
            }
        }
    }
}
