package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3

data class DirectionalLight3D(
    val direction: Vec3 = Vec3(-0.35, -0.75, -0.55),
    val color: Color = Color.fromHex("ffffff"),
    val ambientStrength: Float = 0.28f,
    val diffuseStrength: Float = 0.78f
)
