package com.kengine.three

import com.kengine.math.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Scene3DTest {
    @Test
    fun keepsItemsInSubmissionOrder() {
        val scene = Scene3D()
        val first = FakeSceneItem3D("first")
        val second = FakeSceneItem3D("second")

        scene.add(first)
        scene.add(second)

        assertEquals(listOf(first, second), scene.items)
    }

    @Test
    fun visibleItemsExcludeHiddenItems() {
        val scene = Scene3D()
        val visible = scene.add(FakeSceneItem3D("visible"))
        val hidden = scene.add(FakeSceneItem3D("hidden", isVisible = false))

        assertEquals(listOf(visible), scene.visibleItems)

        hidden.isVisible = true

        assertEquals(listOf(visible, hidden), scene.visibleItems)
    }

    @Test
    fun removeAndClearItems() {
        val scene = Scene3D()
        val first = scene.add(FakeSceneItem3D("first"))
        val second = scene.add(FakeSceneItem3D("second"))

        assertTrue(scene.remove(first))
        assertFalse(scene.remove(first))
        assertEquals(listOf(second), scene.items)

        scene.clear()

        assertEquals(emptyList(), scene.items)
    }

    @Test
    fun addsAnimatedModelsWithPose() {
        val scene = Scene3D()
        val model = FakeAnimatedModel3D()

        val item = scene.addAnimatedModel(
            model = model,
            pose = AnimationPose3D(clipIndex = 2, timeSeconds = 1.25)
        )

        assertEquals(model, item.model)
        assertEquals(2, item.pose.clipIndex)
        assertEquals(1.25, item.pose.timeSeconds)
        assertEquals(listOf(item), scene.items)
    }

    @Test
    fun prepareForDrawUpdatesExplicitAnimatedModelInstances() {
        val scene = Scene3D()
        val model = FakeAnimatedModel3D(AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE)
        val pose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.75)

        scene.addAnimatedModel(model, pose = pose)
        scene.prepareForDraw()

        assertEquals(listOf(pose), model.updatedPoses)
    }

    @Test
    fun prepareForDrawSkipsHiddenAnimatedModelInstances() {
        val scene = Scene3D()
        val model = FakeAnimatedModel3D(AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE)

        scene.addAnimatedModel(
            model = model,
            pose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.75),
            isVisible = false
        )
        scene.prepareForDraw()

        assertEquals(emptyList(), model.updatedPoses)
    }

    @Test
    fun prepareForDrawRejectsConflictingSharedExplicitAnimatedModelPoses() {
        val scene = Scene3D()
        val model = FakeAnimatedModel3D(AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE)

        scene.addAnimatedModel(model, pose = AnimationPose3D(clipIndex = 0, timeSeconds = 0.0))
        scene.addAnimatedModel(model, pose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.5))

        assertFailsWith<IllegalStateException> {
            scene.prepareForDraw()
        }
    }

    @Test
    fun prepareForDrawAllowsConflictingPerInstanceExplicitAnimatedModelPoses() {
        val scene = Scene3D()
        val model = FakePerInstanceAnimatedModel3D()
        val firstPose = AnimationPose3D(clipIndex = 0, timeSeconds = 0.0)
        val secondPose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.5)

        scene.addAnimatedModel(model, pose = firstPose)
        scene.addAnimatedModel(model, pose = secondPose)
        scene.prepareForDraw()

        assertEquals(listOf(listOf(firstPose), listOf(secondPose)), model.instanceStates.map { it.updatedPoses })
    }

    @Test
    fun prepareForDrawAllowsSharedPerDrawAnimatedModelPoses() {
        val scene = Scene3D()
        val model = FakeAnimatedModel3D(AnimatedModelPoseUpdateMode3D.PER_DRAW)

        scene.addAnimatedModel(model, pose = AnimationPose3D(clipIndex = 0, timeSeconds = 0.0))
        scene.addAnimatedModel(model, pose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.5))
        scene.prepareForDraw()

        assertEquals(emptyList(), model.updatedPoses)
    }

    @Test
    fun nodeAppliesTransformToWrappedItemBeforePrepare() {
        val scene = Scene3D()
        val item = FakeSceneItem3D("child")
        val node = scene.addNode(
            Node3D(
                item = item,
                transform = Transform3D(position = Vec3(10.0, 0.0, 0.0)),
                itemTransform = Transform3D(position = Vec3(2.0, 0.0, 0.0))
            )
        )

        scene.prepareForDraw()

        assertEquals(node, scene.items.single())
        assertEquals(1, item.prepareCount)
        assertEquals(Vec3(12.0, 0.0, 0.0), item.transform.position)
    }

    @Test
    fun prepareForDrawSkipsHiddenNodes() {
        val scene = Scene3D()
        val item = FakeSceneItem3D("child")

        scene.addNode(Node3D(item = item, isVisible = false))
        scene.prepareForDraw()

        assertEquals(0, item.prepareCount)
    }

    @Test
    fun nodeFluentSettersUpdateVisibilityAndTransform() {
        val item = FakeSceneItem3D("child")
        val node = Node3D(item = item)

        val returned = node
            .setVisible(false)
            .setPositionYaw(
                position = Vec3(1.0, 2.0, 3.0),
                yawRadians = 0.5,
                yOffset = -0.5,
                yawOffsetRadians = 1.0,
                scale = Vec3(2.0, 2.0, 2.0)
            )

        assertEquals(node, returned)
        assertEquals(false, node.isVisible)
        assertEquals(Vec3(1.0, 1.5, 3.0), node.transform.position)
        assertEquals(1.5, node.transform.rotation.y)
        assertEquals(Vec3(2.0, 2.0, 2.0), node.transform.scale)
    }

    @Test
    fun nodeFactoryAddsAnimatedModelNode() {
        val scene = Scene3D()
        val model = FakeAnimatedModel3D()

        val node = scene.addAnimatedModelNode(
            model = model,
            transform = Transform3D(position = Vec3(1.0, 2.0, 3.0)),
            pose = AnimationPose3D(clipIndex = 1, timeSeconds = 2.0)
        )

        assertEquals(model, node.item.model)
        assertEquals(Vec3(1.0, 2.0, 3.0), node.transform.position)
        assertEquals(1, node.item.pose.clipIndex)
        assertEquals(2.0, node.item.pose.timeSeconds)
        assertEquals(listOf(node), scene.items)
    }

    @Test
    fun assetNodeFactoryAddsAnimatedModelNodeFromLoadedBundle() {
        val scene = Scene3D()
        val asset = AnimatedModelAsset3D.nodeAnimatedLit("models/goomba.glb")
        val model = FakeAnimatedModel3D()
        val bundle = LoadedModelAssetBundle3D(
            sources = ModelAssetSourceBundle3D(
                modelSources = emptyMap(),
                animatedModelSources = emptyMap()
            ),
            models = emptyMap(),
            animatedModels = mapOf(asset to model)
        )

        val node = scene.addAnimatedModelAssetNode(
            bundle = bundle,
            asset = asset,
            transform = Transform3D(position = Vec3(1.0, 2.0, 3.0)),
            pose = AnimationPose3D(clipIndex = 0, timeSeconds = 1.25),
            isVisible = false
        )

        assertEquals(model, node.item.model)
        assertEquals(Vec3(1.0, 2.0, 3.0), node.transform.position)
        assertEquals(1.25, node.item.pose.timeSeconds)
        assertEquals(false, node.isVisible)
        assertEquals(listOf(node), scene.items)
    }

    @Test
    fun animatedModelNodeSetsPoseFluently() {
        val scene = Scene3D()
        val model = FakeAnimatedModel3D()
        val node = scene.addAnimatedModelNode(model)

        val returned = node.setPose(AnimationPose3D(clipIndex = 0, timeSeconds = 1.25))

        assertEquals(node, returned)
        assertEquals(1.25, node.item.pose.timeSeconds)
    }

    @Test
    fun prepareForDrawRejectsConflictingSharedExplicitAnimatedModelPosesInsideNodes() {
        val scene = Scene3D()
        val model = FakeAnimatedModel3D(AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE)

        scene.addAnimatedModelNode(model, pose = AnimationPose3D(clipIndex = 0, timeSeconds = 0.0))
        scene.addAnimatedModelNode(model, pose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.5))

        assertFailsWith<IllegalStateException> {
            scene.prepareForDraw()
        }
    }

    @Test
    fun cleanupCleansAnimatedModelInstanceStatesInsideNodes() {
        val scene = Scene3D()
        val model = FakePerInstanceAnimatedModel3D()

        scene.addAnimatedModel(model)
        scene.addAnimatedModelNode(model)
        scene.cleanup()

        assertEquals(2, model.instanceStates.size)
        assertTrue(model.instanceStates.all { it.cleanedUp })
        assertEquals(emptyList(), scene.items)
        assertFailsWith<IllegalStateException> {
            scene.add(FakeSceneItem3D("after cleanup"))
        }
    }
}

