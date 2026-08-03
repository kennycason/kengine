package com.kengine.storage

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortableStorageTest {
    @Test
    fun keysAllowOnlyPortablePathSafeCharacters() {
        assertTrue(PortableStorageKey.isValid("hextris.high-score_01"))
        assertFalse(PortableStorageKey.isValid(""))
        assertFalse(PortableStorageKey.isValid("hextris/high-score"))
        assertFalse(PortableStorageKey.isValid("hextris high score"))
        assertFalse(PortableStorageKey.isValid("x".repeat(PortableStorageKey.MAX_LENGTH + 1)))
    }

    @Test
    fun inMemoryStorageCopiesBytesOnSaveAndLoad() {
        val storage = InMemoryPortableStorage()
        val source = byteArrayOf(1, 2, 3)

        assertTrue(storage.save("record", source))
        source[0] = 9

        val loaded = storage.load("record")
        assertContentEquals(byteArrayOf(1, 2, 3), loaded)

        loaded!![1] = 8
        assertContentEquals(byteArrayOf(1, 2, 3), storage.load("record"))
    }

    @Test
    fun stringHelpersRoundTripUtf8() {
        val storage = InMemoryPortableStorage()

        assertTrue(storage.saveString("greeting", "hello save data"))

        assertTrue(storage.exists("greeting"))
        assertEquals("hello save data", storage.loadString("greeting"))
    }

    @Test
    fun oversizedRecordsAreRejected() {
        val storage = InMemoryPortableStorage(maxRecordSize = 3)

        assertFalse(storage.save("record", byteArrayOf(1, 2, 3, 4)))
        assertNull(storage.load("record"))
    }

    @Test
    fun deleteLeavesRecordMissing() {
        val storage = InMemoryPortableStorage()

        storage.saveString("record", "value")
        assertTrue(storage.delete("record"))

        assertFalse(storage.exists("record"))
        assertNull(storage.loadString("record"))
    }

    @Test
    fun invalidKeysFailFastInTestStorage() {
        val storage = InMemoryPortableStorage()

        assertFailsWith<IllegalArgumentException> {
            storage.saveString("../escape", "no")
        }
    }
}
