package com.kengine.three

import com.kengine.math.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ThirdPersonCameraSettings3D(
    val fovDegrees: Float = 58f,
    val near: Float = 0.1f,
    val far: Float = 240f,
    val targetHeight: Double = 0.62,
    val eyeHeight: Double = 0.95,
    val yawSpeed: Double = 3.0,
    val pitchSpeed: Double = 1.75,
    val zoomSpeed: Double = 3.6,
    val lookSmoothing: Double = 18.0,
    val followSmoothing: Double = 10.0,
    val minPitch: Double = -0.08,
    val maxPitch: Double = 0.95,
    val minDistance: Double = 2.8,
    val maxDistance: Double = 7.2,
    val distanceStops: List<Double> = listOf(3.5, 4.9, 6.3),
    val inputStopEpsilon: Double = 0.01,
    val invertLookY: Boolean = false
) {
    init {
        require(minPitch <= maxPitch) {
            "ThirdPersonCameraSettings3D minPitch must be <= maxPitch."
        }
        require(minDistance <= maxDistance) {
            "ThirdPersonCameraSettings3D minDistance must be <= maxDistance."
        }
        require(distanceStops.isNotEmpty()) {
            "ThirdPersonCameraSettings3D requires at least one distance stop."
        }
    }
}

data class ThirdPersonCameraInput3D(
    val lookX: Double = 0.0,
    val lookY: Double = 0.0,
    val zoom: Double = 0.0,
    val cycleDistance: Boolean = false
)

data class ThirdPersonCamera3D(
    val eye: Vec3,
    val target: Vec3,
    val fovDegrees: Float = 58f,
    val near: Float = 0.1f,
    val far: Float = 240f
) : Camera3D {
    override fun viewProjection(aspect: Float): Mat4 {
        return Mat4.perspective(
            fovDegrees = fovDegrees,
            aspect = aspect,
            near = near,
            far = far
        ) * Mat4.lookAt(eye, target)
    }

    companion object {
        fun fromTarget(
            target: Vec3,
            yawRadians: Double,
            pitchRadians: Double,
            distance: Double,
            settings: ThirdPersonCameraSettings3D = ThirdPersonCameraSettings3D()
        ): ThirdPersonCamera3D {
            val forward = forwardForYaw(yawRadians)
            val horizontalDistance = cos(pitchRadians) * distance
            val eye = Vec3(
                x = target.x - forward.x * horizontalDistance,
                y = target.y + settings.eyeHeight + sin(pitchRadians) * distance,
                z = target.z - forward.z * horizontalDistance
            )
            val lookTarget = Vec3(target.x, target.y + settings.targetHeight, target.z)
            return ThirdPersonCamera3D(
                eye = eye,
                target = lookTarget,
                fovDegrees = settings.fovDegrees,
                near = settings.near,
                far = settings.far
            )
        }
    }
}

class ThirdPersonCameraController3D(
    target: Vec3,
    yawRadians: Double = 0.0,
    pitchRadians: Double = 0.38,
    distance: Double? = null,
    distanceStopIndex: Int = 0,
    val settings: ThirdPersonCameraSettings3D = ThirdPersonCameraSettings3D()
) {
    var focus: Vec3 = target
        private set
    var yawRadians: Double = yawRadians
        private set
    var pitchRadians: Double = pitchRadians.coerceIn(settings.minPitch, settings.maxPitch)
        private set
    var distanceStopIndex: Int = distanceStopIndex.coerceIn(0, settings.distanceStops.lastIndex)
        private set
    var distance: Double = (distance ?: settings.distanceStops[this.distanceStopIndex])
        .coerceIn(settings.minDistance, settings.maxDistance)
        private set

    private var smoothedLookX = 0.0
    private var smoothedLookY = 0.0
    private var wasCycleDistancePressed = false

    fun updateInput(
        input: ThirdPersonCameraInput3D,
        deltaSeconds: Double
    ) {
        val clampedDelta = deltaSeconds.coerceAtLeast(0.0)
        val lookSmoothing = smoothingFactor(settings.lookSmoothing, clampedDelta)
        smoothedLookX = smoothInput(smoothedLookX, input.lookX.coerceIn(-1.0, 1.0), lookSmoothing)
        val lookY = if (settings.invertLookY) input.lookY else -input.lookY
        smoothedLookY = smoothInput(smoothedLookY, lookY.coerceIn(-1.0, 1.0), lookSmoothing)
        yawRadians = wrapAngle(yawRadians + smoothedLookX * settings.yawSpeed * clampedDelta)
        pitchRadians = (pitchRadians + smoothedLookY * settings.pitchSpeed * clampedDelta)
            .coerceIn(settings.minPitch, settings.maxPitch)

        if (input.cycleDistance && !wasCycleDistancePressed) {
            distanceStopIndex = (distanceStopIndex + 1) % settings.distanceStops.size
            distance = settings.distanceStops[distanceStopIndex].coerceIn(settings.minDistance, settings.maxDistance)
        }
        wasCycleDistancePressed = input.cycleDistance

        if (input.zoom != 0.0) {
            distance = (distance + input.zoom.coerceIn(-1.0, 1.0) * settings.zoomSpeed * clampedDelta)
                .coerceIn(settings.minDistance, settings.maxDistance)
        }
    }

    fun follow(
        target: Vec3,
        deltaSeconds: Double
    ) {
        focus = lerp(focus, target, smoothingFactor(settings.followSmoothing, deltaSeconds.coerceAtLeast(0.0)))
    }

    fun update(
        target: Vec3,
        input: ThirdPersonCameraInput3D,
        deltaSeconds: Double
    ): ThirdPersonCamera3D {
        updateInput(input, deltaSeconds)
        follow(target, deltaSeconds)
        return camera()
    }

    fun camera(): ThirdPersonCamera3D {
        return ThirdPersonCamera3D.fromTarget(
            target = focus,
            yawRadians = yawRadians,
            pitchRadians = pitchRadians,
            distance = distance,
            settings = settings
        )
    }

    fun movementDirection(
        inputRight: Double,
        inputForward: Double
    ): Vec3 {
        return movementDirection(inputRight, inputForward, yawRadians)
    }

    fun horizontalVelocity(
        inputRight: Double,
        inputForward: Double,
        speed: Double
    ): Vec3 {
        val direction = movementDirection(inputRight, inputForward)
        return horizontalVelocityForMovement(direction, horizontalLength(direction), speed)
    }

    private fun smoothInput(
        current: Double,
        target: Double,
        amount: Double
    ): Double {
        val next = lerp(current, target, amount)
        return if (target == 0.0 && abs(next) < settings.inputStopEpsilon) 0.0 else next
    }
}

