package com.kengine.three

enum class AnimatedModelType3D {
    NODE_ANIMATED_LIT,
    SKINNED_TEXTURED_LIT
}

enum class AnimatedModelPoseUpdateMode3D {
    PER_DRAW,
    EXPLICIT_UPDATE
}

enum class AnimatedModelPoseUpdateScope3D {
    MODEL,
    INSTANCE
}

enum class AnimatedModelSkinningMode3D {
    AUTO,
    CPU_VERTEX_BUFFER,
    GPU_JOINT_PALETTE
}

internal enum class ResolvedAnimatedModelSkinningMode3D {
    CPU_VERTEX_BUFFER,
    GPU_JOINT_PALETTE
}

internal fun resolveAnimatedModelSkinningMode3D(
    skinningMode: AnimatedModelSkinningMode3D,
    maxSkinJointCount: Int
): ResolvedAnimatedModelSkinningMode3D {
    return when (skinningMode) {
        AnimatedModelSkinningMode3D.AUTO -> {
            if (maxSkinJointCount <= SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS) {
                ResolvedAnimatedModelSkinningMode3D.GPU_JOINT_PALETTE
            } else {
                ResolvedAnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER
            }
        }

        AnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER -> ResolvedAnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER
        AnimatedModelSkinningMode3D.GPU_JOINT_PALETTE -> ResolvedAnimatedModelSkinningMode3D.GPU_JOINT_PALETTE
    }
}

interface AnimatedModelInstanceRenderState3D : GpuResource3D {
    fun updatePose(pose: AnimationPose3D) {
    }

    fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D(),
        pose: AnimationPose3D = AnimationPose3D()
    )

    override fun cleanup() {
    }
}

interface AnimatedModel3D : GpuResource3D {
    val clips: AnimationClipSet3D
    val poseUpdateMode: AnimatedModelPoseUpdateMode3D
    val poseUpdateScope: AnimatedModelPoseUpdateScope3D
        get() = when (poseUpdateMode) {
            AnimatedModelPoseUpdateMode3D.PER_DRAW -> AnimatedModelPoseUpdateScope3D.INSTANCE
            AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE -> AnimatedModelPoseUpdateScope3D.MODEL
        }

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

    fun createInstanceRenderState(): AnimatedModelInstanceRenderState3D {
        return SharedAnimatedModelInstanceRenderState3D(this)
    }
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
                posePreparation.invalidate()
            }
            field = value
        }

    val clips: AnimationClipSet3D
        get() = model.clips

    val requiresExplicitPoseUpdate: Boolean
        get() = model.poseUpdateMode == AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE

    val requiresSharedExplicitPoseUpdate: Boolean
        get() = requiresExplicitPoseUpdate && model.poseUpdateScope == AnimatedModelPoseUpdateScope3D.MODEL

    fun setPose(pose: AnimationPose3D): AnimatedModelInstance3D {
        this.pose = pose
        return this
    }

    fun setPose(frame: AnimationPlaybackFrame3D<*>): AnimatedModelInstance3D {
        return setPose(frame.toAnimationPose3D())
    }

    override fun prepareForDraw() {
        if (!requiresExplicitPoseUpdate) {
            return
        }

        posePreparation.prepare(pose, renderState::updatePose)
    }

    override fun collectAnimatedModelInstances(instances: MutableList<AnimatedModelInstance3D>) {
        if (isVisible) {
            instances += this
        }
    }

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    ) {
        if (requiresExplicitPoseUpdate) {
            posePreparation.requirePrepared(pose)
        }
        renderState.draw(
            renderer = renderer,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light ?: sceneLight,
            pose = pose
        )
    }

    override fun cleanup() {
        renderState.cleanup()
    }

    private val renderState = model.createInstanceRenderState()
    private val posePreparation = AnimationPosePreparation3D()
}

