package com.kengine.three

import com.kengine.math.Vec3

class Node3D<T : SceneItem3D>(
    val item: T,
    val name: String? = null,
    override var transform: Transform3D = Transform3D(),
    var itemTransform: Transform3D = Transform3D(),
    override var isVisible: Boolean = true
) : SceneItem3D {
    override fun prepareForDraw() {
        if (!isVisible || !item.isVisible) {
            return
        }

        syncItemTransform()
        item.prepareForDraw()
    }

    override fun collectAnimatedModelInstances(instances: MutableList<AnimatedModelInstance3D>) {
        if (isVisible && item.isVisible) {
            item.collectAnimatedModelInstances(instances)
        }
    }

    override fun cleanup() {
        item.cleanup()
    }

    override fun draw(
        renderer: SceneRenderer3D,
        frame: GpuFrame,
        camera: Camera3D,
        sceneLight: DirectionalLight3D
    ) {
        if (!isVisible || !item.isVisible) {
            return
        }

        syncItemTransform()
        item.draw(renderer, frame, camera, sceneLight)
    }

    fun setItemVisible(isVisible: Boolean): Node3D<T> {
        item.isVisible = isVisible
        return this
    }

    fun setVisible(isVisible: Boolean): Node3D<T> {
        this.isVisible = isVisible
        return this
    }

    fun setTransform(transform: Transform3D): Node3D<T> {
        this.transform = transform
        return this
    }

    fun setPositionYaw(
        position: Vec3,
        yawRadians: Double,
        yOffset: Double = 0.0,
        yawOffsetRadians: Double = 0.0,
        scale: Vec3 = Vec3(1.0, 1.0, 1.0)
    ): Node3D<T> {
        return setTransform(
            Transform3D.positionYaw(
                position = position,
                yawRadians = yawRadians,
                yOffset = yOffset,
                yawOffsetRadians = yawOffsetRadians,
                scale = scale
            )
        )
    }

    fun setItemTransform(transform: Transform3D): Node3D<T> {
        itemTransform = transform
        return this
    }

    private fun syncItemTransform() {
        item.transform = transform * itemTransform
    }
}

fun Node3D<AnimatedModelInstance3D>.setPose(pose: AnimationPose3D): Node3D<AnimatedModelInstance3D> {
    item.setPose(pose)
    return this
}
