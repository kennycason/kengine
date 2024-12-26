package com.kengine.file

import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.F_OK
import platform.posix.access
import platform.posix.getcwd

object File {
    fun pwd(): String {
        val buffer = ByteArray(1024)
        return getcwd(buffer.refTo(0), buffer.size.toULong())?.toKString() ?: "Unknown"
    }
    fun isExist(path: String): Boolean {
        return access(path, F_OK) == 0 // F_OK checks existence only
    }
}
