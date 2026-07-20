package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModelSourceCache3DTest {
    @Test
    fun loadReusesParsedSourceForTheSamePathAndOptions() {
        var loadCount = 0
        val options = ModelLoadOptions3D(targetSize = 4.0)
        val cache = ModelSourceCache3D { assetPath, loadOptions ->
            loadCount += 1
            testSource(assetPath, loadOptions)
        }

        val first = cache.load("models/world.glb", options)
        val second = cache.load("models/world.glb", options)

        assertSame(first, second)
        assertEquals(1, loadCount)
        assertEquals(1, cache.size)
        assertTrue(cache.contains("models/world.glb", options))
        assertSame(first, cache.get("models/world.glb", options))
    }

    @Test
    fun loadKeepsDifferentOptionsSeparate() {
        var loadCount = 0
        val cache = ModelSourceCache3D { assetPath, loadOptions ->
            loadCount += 1
            testSource(assetPath, loadOptions)
        }

        val small = cache.load("models/world.glb", ModelLoadOptions3D(targetSize = 1.0))
        val large = cache.load("models/world.glb", ModelLoadOptions3D(targetSize = 2.0))

        assertFalse(small === large)
        assertEquals(2, loadCount)
        assertEquals(2, cache.size)
    }

    @Test
    fun clearRemovesCachedSources() {
        val cache = ModelSourceCache3D { assetPath, loadOptions ->
            testSource(assetPath, loadOptions)
        }
        val options = ModelLoadOptions3D()

        cache.load("models/world.glb", options)
        cache.clear()

        assertEquals(0, cache.size)
        assertFalse(cache.contains("models/world.glb", options))
        assertEquals(null, cache.get("models/world.glb", options))
    }

    @Test
    fun cacheKeysRejectBlankAssetPaths() {
        assertFailsWith<IllegalArgumentException> {
            ModelSourceCacheKey3D("")
        }
    }

    private fun testSource(
        assetPath: String,
        options: ModelLoadOptions3D
    ): ParsedModel3D {
        val vertices = listOf(
            LitVertex3D(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Color.fromHex("ffffff")),
            LitVertex3D(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Color.fromHex("ffffff")),
            LitVertex3D(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 1.0, 0.0), Color.fromHex("ffffff"))
        )
        return ParsedModel3D(
            assetPath = assetPath,
            format = ModelFormat3D.GLB,
            options = options,
            info = ModelInfo3D(
                assetPath = assetPath,
                format = ModelFormat3D.GLB,
                vertexCount = vertices.size
            ),
            litVertices = vertices
        )
    }
}
