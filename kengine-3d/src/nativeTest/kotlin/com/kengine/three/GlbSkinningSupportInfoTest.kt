package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlbSkinningSupportInfoTest {
    @Test
    fun reportsSupportedPrimitives() {
        val support = GlbSkinningSupportInfo(
            skinnedNodeCount = 1,
            supportedPrimitiveCount = 2
        )

        assertTrue(support.hasSupportedPrimitives)
        assertEquals(0, support.unsupportedPrimitiveCount)
    }

    @Test
    fun summarizesUnsupportedSkinnedInputs() {
        val support = GlbSkinningSupportInfo(
            skinnedNodeCount = 0,
            supportedPrimitiveCount = 0,
            nonTrianglePrimitiveCount = 1,
            missingJointOrWeightPrimitiveCount = 2,
            missingMeshReferenceCount = 3,
            missingSkinReferenceCount = 4
        )

        assertFalse(support.hasSupportedPrimitives)
        assertEquals(10, support.unsupportedPrimitiveCount)
        assertEquals(
            "no mesh nodes reference a skin; " +
                "1 skinned primitive(s) are not triangle lists; " +
                "2 triangle primitive(s) are missing JOINTS_0 or WEIGHTS_0; " +
                "3 skinned node(s) reference missing meshes; " +
                "4 skinned node(s) reference missing skins",
            support.unsupportedSummary()
        )
    }
}
