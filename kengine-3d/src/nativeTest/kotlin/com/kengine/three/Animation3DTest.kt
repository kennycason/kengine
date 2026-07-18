package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun rejectsNegativePoseValues() {
        assertFailsWith<IllegalArgumentException> {
            AnimationPose3D(clipIndex = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            AnimationPose3D(timeSeconds = -0.01)
        }
    }
}
