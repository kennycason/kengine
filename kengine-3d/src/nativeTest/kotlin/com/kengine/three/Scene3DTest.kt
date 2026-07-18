package com.kengine.three

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
    fun prepareForDrawAllowsSharedPerDrawAnimatedModelPoses() {
        val scene = Scene3D()
        val model = FakeAnimatedModel3D(AnimatedModelPoseUpdateMode3D.PER_DRAW)

        scene.addAnimatedModel(model, pose = AnimationPose3D(clipIndex = 0, timeSeconds = 0.0))
        scene.addAnimatedModel(model, pose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.5))
        scene.prepareForDraw()

        assertEquals(emptyList(), model.updatedPoses)
    }
}

private data class FakeSceneItem3D(
    val name: String,
    override var transform: Transform3D = Transform3D(),
    override var isVisible: Boolean = true
) : SceneItem3D {
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
