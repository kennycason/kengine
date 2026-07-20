package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertEquals

class GpuResourceOwnership3DTest {
    @Test
    fun cleanupIfOwnedRunsOnlyForOwnedResources() {
        val cleaned = mutableListOf<String>()

        GpuResourceOwnership3D.OWNED.cleanupIfOwned("owned") { cleaned += it }
        GpuResourceOwnership3D.BORROWED.cleanupIfOwned("borrowed") { cleaned += it }
        GpuResourceOwnership3D.OWNED.cleanupIfOwned<String>(null) { cleaned += it }

        assertEquals(listOf("owned"), cleaned)
    }
}
