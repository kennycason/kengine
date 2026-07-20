package com.kengine.three

class Scene3D(
    var light: DirectionalLight3D = DirectionalLight3D()
) : GpuResource3D {
    private val renderItems = mutableListOf<SceneItem3D>()
    private var cleanedUp = false

    val items: List<SceneItem3D>
        get() = renderItems

    val visibleItems: List<SceneItem3D>
        get() = renderItems.filter { it.isVisible }

    fun <T : SceneItem3D> add(item: T): T {
        check(!cleanedUp) {
            "Scene3D has already been cleaned up."
        }
        renderItems += item
        return item
    }

    fun <T : SceneItem3D> addNode(node: Node3D<T>): Node3D<T> {
        return add(node)
    }

    fun addModelNode(
        model: Model3D,
        transform: Transform3D = Transform3D(),
        itemTransform: Transform3D = Transform3D(),
        light: DirectionalLight3D? = null,
        isVisible: Boolean = true
    ): Node3D<SceneModel3D> {
        return addNode(
            Node3D(
                item = SceneModel3D(
                    model = model,
                    light = light
                ),
                transform = transform,
                itemTransform = itemTransform,
                isVisible = isVisible
            )
        )
    }

    fun addAnimatedModelNode(
        model: AnimatedModel3D,
        transform: Transform3D = Transform3D(),
        itemTransform: Transform3D = Transform3D(),
        pose: AnimationPose3D = AnimationPose3D(),
        light: DirectionalLight3D? = null,
        isVisible: Boolean = true
    ): Node3D<AnimatedModelInstance3D> {
        return addNode(
            Node3D(
                item = model.createInstance(
                    pose = pose,
                    light = light
                ),
                transform = transform,
                itemTransform = itemTransform,
                isVisible = isVisible
            )
        )
    }

    fun addMeshNode(
        mesh: GpuMesh,
        transform: Transform3D = Transform3D(),
        itemTransform: Transform3D = Transform3D(),
        isVisible: Boolean = true
    ): Node3D<SceneMesh3D> {
        return addNode(
            Node3D(
                item = SceneMesh3D(mesh = mesh),
                transform = transform,
                itemTransform = itemTransform,
                isVisible = isVisible
            )
        )
    }

    fun addLitMeshNode(
        mesh: LitGpuMesh,
        transform: Transform3D = Transform3D(),
        itemTransform: Transform3D = Transform3D(),
        light: DirectionalLight3D? = null,
        isVisible: Boolean = true
    ): Node3D<SceneLitMesh3D> {
        return addNode(
            Node3D(
                item = SceneLitMesh3D(
                    mesh = mesh,
                    light = light
                ),
                transform = transform,
                itemTransform = itemTransform,
                isVisible = isVisible
            )
        )
    }

    fun addTexturedMeshNode(
        mesh: TexturedGpuMesh,
        texture: GpuTexture,
        transform: Transform3D = Transform3D(),
        itemTransform: Transform3D = Transform3D(),
        isVisible: Boolean = true
    ): Node3D<SceneTexturedMesh3D> {
        return addNode(
            Node3D(
                item = SceneTexturedMesh3D(
                    mesh = mesh,
                    texture = texture
                ),
                transform = transform,
                itemTransform = itemTransform,
                isVisible = isVisible
            )
        )
    }

    fun addTexturedLitMeshNode(
        mesh: TexturedLitGpuMesh,
        texture: GpuTexture,
        transform: Transform3D = Transform3D(),
        itemTransform: Transform3D = Transform3D(),
        light: DirectionalLight3D? = null,
        isVisible: Boolean = true
    ): Node3D<SceneTexturedLitMesh3D> {
        return addNode(
            Node3D(
                item = SceneTexturedLitMesh3D(
                    mesh = mesh,
                    texture = texture,
                    light = light
                ),
                transform = transform,
                itemTransform = itemTransform,
                isVisible = isVisible
            )
        )
    }

    fun addModel(
        model: Model3D,
        transform: Transform3D = Transform3D(),
        light: DirectionalLight3D? = null,
        isVisible: Boolean = true
    ): SceneModel3D {
        return add(
            SceneModel3D(
                model = model,
                transform = transform,
                light = light,
                isVisible = isVisible
            )
        )
    }

    fun addAnimatedModel(
        model: AnimatedModel3D,
        transform: Transform3D = Transform3D(),
        pose: AnimationPose3D = AnimationPose3D(),
        light: DirectionalLight3D? = null,
        isVisible: Boolean = true
    ): AnimatedModelInstance3D {
        return add(
            model.createInstance(
                transform = transform,
                pose = pose,
                light = light,
                isVisible = isVisible
            )
        )
    }

    fun addAnimatedModel(instance: AnimatedModelInstance3D): AnimatedModelInstance3D {
        return add(instance)
    }

    fun addMesh(
        mesh: GpuMesh,
        transform: Transform3D = Transform3D(),
        isVisible: Boolean = true
    ): SceneMesh3D {
        return add(
            SceneMesh3D(
                mesh = mesh,
                transform = transform,
                isVisible = isVisible
            )
        )
    }

    fun addLitMesh(
        mesh: LitGpuMesh,
        transform: Transform3D = Transform3D(),
        light: DirectionalLight3D? = null,
        isVisible: Boolean = true
    ): SceneLitMesh3D {
        return add(
            SceneLitMesh3D(
                mesh = mesh,
                transform = transform,
                light = light,
                isVisible = isVisible
            )
        )
    }

    fun addTexturedMesh(
        mesh: TexturedGpuMesh,
        texture: GpuTexture,
        transform: Transform3D = Transform3D(),
        isVisible: Boolean = true
    ): SceneTexturedMesh3D {
        return add(
            SceneTexturedMesh3D(
                mesh = mesh,
                texture = texture,
                transform = transform,
                isVisible = isVisible
            )
        )
    }

    fun addTexturedLitMesh(
        mesh: TexturedLitGpuMesh,
        texture: GpuTexture,
        transform: Transform3D = Transform3D(),
        light: DirectionalLight3D? = null,
        isVisible: Boolean = true
    ): SceneTexturedLitMesh3D {
        return add(
            SceneTexturedLitMesh3D(
                mesh = mesh,
                texture = texture,
                transform = transform,
                light = light,
                isVisible = isVisible
            )
        )
    }

    fun remove(item: SceneItem3D): Boolean {
        return renderItems.remove(item)
    }

    fun clear() {
        renderItems.clear()
    }

    fun prepareForDraw() {
        validateExplicitAnimatedModelPoses()
        renderItems.forEach { item ->
            if (item.isVisible) {
                item.prepareForDraw()
            }
        }
    }

    fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D
    ) {
        renderItems.forEach { item ->
            if (item.isVisible) {
                item.draw(renderer, frame, camera, light)
            }
        }
    }

    private fun validateExplicitAnimatedModelPoses() {
        val instances = mutableListOf<AnimatedModelInstance3D>()
        renderItems.forEach { item ->
            if (item.isVisible) {
                item.collectAnimatedModelInstances(instances)
            }
        }

        val posesByModel = mutableMapOf<AnimatedModel3D, AnimationPose3D>()
        instances.forEach { instance ->
            if (!instance.isVisible || !instance.requiresSharedExplicitPoseUpdate) {
                return@forEach
            }

            val previousPose = posesByModel[instance.model]
            check(previousPose == null || previousPose == instance.pose) {
                "Shared explicit-update animated models cannot be prepared with multiple poses in the same scene. " +
                    "Load a separate AnimatedModel3D for each independently animated CPU-skinned instance."
            }
            posesByModel[instance.model] = instance.pose
        }
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        var firstError: Throwable? = null
        renderItems.asReversed().forEach { item ->
            try {
                item.cleanup()
            } catch (e: Throwable) {
                if (firstError == null) {
                    firstError = e
                }
            }
        }
        renderItems.clear()
        firstError?.let { throw it }
    }
}

