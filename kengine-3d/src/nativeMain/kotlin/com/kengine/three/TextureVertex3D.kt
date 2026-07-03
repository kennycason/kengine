package com.kengine.three

import com.kengine.math.Vec3

data class TextureVertex3D(
    val position: Vec3,
    val u: Float,
    val v: Float
) {
    fun writeTo(values: FloatArray, offset: Int) {
        values[offset] = position.x.toFloat()
        values[offset + 1] = position.y.toFloat()
        values[offset + 2] = position.z.toFloat()
        values[offset + 3] = u
        values[offset + 4] = v
    }

    companion object {
        const val FLOATS_PER_VERTEX = 5
        const val BYTES_PER_VERTEX = FLOATS_PER_VERTEX * 4
    }
}