private class SharedAnimatedModelInstanceRenderState3D(
    private val model: AnimatedModel3D
) : AnimatedModelInstanceRenderState3D {
    override fun updatePose(pose: AnimationPose3D) {
        model.updatePose(pose)
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
            renderer = renderer,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light,
            pose = pose
        )
    }
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

class AnimatedModelSource3D internal constructor(
    val assetPath: String,
    val format: ModelFormat3D,
    val type: AnimatedModelType3D,
    val options: ModelLoadOptions3D,
    internal val data: AnimatedModelSourceData3D
) {
    val info: ModelInfo3D = data.info
    val clips: AnimationClipSet3D = data.clips

    fun upload(
        gpu: GpuContext,
        textureCache: GpuTextureCache3D? = null
    ): AnimatedModel3D {
        return AnimatedModelLoader3D.upload(this, gpu, textureCache)
    }
}

internal abstract class AnimatedModelSourceData3D {
    abstract val info: ModelInfo3D
    abstract val clips: AnimationClipSet3D
    abstract val vertexCount: Int

    abstract fun upload(
        gpu: GpuContext,
        textureCache: GpuTextureCache3D?,
        options: ModelLoadOptions3D
    ): AnimatedModel3D
}

internal data class NodeAnimatedLitModelSource3D(
    override val info: ModelInfo3D,
    val parts: List<NodeAnimatedLitMeshPartSource3D>,
    val nodes: List<ModelNode3D>,
    val sceneNodeIndices: List<Int>,
    val animationClips: List<ModelAnimationClip3D>,
    val normalizationMatrix: Mat4
) : AnimatedModelSourceData3D() {
    override val clips: AnimationClipSet3D = AnimationClipSet3D(info.animations)
    override val vertexCount: Int = parts.sumOf { it.vertices.size }

    override fun upload(
        gpu: GpuContext,
        textureCache: GpuTextureCache3D?,
        options: ModelLoadOptions3D
    ): AnimatedModel3D {
        return GlbNodeAnimatedLitModel3D(GlbMeshLoader.uploadAnimatedLit(gpu, this))
    }
}

internal data class NodeAnimatedLitMeshPartSource3D(
    val nodeIndex: Int,
    val meshIndex: Int,
    val vertices: List<LitVertex3D>
)

internal data class SkinnedTexturedLitModelSource3D(
    override val info: ModelInfo3D,
    val parts: List<SkinnedTexturedLitMeshPartSource3D>,
    val nodes: List<ModelNode3D>,
    val sceneNodeIndices: List<Int>,
    val skins: List<ModelSkin3D>,
    val animationClips: List<ModelAnimationClip3D>,
    val normalizationMatrix: Mat4
) : AnimatedModelSourceData3D() {
    override val clips: AnimationClipSet3D = AnimationClipSet3D(info.animations)
    override val vertexCount: Int = parts.sumOf { it.restVertices.size }
    val maxSkinJointCount: Int = skins.maxOfOrNull { it.joints.size } ?: 0

    override fun upload(
        gpu: GpuContext,
        textureCache: GpuTextureCache3D?,
        options: ModelLoadOptions3D
    ): AnimatedModel3D {
        return GlbSkinnedTexturedLitAnimatedModel3D(
            model = GlbMeshLoader.uploadSkinnedTexturedLit(
                gpu = gpu,
                source = this,
                textureCache = textureCache
            ),
            skinningMode = options.animatedSkinningMode
        )
    }
}

internal data class SkinnedTexturedLitMeshPartSource3D(
    val nodeIndex: Int,
    val skinIndex: Int,
    val materialDescriptor: MaterialDescriptor3D,
    val restVertices: List<TexturedLitVertex3D>,
    val skinnedVertices: List<SkinnedTexturedLitVertex3D>,
    val sourceVertices: List<SkinnedTexturedLitVertexSource3D>
)

