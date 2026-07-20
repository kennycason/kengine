package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3

data class SkinJointIndices3D(
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0,
    val w: Int = 0
) {
    init {
        require(x >= 0 && y >= 0 && z >= 0 && w >= 0) {
            "Skin joint indices must be non-negative."
        }
    }

    fun valueAt(index: Int): Int {
        return when (index) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IndexOutOfBoundsException("Skin joint index component out of range: $index")
        }
    }
}

data class SkinJointWeights3D(
    val x: Double = 1.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val w: Double = 0.0
) {
    init {
        require(x >= 0.0 && y >= 0.0 && z >= 0.0 && w >= 0.0) {
            "Skin joint weights must be non-negative."
        }
    }

    val total: Double
        get() = x + y + z + w

    fun valueAt(index: Int): Double {
        return when (index) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IndexOutOfBoundsException("Skin joint weight component out of range: $index")
        }
    }

    fun normalized(): SkinJointWeights3D {
        val sum = total
        if (sum <= 0.0) {
            return this
        }

        return SkinJointWeights3D(
            x = x / sum,
            y = y / sum,
            z = z / sum,
            w = w / sum
        )
    }
}

data class SkinnedTexturedLitVertex3D(
    val position: Vec3,
    val normal: Vec3,
    val color: Color,
    val u: Float,
    val v: Float,
    val joints: SkinJointIndices3D = SkinJointIndices3D(),
    val weights: SkinJointWeights3D = SkinJointWeights3D()
) {
    fun maxWeightedJointIndex(): Int {
        var maxJoint = 0
        for (index in 0 until 4) {
            if (weights.valueAt(index) > 0.0) {
                maxJoint = maxOf(maxJoint, joints.valueAt(index))
            }
        }
        return maxJoint
    }

    fun writeTo(values: FloatArray, offset: Int) {
        values[offset] = position.x.toFloat()
        values[offset + 1] = position.y.toFloat()
        values[offset + 2] = position.z.toFloat()
        values[offset + 3] = normal.x.toFloat()
        values[offset + 4] = normal.y.toFloat()
        values[offset + 5] = normal.z.toFloat()
        values[offset + 6] = color.r.toFloat() / 255f
        values[offset + 7] = color.g.toFloat() / 255f
        values[offset + 8] = color.b.toFloat() / 255f
        values[offset + 9] = u
        values[offset + 10] = v
        values[offset + 11] = joints.x.toFloat()
        values[offset + 12] = joints.y.toFloat()
        values[offset + 13] = joints.z.toFloat()
        values[offset + 14] = joints.w.toFloat()
        values[offset + 15] = weights.x.toFloat()
        values[offset + 16] = weights.y.toFloat()
        values[offset + 17] = weights.z.toFloat()
        values[offset + 18] = weights.w.toFloat()
    }

    companion object {
        const val FLOATS_PER_VERTEX = 19
        const val BYTES_PER_VERTEX = FLOATS_PER_VERTEX * 4
    }
}
