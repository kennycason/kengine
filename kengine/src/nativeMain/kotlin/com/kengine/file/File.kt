package com.kengine.file

import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.F_OK
import platform.posix.access
import platform.posix.getcwd
import platform.posix.readlink

object File {
    private val cachedAssetBasePath: String by lazy { resolveAssetBasePath() }

    fun pwd(): String {
        val buffer = ByteArray(1024)
        return getcwd(buffer.refTo(0), buffer.size.convert())?.toKString() ?: "Unknown"
    }

    fun isExist(path: String): Boolean {
        return access(path, F_OK) == 0
    }

    fun assetBasePath(): String = cachedAssetBasePath

    private fun executablePath(): String? {
        // Linux: /proc/self/exe
        val buffer = ByteArray(4096)
        val len = readlink("/proc/self/exe", buffer.refTo(0), buffer.size.convert())
        if (len > 0) {
            return buffer.decodeToString(0, len.toInt())
        }
        return null
    }

    private fun resolveAssetBasePath(): String {
        // macOS .app bundle detection:
        // Check if the executable lives inside a .app/Contents/MacOS/ directory.
        // We check both the resolved executable path and the CWD.
        val execPath = executablePath()
        val execDir = execPath?.substringBeforeLast('/')

        if (execDir != null && execDir.contains(".app/Contents/MacOS")) {
            val resourcesPath = execDir.replace("/Contents/MacOS", "/Contents/Resources")
            if (isExist(resourcesPath)) {
                return resourcesPath
            }
        }

        val cwd = pwd()

        // Check CWD (covers Finder launch)
        if (cwd.contains(".app/Contents/MacOS")) {
            val resourcesPath = cwd.replace("/Contents/MacOS", "/Contents/Resources")
            if (isExist(resourcesPath)) {
                return resourcesPath
            }
        }

        // macOS: check if a .app Resources dir exists relative to CWD/../Resources
        // This handles the case where the binary is inside a .app but CWD is elsewhere.
        // We use the _ env var which bash/zsh set to the invoked command path.
        val invokedPath = platform.posix.getenv("_")?.toKString()
        if (invokedPath != null && invokedPath.contains(".app/Contents/MacOS")) {
            val resourcesPath = invokedPath.substringBeforeLast('/').replace("/Contents/MacOS", "/Contents/Resources")
            if (isExist(resourcesPath)) {
                return resourcesPath
            }
        }

        // Linux AppImage
        val appDir = platform.posix.getenv("APPDIR")?.toKString()
        if (appDir != null && isExist("$appDir/assets")) {
            return appDir
        }

        return cwd
    }

    fun resolveAssetPath(relativePath: String): String {
        if (relativePath.startsWith("/")) return relativePath
        return "${assetBasePath()}/$relativePath"
    }
}