interface SceneItem3D {
    var transform: Transform3D
    var isVisible: Boolean

    fun prepareForDraw() {
    }

    fun collectAnimatedModelInstances(instances: MutableList<AnimatedModelInstance3D>) {
    }

    fun cleanup() {
    }

    fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    )
}

data class SceneModel3D(
    val model: Model3D,
    override var transform: Transform3D = Transform3D(),
    var light: DirectionalLight3D? = null,
    override var isVisible: Boolean = true
) : SceneItem3D {
    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    ) {
        renderer.draw(
            model = model,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light ?: sceneLight
        )
    }
}

data class SceneMesh3D(
    val mesh: GpuMesh,
    override var transform: Transform3D = Transform3D(),
    override var isVisible: Boolean = true
) : SceneItem3D {
    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    ) {
        renderer.draw(
            mesh = mesh,
            frame = frame,
            transform = transform,
            camera = camera
        )
    }
}

data class SceneLitMesh3D(
    val mesh: LitGpuMesh,
    override var transform: Transform3D = Transform3D(),
    var light: DirectionalLight3D? = null,
    override var isVisible: Boolean = true
) : SceneItem3D {
    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    ) {
        renderer.draw(
            mesh = mesh,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light ?: sceneLight
        )
    }
}

data class SceneTexturedMesh3D(
    val mesh: TexturedGpuMesh,
    val texture: GpuTexture,
    override var transform: Transform3D = Transform3D(),
    override var isVisible: Boolean = true
) : SceneItem3D {
    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    ) {
        renderer.draw(
            mesh = mesh,
            texture = texture,
            frame = frame,
            transform = transform,
            camera = camera
        )
    }
}