object AnimatedModelLoader3D {
    fun load(
        gpu: GpuContext,
        assetPath: String,
        type: AnimatedModelType3D,
        options: ModelLoadOptions3D = ModelLoadOptions3D(),
        textureCache: GpuTextureCache3D? = null,
        sourceCache: AnimatedModelSourceCache3D? = null
    ): AnimatedModel3D {
        val source = sourceCache?.load(assetPath, type, options) ?: loadSource(assetPath, type, options)
        return upload(source, gpu, textureCache)
    }

    fun loadSource(
        assetPath: String,
        type: AnimatedModelType3D,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): AnimatedModelSource3D {
        val format = requireGltfAsset(assetPath, type)
        return when (type) {
            AnimatedModelType3D.NODE_ANIMATED_LIT -> loadNodeAnimatedLitSource(assetPath, options, format)
            AnimatedModelType3D.SKINNED_TEXTURED_LIT -> loadSkinnedTexturedLitSource(assetPath, options, format)
        }
    }

    fun upload(
        source: AnimatedModelSource3D,
        gpu: GpuContext,
        textureCache: GpuTextureCache3D? = null
    ): AnimatedModel3D {
        return source.data.upload(gpu, textureCache, source.options)
    }

    fun inspect(
        assetPath: String,
        type: AnimatedModelType3D
    ): ModelInfo3D {
        requireGltfAsset(assetPath, type)
        return ModelLoader3D.inspect(assetPath)
    }

    fun loadNodeAnimatedLit(
        gpu: GpuContext,
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D()
    ): AnimatedModel3D {
        return load(
            gpu = gpu,
            assetPath = assetPath,
            type = AnimatedModelType3D.NODE_ANIMATED_LIT,
            options = options
        )
    }

    fun loadSkinnedTexturedLit(
        gpu: GpuContext,
        assetPath: String,
        options: ModelLoadOptions3D = ModelLoadOptions3D(),
        textureCache: GpuTextureCache3D? = null
    ): AnimatedModel3D {
        return load(
            gpu = gpu,
            assetPath = assetPath,
            type = AnimatedModelType3D.SKINNED_TEXTURED_LIT,
            options = options,
            textureCache = textureCache
        )
    }

    private fun loadNodeAnimatedLitSource(
        assetPath: String,
        options: ModelLoadOptions3D,
        format: ModelFormat3D
    ): AnimatedModelSource3D {
        val source = GlbMeshLoader.loadAnimatedLitSource(assetPath, options.toGlbOptions())
        return AnimatedModelSource3D(
            assetPath = assetPath,
            format = format,
            type = AnimatedModelType3D.NODE_ANIMATED_LIT,
            options = options,
            data = source
        )
    }

    private fun loadSkinnedTexturedLitSource(
        assetPath: String,
        options: ModelLoadOptions3D,
        format: ModelFormat3D
    ): AnimatedModelSource3D {
        val source = GlbMeshLoader.loadSkinnedTexturedLitSource(assetPath, options.toGlbOptions())
        return AnimatedModelSource3D(
            assetPath = assetPath,
            format = format,
            type = AnimatedModelType3D.SKINNED_TEXTURED_LIT,
            options = options,
            data = source
        )
    }

