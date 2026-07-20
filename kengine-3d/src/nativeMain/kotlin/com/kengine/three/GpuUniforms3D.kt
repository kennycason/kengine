package com.kengine.three

import com.kengine.graphics.Color

internal const val MAT4_FLOATS_3D = 16

internal fun modelViewProjectionUniforms3D(
    aspect: Float,
    modelMatrix: Mat4,
    camera: Camera3D
): FloatArray {
    return (camera.viewProjection(aspect) * modelMatrix).values.copyOf()
}

internal fun modelAndModelViewProjectionUniforms3D(
    aspect: Float,
    modelMatrix: Mat4,
    camera: Camera3D
): FloatArray {
    val modelViewProjection = camera.viewProjection(aspect) * modelMatrix
    val uniforms = FloatArray(MAT4_FLOATS_3D * 2)
    modelViewProjection.values.copyInto(uniforms, destinationOffset = 0)
    modelMatrix.values.copyInto(uniforms, destinationOffset = MAT4_FLOATS_3D)
    return uniforms
}

internal fun coloredModelViewProjectionUniforms3D(
    aspect: Float,
    modelMatrix: Mat4,
    camera: Camera3D,
    color: Color
): FloatArray {
    val uniforms = FloatArray(MAT4_FLOATS_3D + 4)
    modelViewProjectionUniforms3D(aspect, modelMatrix, camera).copyInto(uniforms, destinationOffset = 0)
    uniforms[MAT4_FLOATS_3D] = color.r.toFloat() / 255f
    uniforms[MAT4_FLOATS_3D + 1] = color.g.toFloat() / 255f
    uniforms[MAT4_FLOATS_3D + 2] = color.b.toFloat() / 255f
    uniforms[MAT4_FLOATS_3D + 3] = color.a.toFloat() / 255f
    return uniforms
}

internal fun skinnedModelUniforms3D(
    aspect: Float,
    modelMatrix: Mat4,
    camera: Camera3D,
    skinMatrices: List<Mat4>,
    maxSkinJoints: Int
): FloatArray {
    require(maxSkinJoints >= 0) {
        "maxSkinJoints must be non-negative."
    }

    val uniforms = FloatArray(MAT4_FLOATS_3D * 2 + maxSkinJoints * MAT4_FLOATS_3D)
    modelAndModelViewProjectionUniforms3D(aspect, modelMatrix, camera).copyInto(uniforms, destinationOffset = 0)

    val identity = Mat4.identity()
    for (index in 0 until maxSkinJoints) {
        val matrix = skinMatrices.getOrNull(index) ?: identity
        matrix.values.copyInto(
            uniforms,
            destinationOffset = MAT4_FLOATS_3D * 2 + index * MAT4_FLOATS_3D
        )
    }

    return uniforms
}

internal fun directionalLightUniforms3D(light: DirectionalLight3D): FloatArray {
    return floatArrayOf(
        light.direction.x.toFloat(),
        light.direction.y.toFloat(),
        light.direction.z.toFloat(),
        light.ambientStrength,
        light.color.r.toFloat() / 255f,
        light.color.g.toFloat() / 255f,
        light.color.b.toFloat() / 255f,
        light.diffuseStrength
    )
}