data class SceneTexturedLitMesh3D(
    val mesh: TexturedLitGpuMesh,
    val texture: GpuTexture,
    override var transform: Transform3D = Transform3D(),
    var light: DirectionalLight3D? = null,
    override var isVisible: Boolean = true
) : SceneItem3D {
    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    ) {
        renderer.draw(
            mesh = mesh,
            texture = texture,
            frame = frame,
            transform = transform,
            camera = camera,
            light = light ?: sceneLight
        )
    }
}

class SceneRenderer3D(
    gpu: GpuContext
) : GpuResource3D {
    val meshRenderer: MeshRenderer3D = MeshRenderer3D(gpu)
    val litRenderer: LitMeshRenderer3D = LitMeshRenderer3D(gpu)
    val texturedMeshRenderer: TexturedMeshRenderer3D = TexturedMeshRenderer3D(gpu)
    val texturedLitRenderer: TexturedLitMeshRenderer3D = TexturedLitMeshRenderer3D(gpu)
    private val skinnedTexturedLitRendererValue = lazy { SkinnedTexturedLitMeshRenderer3D(gpu) }
    val skinnedTexturedLitRenderer: SkinnedTexturedLitMeshRenderer3D
        get() = skinnedTexturedLitRendererValue.value
    val modelRenderer: ModelRenderer3D = ModelRenderer3D(
        litRenderer = litRenderer,
        texturedLitRenderer = texturedLitRenderer
    )

    private var cleanedUp = false

    fun draw(
        scene: Scene3D,
        frame: GpuFrame,
        camera: Camera3D
    ) {
        check(!cleanedUp) {
            "SceneRenderer3D has already been cleaned up."
        }
        scene.draw(this, frame, camera)
    }

    fun draw(
        model: Model3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        modelRenderer.draw(model, frame, transform, camera, light)
    }

    fun draw(
        model: AnimatedModel3D,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D(),
        pose: AnimationPose3D = AnimationPose3D()
    ) {
        model.draw(this, frame, transform, camera, light, pose)
    }

    fun draw(
        mesh: GpuMesh,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D
    ) {
        meshRenderer.draw(frame, mesh, transform, camera)
    }

    fun draw(
        mesh: LitGpuMesh,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        litRenderer.draw(frame, mesh, transform, camera, light)
    }

    fun draw(
        mesh: TexturedGpuMesh,
        texture: GpuTexture,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D
    ) {
        texturedMeshRenderer.draw(frame, mesh, texture, transform, camera)
    }

    fun draw(
        mesh: TexturedLitGpuMesh,
        texture: GpuTexture,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        texturedLitRenderer.draw(frame, mesh, texture, transform, camera, light)
    }

    fun draw(
        mesh: SkinnedTexturedLitGpuMesh,
        texture: GpuTexture,
        frame: GpuFrame,
        transform: Transform3D,
        camera: Camera3D,
        skinMatrices: List<Mat4>,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        skinnedTexturedLitRenderer.draw(frame, mesh, texture, transform, camera, skinMatrices, light)
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        if (skinnedTexturedLitRendererValue.isInitialized()) {
            skinnedTexturedLitRenderer.cleanup()
        }
        texturedLitRenderer.cleanup()
        texturedMeshRenderer.cleanup()
        litRenderer.cleanup()
        meshRenderer.cleanup()
    }
}
