package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ModelAsset3DTest {
    @Test
    fun resolvesPackagedAssetsFirst() {
        val resolver = ModelAssetPathResolver3D(
            packagedAssetRoot = "assets",
            sourceAssetRoot = "games/demo/assets",
            resolveAssetPath = { "/project/$it" },
            assetExists = { it == "/project/assets/models/world.glb" || it == "/project/games/demo/assets/models/world.glb" }
        )

        assertEquals("/project/assets/models/world.glb", resolver.resolve("models/world.glb"))
    }

    @Test
    fun fallsBackToSourceAssetsWhenPackagedAssetIsMissing() {
        val resolver = ModelAssetPathResolver3D(
            packagedAssetRoot = "assets",
            sourceAssetRoot = "games/demo/assets",
            resolveAssetPath = { "/project/$it" },
            assetExists = { it == "/project/games/demo/assets/models/world.glb" }
        )

        assertEquals("/project/games/demo/assets/models/world.glb", resolver.resolve("models/world.glb"))
    }

    @Test
    fun returnsPackagedPathWhenAssetDoesNotExistYet() {
        val resolver = ModelAssetPathResolver3D(
            packagedAssetRoot = "assets",
            sourceAssetRoot = "games/demo/assets",
            resolveAssetPath = { "/project/$it" },
            assetExists = { false }
        )

        assertEquals("/project/assets/models/world.glb", resolver.resolve("models/world.glb"))
    }

    @Test
    fun doesNotDuplicatePackagedRoot() {
        val resolver = ModelAssetPathResolver3D(
            packagedAssetRoot = "assets",
            resolveAssetPath = { "/project/$it" },
            assetExists = { false }
        )

        assertEquals("/project/assets/models/world.glb", resolver.resolve("assets/models/world.glb"))
    }

    @Test
    fun resolvesModelAssetDescriptors() {
        val resolver = ModelAssetPathResolver3D(
            packagedAssetRoot = "assets",
            resolveAssetPath = { "/project/$it" },
            assetExists = { false }
        )
        val asset = ModelAsset3D("models/ship.OBJ")

        val resolved = resolver.resolve(asset)

        assertEquals(asset, resolved.asset)
        assertEquals("/project/assets/models/ship.OBJ", resolved.assetPath)
        assertEquals(ModelFormat3D.OBJ, resolved.format)
        assertEquals(ModelFormat3D.OBJ, asset.format)
    }

    @Test
    fun resolvesGltfModelAssetDescriptors() {
        val resolver = ModelAssetPathResolver3D(
            packagedAssetRoot = "assets",
            resolveAssetPath = { "/project/$it" },
            assetExists = { false }
        )
        val asset = ModelAsset3D("models/world.GLTF")

        val resolved = resolver.resolve(asset)

        assertEquals(asset, resolved.asset)
        assertEquals("/project/assets/models/world.GLTF", resolved.assetPath)
        assertEquals(ModelFormat3D.GLTF, resolved.format)
        assertEquals(ModelFormat3D.GLTF, asset.format)
    }

    @Test
    fun createsAnimatedModelAssetDescriptors() {
        val skinned = AnimatedModelAsset3D.skinnedTexturedLit("models/mario.glb")
        val nodeAnimated = AnimatedModelAsset3D.nodeAnimatedLit("models/goomba.glb")

        assertEquals(AnimatedModelType3D.SKINNED_TEXTURED_LIT, skinned.type)
        assertEquals(AnimatedModelType3D.NODE_ANIMATED_LIT, nodeAnimated.type)
        assertEquals(ModelFormat3D.GLB, skinned.format)
        assertEquals(ModelFormat3D.GLB, nodeAnimated.format)
        assertEquals(AnimatedModelSkinningMode3D.AUTO, skinned.options.animatedSkinningMode)
    }

    @Test
    fun animatedModelAssetDescriptorsCanSelectGpuSkinning() {
        val skinned = AnimatedModelAsset3D.skinnedTexturedLit(
            relativePath = "models/mario.glb",
            options = ModelLoadOptions3D(
                animatedSkinningMode = AnimatedModelSkinningMode3D.GPU_JOINT_PALETTE
            )
        )

        assertEquals(AnimatedModelSkinningMode3D.GPU_JOINT_PALETTE, skinned.options.animatedSkinningMode)
    }

    @Test
    fun autoSkinningUsesGpuWhenJointCountFitsPalette() {
        val resolved = resolveAnimatedModelSkinningMode3D(
            skinningMode = AnimatedModelSkinningMode3D.AUTO,
            maxSkinJointCount = SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS
        )

        assertEquals(ResolvedAnimatedModelSkinningMode3D.GPU_JOINT_PALETTE, resolved)
    }

    @Test
    fun autoSkinningFallsBackToCpuWhenJointCountExceedsPalette() {
        val resolved = resolveAnimatedModelSkinningMode3D(
            skinningMode = AnimatedModelSkinningMode3D.AUTO,
            maxSkinJointCount = SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS + 1
        )

        assertEquals(ResolvedAnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER, resolved)
    }

    @Test
    fun explicitSkinningModesArePreserved() {
        assertEquals(
            ResolvedAnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER,
            resolveAnimatedModelSkinningMode3D(
                skinningMode = AnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER,
                maxSkinJointCount = 1
            )
        )
        assertEquals(
            ResolvedAnimatedModelSkinningMode3D.GPU_JOINT_PALETTE,
            resolveAnimatedModelSkinningMode3D(
                skinningMode = AnimatedModelSkinningMode3D.GPU_JOINT_PALETTE,
                maxSkinJointCount = SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS + 1
            )
        )
    }

    @Test
    fun bundlePreloadsStaticAndAnimatedSourcesThroughCaches() {
        var staticLoadCount = 0
        var animatedLoadCount = 0
        val world = ModelAsset3D(
            relativePath = "models/world.glb",
            options = ModelLoadOptions3D(targetSize = 12.0)
        )
        val mario = AnimatedModelAsset3D.skinnedTexturedLit(
            relativePath = "models/mario.glb",
            options = ModelLoadOptions3D(targetSize = 1.6)
        )
        val sourceLoader = ModelAssetSourceLoader3D(
            resolver = ModelAssetPathResolver3D(
                packagedAssetRoot = "assets",
                resolveAssetPath = { "/project/$it" },
                assetExists = { false }
            ),
            sourceCache = ModelSourceCache3D { assetPath, options ->
                staticLoadCount += 1
                testParsedSource(assetPath, options)
            },
            animatedSourceCache = AnimatedModelSourceCache3D { assetPath, type, options ->
                animatedLoadCount += 1
                testAnimatedSource(assetPath, type, options)
            }
        )
        val bundle = ModelAssetBundle3D(
            models = listOf(world),
            animatedModels = listOf(mario)
        )

        val first = bundle.preloadSources(sourceLoader)
        val second = bundle.preloadSources(sourceLoader)

        assertEquals(2, bundle.size)
        assertEquals(2, first.size)
        assertEquals("/project/assets/models/world.glb", first.modelSource(world).assetPath)
        assertEquals("/project/assets/models/mario.glb", first.animatedModelSource(mario).assetPath)
        assertEquals(12.0, first.modelSource(world).options.targetSize)
        assertEquals(AnimatedModelType3D.SKINNED_TEXTURED_LIT, first.animatedModelSource(mario).type)
        assertSame(first.modelSource(world), second.modelSource(world))
        assertSame(first.animatedModelSource(mario), second.animatedModelSource(mario))
        assertEquals(1, staticLoadCount)
        assertEquals(1, animatedLoadCount)
    }

    @Test
    fun bundleRejectsDuplicateAssets() {
        val world = ModelAsset3D("models/world.glb")
        val mario = AnimatedModelAsset3D.skinnedTexturedLit("models/mario.glb")

        assertFailsWith<IllegalArgumentException> {
            ModelAssetBundle3D(models = listOf(world, world))
        }
        assertFailsWith<IllegalArgumentException> {
            ModelAssetBundle3D(animatedModels = listOf(mario, mario))
        }
    }

    @Test
    fun preloadedSourceBundleReportsMissingAssets() {
        val world = ModelAsset3D("models/world.glb")
        val bowser = ModelAsset3D("models/bowser.glb")
        val sourceLoader = ModelAssetSourceLoader3D(
            resolver = ModelAssetPathResolver3D(
                resolveAssetPath = { "/project/$it" },
                assetExists = { false }
            ),
            sourceCache = ModelSourceCache3D { assetPath, options ->
                testParsedSource(assetPath, options)
            }
        )

        val sources = ModelAssetBundle3D(models = listOf(world)).preloadSources(sourceLoader)

        assertFailsWith<IllegalArgumentException> {
            sources.modelSource(bowser)
        }
    }

    @Test
    fun preloadedSourceBundleCreatesCollidersFromStaticModelAsset() {
        val world = ModelAsset3D("models/world.glb")
        val sourceLoader = ModelAssetSourceLoader3D(
            resolver = ModelAssetPathResolver3D(
                resolveAssetPath = { "/project/$it" },
                assetExists = { false }
            ),
            sourceCache = ModelSourceCache3D { assetPath, options ->
                testParsedSource(assetPath, options)
            }
        )
        val sources = ModelAssetBundle3D(models = listOf(world)).preloadSources(sourceLoader)

        val terrain = sources.createTerrainCollider(world)
        val staticCollider = sources.createStaticCollider(world)

        assertEquals(null, terrain.groundYAt(2.0, 2.0))
        assertFalse(
            staticCollider.resolveCapsule(
                position = Vec3(2.0, 2.0, 2.0),
                halfHeight = 0.5,
                radius = 0.25
            ).collided
        )
    }

    private fun testParsedSource(
        assetPath: String,
        options: ModelLoadOptions3D
    ): ParsedModel3D {
        val vertices = listOf(
            LitVertex3D(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Color.white),
            LitVertex3D(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Color.white),
            LitVertex3D(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 1.0, 0.0), Color.white)
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
