package com.kengine.three

import com.kengine.file.File

internal object ModelResourcePath3D {
    fun parentDirectory(path: String): String {
        val index = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        return if (index >= 0) path.substring(0, index) else "."
    }

    fun resolveSiblingPath(
        filePath: String,
        path: String,
        decodeUriPath: Boolean = false,
        stripFragment: Boolean = false,
        resolveRootRelativeFromParent: Boolean = false
    ): String {
        val normalizedPath = normalizePathReference(
            path = path,
            decodeUriPath = decodeUriPath,
            stripFragment = stripFragment
        )
        if (isAbsolutePath(normalizedPath)) {
            if (!resolveRootRelativeFromParent ||
                isWindowsAbsolutePath(normalizedPath) ||
                File.isExist(normalizedPath)
            ) {
                return normalizedPath
            }
        }

        val parent = parentDirectory(filePath)
        val siblingPath = if (resolveRootRelativeFromParent) {
            normalizedPath.trimStart('/', '\\')
        } else {
            normalizedPath
        }
        if (parent.endsWith("/") || parent.endsWith("\\")) {
            return parent + siblingPath
        }
        return "$parent/$siblingPath"
    }

    fun requireExistingFile(
        path: String,
        description: String,
        referencedBy: String? = null
    ): String {
        if (!File.isExist(path)) {
            throw IllegalArgumentException(
                "$description was not found: $path${referencedBy.suffixReferencedBy()}"
            )
        }
        return path
    }

    fun cannotOpenFileMessage(
        path: String,
        description: String,
        referencedBy: String? = null
    ): String {
        return "$description could not be opened: $path${referencedBy.suffixReferencedBy()}"
    }

    private fun normalizePathReference(
        path: String,
        decodeUriPath: Boolean,
        stripFragment: Boolean
    ): String {
        val unfragmented = if (stripFragment) path.substringBefore('#') else path
        return if (decodeUriPath) decodeUriPath(unfragmented) else unfragmented
    }

    private fun isAbsolutePath(path: String): Boolean {
        return path.startsWith("/") || isWindowsAbsolutePath(path)
    }

    private fun isWindowsAbsolutePath(path: String): Boolean {
        return path.length > 2 && path[1] == ':' && (path[2] == '\\' || path[2] == '/')
    }

    private fun decodeUriPath(uri: String): String {
        val result = StringBuilder()
        var index = 0
        while (index < uri.length) {
            val char = uri[index]
            if (char == '%' && index + 2 < uri.length) {
                val value = uri.substring(index + 1, index + 3).toIntOrNull(16)
                if (value != null) {
                    result.append(value.toChar())
                    index += 3
                    continue
                }
            }
            result.append(char)
            index += 1
        }
        return result.toString()
    }

    private fun String?.suffixReferencedBy(): String {
        return if (this == null) "" else " referenced by $this"
    }
}
