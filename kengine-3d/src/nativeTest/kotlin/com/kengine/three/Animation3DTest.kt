package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Animation3DTest {
    private enum class TestState {
        IDLE,
        RUN
    }

    @Test
    fun mapsStatesToClipIndicesByName() {
        val clips = AnimationClipSet3D.fromNames(listOf("Idle", "Run"))
        val map = AnimationClipMap3D.fromClipNames(
            clips = clips,
            references = listOf(
                AnimationClipReference3D(TestState.IDLE, "Idle"),
                AnimationClipReference3D(TestState.RUN, "Run", playbackSpeed = 2.0)
            )
        )

        assertEquals(0, map.selectionFor(TestState.IDLE).clipIndex)
        assertEquals(1, map.selectionFor(TestState.RUN).clipIndex)
        assertEquals(2.0, map.selectionFor(TestState.RUN).playbackSpeed)
    }

    @Test
    fun reportsMissingClipNames() {
        val clips = AnimationClipSet3D.fromNames(listOf("Idle"))

        assertFailsWith<IllegalArgumentException> {
            AnimationClipMap3D.fromClipNames(
                clips = clips,
                references = listOf(AnimationClipReference3D(TestState.RUN, "Run"))
            )
        }
    }

    @Test
    fun advancesTimeUsingPlaybackSpeed() {
        val selection = AnimationClipSelection3D(
            state = TestState.RUN,
            clipName = "Run",
            clipIndex = 1,
            playbackSpeed = 2.0
        )
        val player = AnimationPlayer3D(selection)

        val frame = player.play(selection, deltaSeconds = 0.25)

        assertEquals(TestState.RUN, frame.state)
        assertEquals(1, frame.clipIndex)
        assertEquals(0.5, frame.timeSeconds)
        assertEquals(false, frame.restarted)
    }

    @Test
    fun restartsTimeWhenSelectionChanges() {
        val idle = AnimationClipSelection3D(
            state = TestState.IDLE,
            clipName = "Idle",
            clipIndex = 0
        )
        val run = AnimationClipSelection3D(
            state = TestState.RUN,
            clipName = "Run",
            clipIndex = 1
        )
        val player = AnimationPlayer3D(idle)
        player.play(idle, deltaSeconds = 0.5)

        val frame = player.play(run, deltaSeconds = 0.5)

        assertEquals(TestState.RUN, frame.state)
        assertEquals(0.0, frame.timeSeconds)
        assertEquals(true, frame.restarted)
    }

    @Test
    fun createsPoseFromPlaybackFrame() {
        val selection = AnimationClipSelection3D(
            state = TestState.RUN,
            clipName = "Run",
            clipIndex = 3
        )
        val player = AnimationPlayer3D(selection)

        val pose = player.play(selection, deltaSeconds = 0.25).toAnimationPose3D()

        assertEquals(3, pose.clipIndex)
        assertEquals(0.25, pose.timeSeconds)
    }

    @Test
    fun stateControllerPlaysMappedStates() {
        val controller = AnimationStateController3D(
            clipMap = testClipMap(),
            initialState = TestState.IDLE
        )

        val idleFrame = controller.play(TestState.IDLE, deltaSeconds = 0.25)
        val runFrame = controller.play(TestState.RUN, deltaSeconds = 0.25)

        assertEquals(TestState.IDLE, idleFrame.state)
        assertEquals(0, idleFrame.clipIndex)
        assertEquals(0.25, idleFrame.timeSeconds)
        assertEquals(false, idleFrame.restarted)
        assertEquals(TestState.RUN, runFrame.state)
        assertEquals(1, runFrame.clipIndex)
        assertEquals(0.0, runFrame.timeSeconds)
        assertEquals(true, runFrame.restarted)
        assertEquals(TestState.RUN, controller.state)
        assertEquals("Run", controller.clipName)
        assertEquals(1, controller.clipIndex)
        assertEquals(2.0, controller.playbackSpeed)
    }

    @Test
    fun stateControllerCreatesPoseFrames() {
        val controller = AnimationStateController3D(
            clipMap = testClipMap(),
            initialState = TestState.RUN
        )

        val pose = controller.pose(TestState.RUN, deltaSeconds = 0.25)

        assertEquals(1, pose.clipIndex)
        assertEquals(0.5, pose.timeSeconds)
    }

    @Test
    fun stateControllerResetsToMappedState() {
        val controller = AnimationStateController3D(
            clipMap = testClipMap(),
            initialState = TestState.IDLE
        )
        controller.play(TestState.IDLE, deltaSeconds = 0.5)

        controller.reset(TestState.RUN)

        assertEquals(TestState.RUN, controller.state)
        assertEquals("Run", controller.clipName)
        assertEquals(1, controller.clipIndex)
        assertEquals(0.0, controller.timeSeconds)
    }

    @Test
    fun rejectsNegativePoseValues() {
        assertFailsWith<IllegalArgumentException> {
            AnimationPose3D(clipIndex = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            AnimationPose3D(timeSeconds = -0.01)
        }
    }

    @Test
    fun posePreparationUpdatesOnlyWhenPoseChanges() {
        val preparation = AnimationPosePreparation3D()
        val pose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.5)
        val updates = mutableListOf<AnimationPose3D>()

        assertTrue(preparation.prepare(pose) { updates += it })
        assertFalse(preparation.prepare(pose) { updates += it })

        val nextPose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.75)
        assertTrue(preparation.prepare(nextPose) { updates += it })

        assertEquals(listOf(pose, nextPose), updates)
        assertEquals(nextPose, preparation.preparedPose)
    }

    @Test
    fun posePreparationCanBeInvalidated() {
        val preparation = AnimationPosePreparation3D()
        val pose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.5)
        val updates = mutableListOf<AnimationPose3D>()

        preparation.prepare(pose) { updates += it }
        preparation.invalidate()
        preparation.prepare(pose) { updates += it }

        assertEquals(listOf(pose, pose), updates)
    }

    @Test
    fun posePreparationRejectsUnpreparedPose() {
        val preparation = AnimationPosePreparation3D()
        val pose = AnimationPose3D(clipIndex = 1, timeSeconds = 0.5)

        assertFailsWith<IllegalStateException> {
            preparation.requirePrepared(pose)
        }

        preparation.prepare(pose) {}
        preparation.requirePrepared(pose)
    }

    private fun testClipMap(): AnimationClipMap3D<TestState> {
        return AnimationClipMap3D.fromClipNames(
            clips = AnimationClipSet3D.fromNames(listOf("Idle", "Run")),
            references = listOf(
                AnimationClipReference3D(TestState.IDLE, "Idle"),
                AnimationClipReference3D(TestState.RUN, "Run", playbackSpeed = 2.0)
            )
        )
    }
}
