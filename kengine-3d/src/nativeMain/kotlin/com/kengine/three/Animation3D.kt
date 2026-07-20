package com.kengine.three

data class AnimationClipSet3D(
    val clips: List<AnimationClipInfo3D>
) {
    val names: List<String> = clips.map { it.name }

    init {
        require(clips.isNotEmpty()) {
            "AnimationClipSet3D requires at least one clip."
        }
    }

    fun indexOf(name: String): Int? {
        val index = clips.indexOfFirst { it.name == name }
        return if (index >= 0) index else null
    }

    fun requireIndex(name: String): Int {
        return indexOf(name)
            ?: throw IllegalArgumentException("Missing animation clip '$name'. Available: ${names.joinToString()}")
    }

    companion object {
        fun fromNames(names: List<String>): AnimationClipSet3D {
            return AnimationClipSet3D(
                names.map {
                    AnimationClipInfo3D(
                        name = it,
                        durationSeconds = 0.0,
                        channelCount = 0
                    )
                }
            )
        }

        fun fromGlb(clips: List<GlbAnimationClipInfo>): AnimationClipSet3D {
            return AnimationClipSet3D(clips.map { it.toAnimationClipInfo3D() })
        }
    }
}

data class AnimationClipReference3D<T>(
    val state: T,
    val clipName: String,
    val playbackSpeed: Double = 1.0
)

data class AnimationClipSelection3D<T>(
    val state: T,
    val clipName: String,
    val clipIndex: Int,
    val playbackSpeed: Double = 1.0
)

class AnimationClipMap3D<T> private constructor(
    private val selectionsByState: Map<T, AnimationClipSelection3D<T>>
) {
    init {
        require(selectionsByState.isNotEmpty()) {
            "AnimationClipMap3D requires at least one state mapping."
        }
    }

    fun selectionFor(state: T): AnimationClipSelection3D<T> {
        return selectionsByState[state]
            ?: throw IllegalArgumentException("No animation clip mapping exists for state '$state'.")
    }

    companion object {
        fun <T> fromClipNames(
            clips: AnimationClipSet3D,
            references: Iterable<AnimationClipReference3D<T>>
        ): AnimationClipMap3D<T> {
            val selections = references.associate { reference ->
                reference.state to AnimationClipSelection3D(
                    state = reference.state,
                    clipName = reference.clipName,
                    clipIndex = clips.requireIndex(reference.clipName),
                    playbackSpeed = reference.playbackSpeed
                )
            }
            return AnimationClipMap3D(selections)
        }
    }
}

data class AnimationPlaybackFrame3D<T>(
    val state: T,
    val clipName: String,
    val clipIndex: Int,
    val timeSeconds: Double,
    val playbackSpeed: Double,
    val restarted: Boolean
)

data class AnimationPose3D(
    val clipIndex: Int = 0,
    val timeSeconds: Double = 0.0
) {
    init {
        require(clipIndex >= 0) {
            "AnimationPose3D clipIndex must be non-negative."
        }
        require(timeSeconds >= 0.0) {
            "AnimationPose3D timeSeconds must be non-negative."
        }
    }

    companion object {
        fun from(frame: AnimationPlaybackFrame3D<*>): AnimationPose3D {
            return AnimationPose3D(
                clipIndex = frame.clipIndex,
                timeSeconds = frame.timeSeconds
            )
        }
    }
}

class AnimationPosePreparation3D {
    var preparedPose: AnimationPose3D? = null
        private set

    fun invalidate() {
        preparedPose = null
    }

    fun isPrepared(pose: AnimationPose3D): Boolean {
        return preparedPose == pose
    }

    fun prepare(
        pose: AnimationPose3D,
        updatePose: (AnimationPose3D) -> Unit
    ): Boolean {
        if (isPrepared(pose)) {
            return false
        }

        updatePose(pose)
        preparedPose = pose
        return true
    }

    fun requirePrepared(
        pose: AnimationPose3D,
        message: String = "Explicit-update animated model instances must be prepared before drawing."
    ) {
        check(isPrepared(pose)) {
            message
        }
    }
}

fun AnimationPlaybackFrame3D<*>.toAnimationPose3D(): AnimationPose3D {
    return AnimationPose3D.from(this)
}

class AnimationPlayer3D<T>(
    initialSelection: AnimationClipSelection3D<T>
) {
    var state: T = initialSelection.state
        private set
    var clipName: String = initialSelection.clipName
        private set
    var clipIndex: Int = initialSelection.clipIndex
        private set
    var timeSeconds: Double = 0.0
        private set
    var playbackSpeed: Double = initialSelection.playbackSpeed
        private set

    fun play(
        selection: AnimationClipSelection3D<T>,
        deltaSeconds: Double
    ): AnimationPlaybackFrame3D<T> {
        val restarted = selection.state != state || selection.clipIndex != clipIndex
        if (restarted) {
            timeSeconds = 0.0
        } else {
            timeSeconds += deltaSeconds.coerceAtLeast(0.0) * selection.playbackSpeed
        }

        state = selection.state
        clipName = selection.clipName
        clipIndex = selection.clipIndex
        playbackSpeed = selection.playbackSpeed

        return AnimationPlaybackFrame3D(
            state = state,
            clipName = clipName,
            clipIndex = clipIndex,
            timeSeconds = timeSeconds,
            playbackSpeed = playbackSpeed,
            restarted = restarted
        )
    }

    fun reset(selection: AnimationClipSelection3D<T>) {
        state = selection.state
        clipName = selection.clipName
        clipIndex = selection.clipIndex
        playbackSpeed = selection.playbackSpeed
        timeSeconds = 0.0
    }
}

class AnimationStateController3D<T>(
    val clipMap: AnimationClipMap3D<T>,
    initialState: T
) {
    private val player = AnimationPlayer3D(clipMap.selectionFor(initialState))

    val state: T
        get() = player.state
    val clipName: String
        get() = player.clipName
    val clipIndex: Int
        get() = player.clipIndex
    val timeSeconds: Double
        get() = player.timeSeconds
    val playbackSpeed: Double
        get() = player.playbackSpeed

    fun play(
        state: T,
        deltaSeconds: Double
    ): AnimationPlaybackFrame3D<T> {
        return player.play(
            selection = clipMap.selectionFor(state),
            deltaSeconds = deltaSeconds
        )
    }

    fun pose(
        state: T,
        deltaSeconds: Double
    ): AnimationPose3D {
        return play(state, deltaSeconds).toAnimationPose3D()
    }

    fun reset(state: T) {
        player.reset(clipMap.selectionFor(state))
    }
}

private fun GlbAnimationClipInfo.toAnimationClipInfo3D(): AnimationClipInfo3D {
    return AnimationClipInfo3D(
        name = name,
        durationSeconds = durationSeconds,
        channelCount = channelCount,
        channels = channels.map { it.toAnimationChannelInfo3D() }
    )
}

private fun GlbAnimationChannelInfo.toAnimationChannelInfo3D(): AnimationChannelInfo3D {
    return AnimationChannelInfo3D(
        nodeIndex = nodeIndex,
        nodeName = nodeName,
        path = path,
        interpolation = interpolation,
        keyframeCount = keyframeCount
    )
}
