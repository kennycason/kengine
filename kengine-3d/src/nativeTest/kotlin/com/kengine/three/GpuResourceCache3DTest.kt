package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GpuResourceCache3DTest {
    @Test
    fun getOrPutLoadsOnlyOnceForTheSameKey() {
        var loadCount = 0
        val cache = GpuResourceCache3D<String, TestResource> {
        }

        val first = cache.getOrPut("player") {
            loadCount += 1
            TestResource("mario")
        }
        val second = cache.getOrPut("player") {
            loadCount += 1
            TestResource("luigi")
        }

        assertTrue(first === second)
        assertEquals("mario", second.name)
        assertEquals(1, loadCount)
        assertEquals(1, cache.size)
        assertTrue(cache.containsKey("player"))
    }

    @Test
    fun cleanupReleasesCachedResourcesInReverseInsertionOrder() {
        val cleaned = mutableListOf<String>()
        val cache = GpuResourceCache3D<String, TestResource> { resource ->
            cleaned += resource.name
        }

        cache.getOrPut("a") { TestResource("first") }
        cache.getOrPut("b") { TestResource("second") }

        cache.cleanup()
        cache.cleanup()

        assertEquals(listOf("second", "first"), cleaned)
        assertEquals(0, cache.size)
    }

    @Test
    fun getOrPutRejectsLoadsAfterCleanup() {
        val cache = GpuResourceCache3D<String, TestResource> {
        }

        cache.cleanup()

        assertFailsWith<IllegalStateException> {
            cache.getOrPut("late") { TestResource("late") }
        }
    }

    private class TestResource(
        val name: String
    )
}
