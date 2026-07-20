package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GpuUniforms3DTest {
    @Test
    fun modelViewProjectionUniformsUseCameraAspectAndModelMatrix() {
        val camera = FixedCamera(Mat4.identity())
        val modelMatrix = Mat4.translation(Vec3(1.0, 2.0, 3.0))

        val uniforms = modelViewProjectionUniforms3D(
            aspect = 1.5f,
            modelMatrix = modelMatrix,
            camera = camera
        )

        assertEquals(1.5f, camera.lastAspect)
        assertContentEquals(modelMatrix.values, uniforms)
    }

    @Test
    fun modelAndModelViewProjectionUniformsPackMatricesInShaderOrder() {
        val camera = FixedCamera(Mat4.identity())
        val modelMatrix = Mat4.translation(Vec3(1.0, 2.0, 3.0))

        val uniforms = modelAndModelViewProjectionUniforms3D(
            aspect = 1f,
            modelMatrix = modelMatrix,
            camera = camera
        )

        assertEquals(32, uniforms.size)
        assertContentEquals(modelMatrix.values, uniforms.copyOfRange(0, 16))
        assertContentEquals(modelMatrix.values, uniforms.copyOfRange(16, 32))
    }

    @Test
    fun coloredModelViewProjectionUniformsAppendNormalizedColor() {
        val camera = FixedCamera(Mat4.identity())
        val modelMatrix = Mat4.identity()
        val color = Color.fromHex("804020bf")

        val uniforms = coloredModelViewProjectionUniforms3D(
            aspect = 1f,
            modelMatrix = modelMatrix,
            camera = camera,
            color = color
        )

        assertEquals(20, uniforms.size)
        assertContentEquals(modelMatrix.values, uniforms.copyOfRange(0, 16))
        assertEquals(128f / 255f, uniforms[16])
        assertEquals(64f / 255f, uniforms[17])
        assertEquals(32f / 255f, uniforms[18])
        assertEquals(191f / 255f, uniforms[19])
    }

    @Test
    fun skinnedModelUniformsPadMissingSkinMatricesWithIdentity() {
        val camera = FixedCamera(Mat4.identity())
        val modelMatrix = Mat4.translation(Vec3(1.0, 2.0, 3.0))
        val skinMatrix = Mat4.scale(Vec3(2.0, 3.0, 4.0))

        val uniforms = skinnedModelUniforms3D(
            aspect = 1f,
            modelMatrix = modelMatrix,
            camera = camera,
            skinMatrices = listOf(skinMatrix),
            maxSkinJoints = 2
        )

        assertEquals(64, uniforms.size)
        assertContentEquals(modelMatrix.values, uniforms.copyOfRange(0, 16))
        assertContentEquals(modelMatrix.values, uniforms.copyOfRange(16, 32))
        assertContentEquals(skinMatrix.values, uniforms.copyOfRange(32, 48))
        assertContentEquals(Mat4.identity().values, uniforms.copyOfRange(48, 64))
    }

    @Test
    fun skinnedModelUniformsRejectNegativeJointLimit() {
        assertFailsWith<IllegalArgumentException> {
            skinnedModelUniforms3D(
                aspect = 1f,
                modelMatrix = Mat4.identity(),
                camera = FixedCamera(Mat4.identity()),
                skinMatrices = emptyList(),
                maxSkinJoints = -1
            )
        }
    }

    @Test
    fun directionalLightUniformsUseShaderOrder() {
        val light = DirectionalLight3D(
            direction = Vec3(1.0, 2.0, 3.0),
            color = Color.fromHex("804020"),
            ambientStrength = 0.25f,
            diffuseStrength = 0.75f
        )

        assertContentEquals(
            floatArrayOf(
                1f,
                2f,
                3f,
                0.25f,
                128f / 255f,
                64f / 255f,
                32f / 255f,
                0.75f
            ),
            directionalLightUniforms3D(light)
        )
    }

    @Test
    fun texturedDirectionalLightUniformsAppendMaterialFlags() {
        val light = DirectionalLight3D(
            direction = Vec3(1.0, 2.0, 3.0),
            color = Color.fromHex("804020"),
            ambientStrength = 0.25f,
            diffuseStrength = 0.75f
        )

        assertContentEquals(
            floatArrayOf(
                1f,
                2f,
                3f,
                0.25f,
                128f / 255f,
                64f / 255f,
                32f / 255f,
                0.75f,
                1f,
                0f,
                0f,
                0f
            ),
            texturedDirectionalLightUniforms3D(light, useNormalTexture = true)
        )
    }

    private class FixedCamera(
        private val matrix: Mat4
    ) : Camera3D {
        var lastAspect: Float? = null

        override fun viewProjection(aspect: Float): Mat4 {
            lastAspect = aspect
            return matrix
        }
    }
}