fun movementDirection(
    inputRight: Double,
    inputForward: Double,
    yawRadians: Double
): Vec3 {
    val forward = forwardForYaw(yawRadians)
    val right = rightForYaw(yawRadians)
    return Vec3(
        x = right.x * inputRight + forward.x * inputForward,
        y = 0.0,
        z = right.z * inputRight + forward.z * inputForward
    )
}

fun horizontalVelocityForMovement(
    moveDirection: Vec3,
    moveLength: Double,
    speed: Double
): Vec3 {
    val scale = speed / moveLength.coerceAtLeast(1.0)
    return Vec3(
        x = moveDirection.x * scale,
        y = 0.0,
        z = moveDirection.z * scale
    )
}

fun forwardForYaw(
    yawRadians: Double,
    length: Double = 1.0
): Vec3 {
    return Vec3(-sin(yawRadians) * length, 0.0, -cos(yawRadians) * length)
}

fun rightForYaw(
    yawRadians: Double,
    length: Double = 1.0
): Vec3 {
    return Vec3(cos(yawRadians) * length, 0.0, -sin(yawRadians) * length)
}

fun moveAngleToward(
    current: Double,
    target: Double,
    maxStep: Double
): Double {
    val delta = shortestAngleDelta(current, target)
    val clampedDelta = delta.coerceIn(-maxStep, maxStep)
    return wrapAngle(current + clampedDelta)
}

fun shortestAngleDelta(
    current: Double,
    target: Double
): Double {
    return wrapAngle(target - current)
}

fun wrapAngle(angle: Double): Double {
    var wrapped = angle
    while (wrapped > PI) {
        wrapped -= PI * 2.0
    }
    while (wrapped < -PI) {
        wrapped += PI * 2.0
    }
    return wrapped
}

fun horizontalLength(value: Vec3): Double {
    return sqrt(value.x * value.x + value.z * value.z)
}

fun horizontalDistance(
    a: Vec3,
    b: Vec3
): Double {
    return sqrt(squaredHorizontalDistance(a, b))
}

fun squaredHorizontalDistance(
    a: Vec3,
    b: Vec3
): Double {
    val deltaX = a.x - b.x
    val deltaZ = a.z - b.z
    return deltaX * deltaX + deltaZ * deltaZ
}

fun smoothingFactor(
    speed: Double,
    deltaSeconds: Double
): Double {
    return (speed * deltaSeconds).coerceIn(0.0, 1.0)
}

fun lerp(
    from: Double,
    to: Double,
    amount: Double
): Double {
    return from + (to - from) * amount
}

fun lerp(
    from: Vec3,
    to: Vec3,
    amount: Double
): Vec3 {
    return Vec3(
        x = from.x + (to.x - from.x) * amount,
        y = from.y + (to.y - from.y) * amount,
        z = from.z + (to.z - from.z) * amount
    )
}
