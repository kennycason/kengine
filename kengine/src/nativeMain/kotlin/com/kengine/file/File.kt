package com.kengine.file

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import sdl3.SDL_GetCurrentDirectory
import sdl3.SDL_free
import platform.posix.F_OK
import platform.posix.access

object File {
    private val cachedAssetBasePath: String by lazy { resolveAssetBasePath() }

    @OptIn(ExperimentalForeignApi::class)
    fun pwd(): String {
        val currentDirectory = SDL_GetCurrentDirectory() ?: return "Unknown"
        return try {
            currentDirectory.toKString().withoutTrailingPathSeparator()
        } finally {
            SDL_free(currentDirectory)
        }
    }

    fun isExist(path: String): Boolean {
        return access(path, F_OK) == 0
    }

    fun assetBasePath(): String = cachedAssetBasePath

    private fun resolveAssetBasePath(): String {
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

    private fun String.withoutTrailingPathSeparator(): String {
        val isWindowsRoot = length == 3 && this[1] == ':' && (this[2] == '\\' || this[2] == '/')
        return if (length > 1 && !isWindowsRoot) trimEnd('/', '\\') else this
    }
}
