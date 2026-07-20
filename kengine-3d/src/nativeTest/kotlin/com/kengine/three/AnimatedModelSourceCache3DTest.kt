package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AnimatedModelSourceCache3DTest {
    @Test
    fun loadReusesAnimatedSourceForTheSamePathTypeAndOptions() {
        var loadCount = 0
        val options = ModelLoadOptions3D(targetSize = 1.6)
        val cache = AnimatedModelSourceCache3D { assetPath, type, loadOptions ->
            loadCount += 1
            testAnimatedSource(assetPath, type, loadOptions)
        }

        val first = cache.load("models/mario.glb", AnimatedModelType3D.SKINNED_TEXTURED_LIT, options)
        val second = cache.load("models/mario.glb", AnimatedModelType3D.SKINNED_TEXTURED_LIT, options)

        assertSame(first, second)
        assertEquals(1, loadCount)
        assertEquals(1, cache.size)
        assertTrue(cache.contains("models/mario.glb", AnimatedModelType3D.SKINNED_TEXTURED_LIT, options))
        assertSame(first, cache.get("models/mario.glb", AnimatedModelType3D.SKINNED_TEXTURED_LIT, options))
    }

    @Test
    fun loadKeepsDifferentAnimatedTypesSeparate() {
        var loadCount = 0
        val cache = AnimatedModelSourceCache3D { assetPath, type, loadOptions ->
            loadCount += 1
            testAnimatedSource(assetPath, type, loadOptions)
        }

        val skinned = cache.load("models/character.glb", AnimatedModelType3D.SKINNED_TEXTURED_LIT)
        val nodeAnimated = cache.load("models/character.glb", AnimatedModelType3D.NODE_ANIMATED_LIT)

        assertFalse(skinned === nodeAnimated)
        assertEquals(2, loadCount)
        assertEquals(2, cache.size)
    }

    @Test
    fun loadKeepsDifferentOptionsSeparate() {
        var loadCount = 0
        val cache = AnimatedModelSourceCache3D { assetPath, type, loadOptions ->
            loadCount += 1
            testAnimatedSource(assetPath, type, loadOptions)
        }

        val small = cache.load(
            assetPath = "models/mario.glb",
            type = AnimatedModelType3D.SKINNED_TEXTURED_LIT,
            options = ModelLoadOptions3D(targetSize = 1.0)
        )
        val large = cache.load(
            assetPath = "models/mario.glb",
            type = AnimatedModelType3D.SKINNED_TEXTURED_LIT,
            options = ModelLoadOptions3D(targetSize = 2.0)
        )

        assertFalse(small === large)
        assertEquals(2, loadCount)
        assertEquals(2, cache.size)
    }

    @Test
    fun clearRemovesCachedAnimatedSources() {
        val cache = AnimatedModelSourceCache3D { assetPath, type, loadOptions ->
            testAnimatedSource(assetPath, type, loadOptions)
        }

        cache.load("models/mario.glb", AnimatedModelType3D.SKINNED_TEXTURED_LIT)
        cache.clear()

        assertEquals(0, cache.size)
        assertFalse(cache.contains("models/mario.glb", AnimatedModelType3D.SKINNED_TEXTURED_LIT))
        assertEquals(null, cache.get("models/mario.glb", AnimatedModelType3D.SKINNED_TEXTURED_LIT))
    }

    @Test
    fun sourceExposesDescriptorAndClips() {
        val source = testAnimatedSource(
            assetPath = "models/mario.glb",
            type = AnimatedModelType3D.SKINNED_TEXTURED_LIT,
            options = ModelLoadOptions3D(targetSize = 1.6)
        )

        assertEquals("models/mario.glb", source.assetPath)
        assertEquals(ModelFormat3D.GLB, source.format)
        assertEquals(AnimatedModelType3D.SKINNED_TEXTURED_LIT, source.type)
        assertEquals(1.6, source.options.targetSize)
        assertEquals(listOf("Idle"), source.clips.names)
        assertEquals(1, source.info.animationCount)
    }

    @Test
    fun cacheKeysRejectBlankAssetPaths() {
        assertFailsWith<IllegalArgumentException> {
            AnimatedModelSourceCacheKey3D("", AnimatedModelType3D.SKINNED_TEXTURED_LIT)
        }
    }

    private fun testAnimatedSource(
        assetPath: String,
        type: AnimatedModelType3D,
        options: ModelLoadOptions3D
    ): AnimatedModelSource3D {
        return AnimatedModelSource3D(
            assetPath = assetPath,
            format = ModelFormat3D.GLB,
            type = type,
            options = options,
            data = TestAnimatedModelSourceData3D(assetPath)
        )
    }

    private class TestAnimatedModelSourceData3D(
        assetPath: String
    ) : AnimatedModelSourceData3D() {
        override val info: ModelInfo3D = ModelInfo3D(
            assetPath = assetPath,
            format = ModelFormat3D.GLB,
            vertexCount = 3,
            animationCount = 1,
            hasAnimations = true,
            animations = listOf(
                AnimationClipInfo3D(
                    name = "Idle",
                    durationSeconds = 1.0,
                    channelCount = 0
                )
            )
        )
        override val clips: AnimationClipSet3D = AnimationClipSet3D(info.animations)
        override val vertexCount: Int = 3

        override fun upload(
            gpu: GpuContext,
            textureCache: GpuTextureCache3D?,
            options: ModelLoadOptions3D
        ): AnimatedModel3D {
            throw UnsupportedOperationException("Test source data is not uploadable.")
        }
    }
}
