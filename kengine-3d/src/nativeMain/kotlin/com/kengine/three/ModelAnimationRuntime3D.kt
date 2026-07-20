package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class ModelNode3D(
    val name: String?,
    val meshIndex: Int?,
    val skinIndex: Int?,
    val children: List<Int>,
    val transform: ModelNodeTransform3D,
    val matrix: ModelMatrix3D
)

internal data class ModelAnimationClip3D(
    val info: GlbAnimationClipInfo,
    val channels: List<ModelAnimationChannel3D>,
    val durationSeconds: Double
) {
    fun sampleInto(
        timeSeconds: Double,
        transforms: MutableList<ModelNodeTransform3D>
    ) {
        if (durationSeconds <= 0.0) {
            return
        }

        val localTime = positiveModulo(timeSeconds, durationSeconds)
        channels.forEach { channel ->
            val transform = transforms.getOrNull(channel.targetNodeIndex) ?: return@forEach
            transforms[channel.targetNodeIndex] = when (channel.path) {
                ModelAnimationChannelPath3D.TRANSLATION -> transform.copy(
                    translation = channel.sampler.sampleVec3(localTime)
                )
                ModelAnimationChannelPath3D.ROTATION -> transform.copy(
                    rotation = channel.sampler.sampleQuat(localTime)
                )
                ModelAnimationChannelPath3D.SCALE -> transform.copy(
                    scale = channel.sampler.sampleVec3(localTime)
                )
            }
        }
    }

    private fun positiveModulo(
        value: Double,
        divisor: Double
    ): Double {
        val result = value % divisor
        return if (result < 0.0) result + divisor else result
    }
}

internal data class ModelAnimationChannel3D(
    val targetNodeIndex: Int,
    val path: ModelAnimationChannelPath3D,
    val sampler: ModelAnimationValueSampler3D
)

internal enum class ModelAnimationChannelPath3D(
    val value: String
) {
    TRANSLATION("translation"),
    ROTATION("rotation"),
    SCALE("scale");

    companion object {
        fun from(value: String): ModelAnimationChannelPath3D {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unsupported model animation target path: $value")
        }
    }
}

internal sealed class ModelAnimationValueSampler3D(
    val times: List<Double>,
    val interpolation: String
) {
    val durationSeconds: Double = times.lastOrNull() ?: 0.0
    val keyframeCount: Int = times.size

    open fun sampleVec3(timeSeconds: Double): Vec3 {
        throw IllegalStateException("Animation sampler does not contain Vec3 values.")
    }

    open fun sampleQuat(timeSeconds: Double): ModelQuaternion3D {
        throw IllegalStateException("Animation sampler does not contain quaternion values.")
    }

    protected fun frameAt(timeSeconds: Double): ModelAnimationFrameBlend3D {
        require(times.isNotEmpty()) {
            "Animation sampler contains no keyframes."
        }
        if (times.size == 1 || timeSeconds <= times.first()) {
            return ModelAnimationFrameBlend3D(0, 0, 0.0)
        }
        for (index in 0 until times.size - 1) {
            val start = times[index]
            val end = times[index + 1]
            if (timeSeconds <= end) {
                val amount = if (end > start) ((timeSeconds - start) / (end - start)).coerceIn(0.0, 1.0) else 0.0
                return if (interpolation == "STEP") {
                    ModelAnimationFrameBlend3D(index, index, 0.0)
                } else {
                    ModelAnimationFrameBlend3D(index, index + 1, amount)
                }
            }
        }
        val lastIndex = times.lastIndex
        return ModelAnimationFrameBlend3D(lastIndex, lastIndex, 0.0)
    }

    class Vec3Sampler(
        times: List<Double>,
        private val values: List<Vec3>,
        interpolation: String
    ) : ModelAnimationValueSampler3D(times, interpolation) {
        override fun sampleVec3(timeSeconds: Double): Vec3 {
            val frame = frameAt(timeSeconds)
            val from = values[frame.fromIndex]
            val to = values[frame.toIndex]
            return Vec3(
                x = from.x + (to.x - from.x) * frame.amount,
                y = from.y + (to.y - from.y) * frame.amount,
                z = from.z + (to.z - from.z) * frame.amount
            )
        }
    }

    class QuatSampler(
        times: List<Double>,
        private val values: List<ModelQuaternion3D>,
        interpolation: String
    ) : ModelAnimationValueSampler3D(times, interpolation) {
        override fun sampleQuat(timeSeconds: Double): ModelQuaternion3D {
            val frame = frameAt(timeSeconds)
            return values[frame.fromIndex].slerp(values[frame.toIndex], frame.amount)
        }
    }
}

internal data class ModelAnimationFrameBlend3D(
    val fromIndex: Int,
    val toIndex: Int,
    val amount: Double
)

internal data class ModelSkin3D(
    val name: String?,
    val skeletonRootNodeIndex: Int?,
    val joints: List<Int>,
    val inverseBindMatrices: List<ModelMatrix3D>
)

