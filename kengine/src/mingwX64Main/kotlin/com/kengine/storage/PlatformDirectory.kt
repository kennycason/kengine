package com.kengine.storage

import platform.posix.mkdir

internal actual fun createDirectory(path: String) {
    mkdir(path)
}
