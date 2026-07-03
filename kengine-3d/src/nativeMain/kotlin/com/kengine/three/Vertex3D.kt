package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3

data class Vertex3D(
    val position: Vec3,
    val color: Color
) {
    fun writeTo(values: FloatArray, offset: Int) {
        values[offset] = position.x.toFloat()
        values[offset + 1] = position.y.toFloat()
        values[offset + 2] = position.z.toFloat()
        values[offset + 3] = color.r.toFloat() / 255f
        values[offset + 4] = color.g.toFloat() / 255f
        values[offset + 5] = color.b.toFloat() / 255f
    }

    companion object {
        const val FLOATS_PER_VERTEX = 6
        const val BYTES_PER_VERTEX = FLOATS_PER_VERTEX * 4
    }
}