internal data class SkinnedTexturedLitVertexSource3D(
    val position: Vec3,
    val normal: Vec3,
    val color: Color,
    val u: Float,
    val v: Float,
    val joints: SkinJointIndicesSource3D,
    val weights: SkinJointWeightsSource3D
) {
    fun toTexturedVertex(skinMatrices: List<ModelMatrix3D>): TexturedLitVertex3D {
        var skinnedPosition = Vec3(0.0, 0.0, 0.0)
        var skinnedNormal = Vec3(0.0, 0.0, 0.0)
        var totalWeight = 0.0

        for (index in 0 until 4) {
            val weight = weights.values[index]
            if (weight <= 0.0) {
                continue
            }
            val skinMatrix = skinMatrices.getOrNull(joints.values[index]) ?: continue
            skinnedPosition = addVectors(skinnedPosition, scaleVector(skinMatrix.transformPoint(position), weight))
            skinnedNormal = addVectors(skinnedNormal, scaleVector(skinMatrix.transformVector(normal), weight))
            totalWeight += weight
        }

        if (totalWeight <= 0.0) {
            skinnedPosition = position
            skinnedNormal = normal
        }

        return TexturedLitVertex3D(
            position = skinnedPosition,
            normal = normalizeVector(skinnedNormal),
            color = color,
            u = u,
            v = v
        )
    }

    fun toSkinnedTexturedLitVertex(): SkinnedTexturedLitVertex3D {
        return SkinnedTexturedLitVertex3D(
            position = position,
            normal = normal,
            color = color,
            u = u,
            v = v,
            joints = SkinJointIndices3D(
                x = joints.values.getOrElse(0) { 0 },
                y = joints.values.getOrElse(1) { 0 },
                z = joints.values.getOrElse(2) { 0 },
                w = joints.values.getOrElse(3) { 0 }
            ),
            weights = SkinJointWeights3D(
                x = weights.values.getOrElse(0) { 0.0 },
                y = weights.values.getOrElse(1) { 0.0 },
                z = weights.values.getOrElse(2) { 0.0 },
                w = weights.values.getOrElse(3) { 0.0 }
            ).normalized()
        )
    }
}

internal data class SkinJointIndicesSource3D(
    val values: IntArray
)

internal data class SkinJointWeightsSource3D(
    val values: DoubleArray
) {
    fun normalized(): SkinJointWeightsSource3D {
        val total = values.sum()
        if (total <= 0.0) {
            return this
        }
        return SkinJointWeightsSource3D(DoubleArray(values.size) { index -> values[index] / total })
    }
}

internal data class ModelNodeTransform3D(
    val translation: Vec3,
    val rotation: ModelQuaternion3D,
    val scale: Vec3
) {
    fun matrix(): ModelMatrix3D {
        return ModelMatrix3D.fromTransform(translation, rotation, scale)
    }
}

internal data class ModelQuaternion3D(
    val x: Double,
    val y: Double,
    val z: Double,
    val w: Double
) {
    fun normalized(): ModelQuaternion3D {
        val length = sqrt(x * x + y * y + z * z + w * w)
        if (length == 0.0) {
            return ModelQuaternion3D(0.0, 0.0, 0.0, 1.0)
        }
        return ModelQuaternion3D(x / length, y / length, z / length, w / length)
    }

    fun slerp(
        target: ModelQuaternion3D,
        amount: Double
    ): ModelQuaternion3D {
        var end = target
        var cosine = x * target.x + y * target.y + z * target.z + w * target.w
        if (cosine < 0.0) {
            cosine = -cosine
            end = ModelQuaternion3D(-target.x, -target.y, -target.z, -target.w)
        }

        if (cosine > 0.9995) {
            return ModelQuaternion3D(
                x = x + (end.x - x) * amount,
                y = y + (end.y - y) * amount,
                z = z + (end.z - z) * amount,
                w = w + (end.w - w) * amount
            ).normalized()
        }

        val angle = acos(cosine.coerceIn(-1.0, 1.0))
        val sine = sin(angle)
        if (abs(sine) < 0.000001) {
            return this
        }

        val fromScale = sin((1.0 - amount) * angle) / sine
        val toScale = sin(amount * angle) / sine
        return ModelQuaternion3D(
            x = x * fromScale + end.x * toScale,
            y = y * fromScale + end.y * toScale,
            z = z * fromScale + end.z * toScale,
            w = w * fromScale + end.w * toScale
        ).normalized()
    }
}