private data class FakeSceneItem3D(
    val name: String,
    override var transform: Transform3D = Transform3D(),
    override var isVisible: Boolean = true
) : SceneItem3D {
    var prepareCount = 0

    override fun prepareForDraw() {
        prepareCount += 1
    }

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    ) {
        error("FakeSceneItem3D is only used for Scene3D collection tests.")
    }
}

private class FakeAnimatedModel3D(
    override val poseUpdateMode: AnimatedModelPoseUpdateMode3D = AnimatedModelPoseUpdateMode3D.PER_DRAW
) : AnimatedModel3D {
    override val clips: AnimationClipSet3D = AnimationClipSet3D.fromNames(listOf("Idle"))
    val updatedPoses = mutableListOf<AnimationPose3D>()

    override fun updatePose(pose: AnimationPose3D) {
        updatedPoses += pose
    }

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D,
        pose: AnimationPose3D
    ) {
        error("FakeAnimatedModel3D is only used for Scene3D collection tests.")
    }

    override fun cleanup() {
    }
}

private class FakePerInstanceAnimatedModel3D : AnimatedModel3D {
    override val clips: AnimationClipSet3D = AnimationClipSet3D.fromNames(listOf("Idle", "Run"))
    override val poseUpdateMode: AnimatedModelPoseUpdateMode3D = AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE
    override val poseUpdateScope: AnimatedModelPoseUpdateScope3D = AnimatedModelPoseUpdateScope3D.INSTANCE
    val instanceStates = mutableListOf<FakeAnimatedModelInstanceRenderState3D>()

    override fun createInstanceRenderState(): AnimatedModelInstanceRenderState3D {
        return FakeAnimatedModelInstanceRenderState3D().also { instanceStates += it }
    }

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D,
        pose: AnimationPose3D
    ) {
        error("FakePerInstanceAnimatedModel3D is only used for Scene3D collection tests.")
    }

    override fun cleanup() {
    }
}

private class FakeAnimatedModelInstanceRenderState3D : AnimatedModelInstanceRenderState3D {
    val updatedPoses = mutableListOf<AnimationPose3D>()
    var cleanedUp = false

    override fun updatePose(pose: AnimationPose3D) {
        updatedPoses += pose
    }

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D,
        pose: AnimationPose3D
    ) {
        error("FakeAnimatedModelInstanceRenderState3D is only used for Scene3D collection tests.")
    }

    override fun cleanup() {
        cleanedUp = true
    }
}
