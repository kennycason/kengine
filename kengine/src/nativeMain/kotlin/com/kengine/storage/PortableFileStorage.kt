package com.kengine.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.F_OK
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.access
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite
import platform.posix.getenv
import platform.posix.mkdir
import platform.posix.remove
import platform.posix.rename

private const val DIRECTORY_MODE = 493

class PortableFileStorage(
    namespace: String,
    private val rootPath: String = defaultPortableStorageRoot(),
    private val maxRecordSize: Int = DEFAULT_PORTABLE_STORAGE_RECORD_LIMIT
) : PortableStorage {
    private val namespace = sanitizePathComponent(namespace.ifBlank { "kengine" })

    override fun load(key: String): ByteArray? {
        PortableStorageKey.requireValid(key)
        val path = pathFor(key)
        val file = fopen(path, "rb") ?: return null
        return try {
            if (fseek(file, 0, SEEK_END) != 0) {
                return null
            }
            val size = ftell(file)
            if (size < 0 || size > maxRecordSize) {
                return null
            }
            if (fseek(file, 0, SEEK_SET) != 0) {
                return null
            }
            val bytes = ByteArray(size.toInt())
            if (bytes.isEmpty()) {
                bytes
            } else {
                val read = bytes.usePinned {
                    fread(it.addressOf(0), 1.convert(), bytes.size.convert(), file)
                }
                if (read.toLong() == size) bytes else null
            }
        } finally {
            fclose(file)
        }
    }

    override fun save(key: String, value: ByteArray): Boolean {
        PortableStorageKey.requireValid(key)
        if (value.size > maxRecordSize || !ensureStorageDirectory()) {
            return false
        }

        val path = pathFor(key)
        val temporaryPath = "$path.tmp"
        val file = fopen(temporaryPath, "wb") ?: return false
        val wrote = try {
            if (value.isEmpty()) {
                true
            } else {
                val written = value.usePinned {
                    fwrite(it.addressOf(0), 1.convert(), value.size.convert(), file)
                }
                written.toLong() == value.size.toLong()
            }
        } finally {
            fflush(file)
            fclose(file)
        }

        if (!wrote) {
            remove(temporaryPath)
            return false
        }

        remove(path)
        if (rename(temporaryPath, path) != 0) {
            remove(temporaryPath)
            return false
        }
        return true
    }

    override fun delete(key: String): Boolean {
        PortableStorageKey.requireValid(key)
        remove(pathFor(key))
        return true
    }

    override fun exists(key: String): Boolean {
        PortableStorageKey.requireValid(key)
        return access(pathFor(key), F_OK) == 0
    }

    private fun pathFor(key: String): String = "${storageDirectory()}/$key.dat"

    private fun storageDirectory(): String = "${rootPath.withoutTrailingPathSeparator()}/$namespace"

    private fun ensureStorageDirectory(): Boolean = ensureDirectory(storageDirectory())
}

@OptIn(ExperimentalForeignApi::class)
fun defaultPortableStorageRoot(): String {
    val home = getenv("HOME")?.toKString()
        ?: getenv("USERPROFILE")?.toKString()
        ?: "."
    return "${home.withoutTrailingPathSeparator()}/.kengine/saves"
}

private fun sanitizePathComponent(value: String): String {
    val sanitized = value.map { char ->
        if (
            char in 'a'..'z' ||
            char in 'A'..'Z' ||
            char in '0'..'9' ||
            char == '.' ||
            char == '_' ||
            char == '-'
        ) {
            char
        } else {
            '_'
        }
    }.joinToString("").trim('.', '_', '-')
    return sanitized.ifEmpty { "kengine" }
}

private fun ensureDirectory(path: String): Boolean {
    val normalized = path.withoutTrailingPathSeparator()
    val startsAtRoot = normalized.startsWith("/")
    val parts = normalized.split('/', '\\').filter { it.isNotEmpty() }
    var current = if (startsAtRoot) "/" else ""

    for (part in parts) {
        current = when {
            current == "/" -> "/$part"
            current.isEmpty() -> part
            else -> "$current/$part"
        }
        mkdir(current, DIRECTORY_MODE.convert())
        if (access(current, F_OK) != 0) {
            return false
        }
    }

    return true
}

private fun String.withoutTrailingPathSeparator(): String {
    val isWindowsRoot = length == 3 && this[1] == ':' && (this[2] == '\\' || this[2] == '/')
    return if (length > 1 && !isWindowsRoot) trimEnd('/', '\\') else this
}
