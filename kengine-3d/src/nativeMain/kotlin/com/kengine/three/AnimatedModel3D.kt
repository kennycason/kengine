package com.kengine.three

enum class AnimatedModelType3D {
    NODE_ANIMATED_LIT,
    SKINNED_TEXTURED_LIT
}

enum class AnimatedModelPoseUpdateMode3D {
    PER_DRAW,
    EXPLICIT_UPDATE
}

interface AnimatedModel3D : GpuResource3D {
    val clips: AnimationClipSet3D
    val poseUpdateMode: AnimatedModelPoseUpdateMode3D

    fun updatePose(pose: AnimationPose3D) {
    }

    fun updatePose(
        clipIndex: Int = 0,
        timeSeconds: Double = 0.0
    ) {
        updatePose(AnimationPose3D(clipIndex, timeSeconds))
    }

    fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D(),
        pose: AnimationPose3D = AnimationPose3D()
    )
}

class AnimatedModelInstance3D(
    val model: AnimatedModel3D,
    override var transform: Transform3D = Transform3D(),
    pose: AnimationPose3D = AnimationPose3D(),
    var light: DirectionalLight3D? = null,
    override var isVisible: Boolean = true
) : SceneItem3D {
    var pose: AnimationPose3D = pose
        set(value) {
            if (field != value) {
                preparedPose = null
            }
            field = value
        }

    val clips: AnimationClipSet3D
        get() = model.clips

    val requiresExplicitPoseUpdate: Boolean
        get() = model.poseUpdateMode == AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE

    fun setPose(pose: AnimationPose3D): AnimatedModelInstance3D {
        this.pose = pose
        return this
    }

    fun setPose(frame: AnimationPlaybackFrame3D<*>): AnimatedModelInstance3D {
        return setPose(frame.toAnimationPose3D())
    }

    override fun prepareForDraw() {
        if (!requiresExplicitPoseUpdate || preparedPose == pose) {
            return
        }

        model.updatePose(pose)
        preparedPose = pose
    }

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    ) {
        if (requiresExplicitPoseUpdate) {
            check(preparedPose == pose) {
                "Explicit-update animated model instances must be prepared before drawing."
            }
        }
        renderer.draw(
            model = model,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light ?: sceneLight,
            pose = pose
        )
    }

    private var preparedPose: AnimationPose3D? = null
}

fun AnimatedModel3D.createInstance(
    transform: Transform3D = Transform3D(),
    pose: AnimationPose3D = AnimationPose3D(),
    light: DirectionalLight3D? = null,
    isVisible: Boolean = true
): AnimatedModelInstance3D {
    return AnimatedModelInstance3D(
        model = this,
        transform = transform,
        pose = pose,
        light = light,
        isVisible = isVisible
    )
}

object AnimatedModelLoader3D {
    fun load(
        gpu: GpuContext,
        assetPath: String,
        type: AnimatedModelType3D,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): AnimatedModel3D {
        return when (type) {
            AnimatedModelType3D.NODE_ANIMATED_LIT -> loadNodeAnimatedLit(gpu, assetPath, options)
            AnimatedModelType3D.SKINNED_TEXTURED_LIT -> loadSkinnedTexturedLit(gpu, assetPath, options)
        }
    }

    fun loadNodeAnimatedLit(
        gpu: GpuContext,
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): AnimatedModel3D {
        requireGlbAsset(assetPath, AnimatedModelType3D.NODE_ANIMATED_LIT)
        return GlbNodeAnimatedLitModel3D(
            GlbMeshLoader.loadAnimatedLit(gpu, assetPath, options.toGlbOptions())
        )
    }

    fun loadSkinnedTexturedLit(
        gpu: GpuContext,
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): AnimatedModel3D {
        requireGlbAsset(assetPath, AnimatedModelType3D.SKINNED_TEXTURED_LIT)
        return GlbSkinnedTexturedLitAnimatedModel3D(
            GlbMeshLoader.loadSkinnedTexturedLit(gpu, assetPath, options.toGlbOptions())
        )
    }

    private fun requireGlbAsset(
        assetPath: String,
        type: AnimatedModelType3D
    ) {
        val format = ModelLoader3D.detectFormat(assetPath)
        require(format == ModelFormat3D.GLB) {
            "$type animated model loading currently supports only GLB assets: $assetPath"
        }
    }
}

private class GlbNodeAnimatedLitModel3D(
    private val model: GlbAnimatedLitModel
) : AnimatedModel3D {
    override val clips: AnimationClipSet3D = AnimationClipSet3D.fromGlb(model.clips)
    override val poseUpdateMode: AnimatedModelPoseUpdateMode3D = AnimatedModelPoseUpdateMode3D.PER_DRAW

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D,
        pose: AnimationPose3D
    ) {
        model.draw(
            renderer = renderer.litRenderer,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light,
            clipIndex = pose.clipIndex,
            timeSeconds = pose.timeSeconds
        )
    }

    override fun cleanup() {
        model.cleanup()
    }
}

private class GlbSkinnedTexturedLitAnimatedModel3D(
    private val model: GlbSkinnedTexturedLitModel
) : AnimatedModel3D {
    override val clips: AnimationClipSet3D = AnimationClipSet3D.fromGlb(model.clips)
    override val poseUpdateMode: AnimatedModelPoseUpdateMode3D = AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE

    override fun updatePose(pose: AnimationPose3D) {
        model.updatePose(
            clipIndex = pose.clipIndex,
            timeSeconds = pose.timeSeconds
        )
    }

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D,
        pose: AnimationPose3D
    ) {
        model.draw(
            renderer = renderer.texturedLitRenderer,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light
        )
    }

    override fun cleanup() {
        model.cleanup()
    }
}
