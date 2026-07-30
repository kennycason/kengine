package com.kengine.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.getpid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortableFileStorageTest {
    @Test
    fun savesLoadsAndDeletesRecordsFromDisk() {
        val storage = PortableFileStorage(
            namespace = "file-storage-test",
            rootPath = testRootPath()
        )

        assertTrue(storage.saveString("high-score", "1200"))
        assertTrue(storage.exists("high-score"))
        assertEquals("1200", storage.loadString("high-score"))

        assertTrue(storage.saveString("high-score", "2400"))
        assertEquals("2400", storage.loadString("high-score"))

        assertTrue(storage.delete("high-score"))
        assertFalse(storage.exists("high-score"))
        assertNull(storage.loadString("high-score"))
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun testRootPath(): String {
        val tmp = getenv("TMPDIR")?.toKString()?.trimEnd('/')
            ?: getenv("TEMP")?.toKString()?.trimEnd('/')
            ?: "/tmp"
        return "$tmp/kengine-storage-test-${getpid()}"
    }
}
