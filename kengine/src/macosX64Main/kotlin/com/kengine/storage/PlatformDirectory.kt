package com.kengine.storage

import kotlinx.cinterop.convert
import platform.posix.mkdir

private const val DIRECTORY_MODE = 493

internal actual fun createDirectory(path: String) {
    mkdir(path, DIRECTORY_MODE.convert())
}