internal data class ModelMatrix3D(
    val values: DoubleArray
) {
    operator fun times(other: ModelMatrix3D): ModelMatrix3D {
        val result = DoubleArray(16)
        for (column in 0 until 4) {
            for (row in 0 until 4) {
                result[column * 4 + row] =
                    values[0 * 4 + row] * other.values[column * 4 + 0] +
                    values[1 * 4 + row] * other.values[column * 4 + 1] +
                    values[2 * 4 + row] * other.values[column * 4 + 2] +
                    values[3 * 4 + row] * other.values[column * 4 + 3]
            }
        }
        return ModelMatrix3D(result)
    }

    fun transformPoint(point: Vec3): Vec3 {
        return Vec3(
            x = values[0] * point.x + values[4] * point.y + values[8] * point.z + values[12],
            y = values[1] * point.x + values[5] * point.y + values[9] * point.z + values[13],
            z = values[2] * point.x + values[6] * point.y + values[10] * point.z + values[14]
        )
    }

    fun transformVector(vector: Vec3): Vec3 {
        return Vec3(
            x = values[0] * vector.x + values[4] * vector.y + values[8] * vector.z,
            y = values[1] * vector.x + values[5] * vector.y + values[9] * vector.z,
            z = values[2] * vector.x + values[6] * vector.y + values[10] * vector.z
        )
    }

    fun toMat4(): Mat4 {
        return Mat4(FloatArray(16) { index -> values[index].toFloat() })
    }

    companion object {
        fun identity(): ModelMatrix3D {
            return ModelMatrix3D(
                doubleArrayOf(
                    1.0, 0.0, 0.0, 0.0,
                    0.0, 1.0, 0.0, 0.0,
                    0.0, 0.0, 1.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
                )
            )
        }

        fun fromTransform(
            translation: Vec3,
            rotation: ModelQuaternion3D,
            scale: Vec3
        ): ModelMatrix3D {
            val quat = rotation.normalized()
            val xx = quat.x * quat.x
            val yy = quat.y * quat.y
            val zz = quat.z * quat.z
            val xy = quat.x * quat.y
            val xz = quat.x * quat.z
            val yz = quat.y * quat.z
            val wx = quat.w * quat.x
            val wy = quat.w * quat.y
            val wz = quat.w * quat.z
            val m00 = 1.0 - 2.0 * (yy + zz)
            val m01 = 2.0 * (xy + wz)
            val m02 = 2.0 * (xz - wy)
            val m10 = 2.0 * (xy - wz)
            val m11 = 1.0 - 2.0 * (xx + zz)
            val m12 = 2.0 * (yz + wx)
            val m20 = 2.0 * (xz + wy)
            val m21 = 2.0 * (yz - wx)
            val m22 = 1.0 - 2.0 * (xx + yy)
            return ModelMatrix3D(
                doubleArrayOf(
                    m00 * scale.x, m01 * scale.x, m02 * scale.x, 0.0,
                    m10 * scale.y, m11 * scale.y, m12 * scale.y, 0.0,
                    m20 * scale.z, m21 * scale.z, m22 * scale.z, 0.0,
                    translation.x, translation.y, translation.z, 1.0
                )
            )
        }
    }
}

internal fun sampleModelNodeWorldMatrices3D(
    nodes: List<ModelNode3D>,
    sceneNodeIndices: List<Int>,
    animations: List<ModelAnimationClip3D>,
    clipIndex: Int,
    timeSeconds: Double
): List<ModelMatrix3D> {
    val nodeTransforms = nodes.map { it.transform }.toMutableList()
    animations.getOrNull(clipIndex)?.sampleInto(timeSeconds, nodeTransforms)

    val localMatrices = nodes.mapIndexed { index, node ->
        if (nodeTransforms[index] == node.transform) {
            node.matrix
        } else {
            nodeTransforms[index].matrix()
        }
    }
    val worldMatrices = MutableList(nodes.size) { ModelMatrix3D.identity() }

    fun visit(
        nodeIndex: Int,
        parentTransform: ModelMatrix3D
    ) {
        val node = nodes.getOrNull(nodeIndex) ?: return
        val worldTransform = parentTransform * localMatrices[nodeIndex]
        worldMatrices[nodeIndex] = worldTransform
        node.children.forEach { childIndex -> visit(childIndex, worldTransform) }
    }

    sceneNodeIndices.forEach { nodeIndex -> visit(nodeIndex, ModelMatrix3D.identity()) }
    return worldMatrices
}

internal fun skinMatricesForModelSkin3D(
    skin: ModelSkin3D,
    nodeWorldMatrices: List<ModelMatrix3D>
): List<ModelMatrix3D> {
    return skin.joints.mapIndexed { jointOffset, nodeIndex ->
        val jointWorld = nodeWorldMatrices.getOrNull(nodeIndex) ?: ModelMatrix3D.identity()
        jointWorld * skin.inverseBindMatrices.getOrElse(jointOffset) { ModelMatrix3D.identity() }
    }
}

private fun addVectors(
    a: Vec3,
    b: Vec3
): Vec3 {
    return Vec3(a.x + b.x, a.y + b.y, a.z + b.z)
}

private fun scaleVector(
    value: Vec3,
    scale: Double
): Vec3 {
    return Vec3(value.x * scale, value.y * scale, value.z * scale)
}

private fun normalizeVector(value: Vec3): Vec3 {
    val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
    if (length == 0.0) {
        return Vec3(0.0, 1.0, 0.0)
    }
    return Vec3(value.x / length, value.y / length, value.z / length)
}