    private fun requireGltfAsset(
        assetPath: String,
        type: AnimatedModelType3D
    ): ModelFormat3D {
        val format = ModelLoader3D.detectFormat(assetPath)
        require(format == ModelFormat3D.GLB || format == ModelFormat3D.GLTF) {
            "$type animated model loading currently supports only GLB/GLTF assets: $assetPath"
        }
        return format
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
    private val model: GlbSkinnedTexturedLitModel,
    private val skinningMode: AnimatedModelSkinningMode3D
) : AnimatedModel3D {
    override val clips: AnimationClipSet3D = AnimationClipSet3D.fromGlb(model.clips)
    override val poseUpdateMode: AnimatedModelPoseUpdateMode3D = AnimatedModelPoseUpdateMode3D.EXPLICIT_UPDATE
    override val poseUpdateScope: AnimatedModelPoseUpdateScope3D = AnimatedModelPoseUpdateScope3D.INSTANCE
    private val resolvedSkinningMode = resolveAnimatedModelSkinningMode3D(skinningMode, model.maxSkinJointCount)
    private var gpuSharedInstance: GlbGpuSkinnedTexturedLitModelInstance? = null

    init {
        require(
            resolvedSkinningMode != ResolvedAnimatedModelSkinningMode3D.GPU_JOINT_PALETTE ||
                model.maxSkinJointCount <= SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS
        ) {
            "GPU joint-palette skinning supports at most ${SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS} joints per skin, " +
                "but this animated model has a skin with ${model.maxSkinJointCount} joints. " +
                "Use ${AnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER} for this asset."
        }
    }

    override fun updatePose(pose: AnimationPose3D) {
        when (resolvedSkinningMode) {
            ResolvedAnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER -> {
                model.updatePose(
                    clipIndex = pose.clipIndex,
                    timeSeconds = pose.timeSeconds
                )
            }

            ResolvedAnimatedModelSkinningMode3D.GPU_JOINT_PALETTE -> {
                requireGpuSharedInstance().updatePose(
                    clipIndex = pose.clipIndex,
                    timeSeconds = pose.timeSeconds
                )
            }
        }
    }

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D,
        pose: AnimationPose3D
    ) {
        when (resolvedSkinningMode) {
            ResolvedAnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER -> {
                model.draw(
                    renderer = renderer.texturedLitRenderer,
                    frame = frame,
                    transform = transform,
                    camera = camera,
                    light = light
                )
            }

            ResolvedAnimatedModelSkinningMode3D.GPU_JOINT_PALETTE -> {
                requireGpuSharedInstance().draw(
                    renderer = renderer.skinnedTexturedLitRenderer,
                    frame = frame,
                    transform = transform,
                    camera = camera,
                    light = light
                )
            }
        }
    }

    override fun cleanup() {
        gpuSharedInstance?.cleanup()
        gpuSharedInstance = null
        model.cleanup()
    }

    override fun createInstanceRenderState(): AnimatedModelInstanceRenderState3D {
        return when (resolvedSkinningMode) {
            ResolvedAnimatedModelSkinningMode3D.CPU_VERTEX_BUFFER ->
                GlbSkinnedTexturedLitAnimatedModelInstanceRenderState3D(model.createInstance())

            ResolvedAnimatedModelSkinningMode3D.GPU_JOINT_PALETTE ->
                GlbGpuSkinnedTexturedLitAnimatedModelInstanceRenderState3D(model.createGpuSkinnedInstance())
        }
    }

    private fun requireGpuSharedInstance(): GlbGpuSkinnedTexturedLitModelInstance {
        return gpuSharedInstance ?: model.createGpuSkinnedInstance().also { gpuSharedInstance = it }
    }
}

private class GlbSkinnedTexturedLitAnimatedModelInstanceRenderState3D(
    private val instance: GlbSkinnedTexturedLitModelInstance
) : AnimatedModelInstanceRenderState3D {
    override fun updatePose(pose: AnimationPose3D) {
        instance.updatePose(
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
        instance.draw(
            renderer = renderer.texturedLitRenderer,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light
        )
    }

    override fun cleanup() {
        instance.cleanup()
    }
}

private class GlbGpuSkinnedTexturedLitAnimatedModelInstanceRenderState3D(
    private val instance: GlbGpuSkinnedTexturedLitModelInstance
) : AnimatedModelInstanceRenderState3D {
    override fun updatePose(pose: AnimationPose3D) {
        instance.updatePose(
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
        instance.draw(
            renderer = renderer.skinnedTexturedLitRenderer,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light
        )
    }

    override fun cleanup() {
        instance.cleanup()
    }
}
