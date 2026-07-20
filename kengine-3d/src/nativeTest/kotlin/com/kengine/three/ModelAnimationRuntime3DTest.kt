package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelAnimationRuntime3DTest {
    @Test
    fun samplesAnimatedNodeWorldMatrices() {
        val nodes = listOf(
            testNode(
                children = listOf(1),
                transform = testTransform(translation = Vec3(5.0, 0.0, 0.0))
            ),
            testNode(
                transform = testTransform(translation = Vec3(1.0, 0.0, 0.0))
            )
        )
        val clip = ModelAnimationClip3D(
            info = GlbAnimationClipInfo(
                name = "Move",
                durationSeconds = 1.0,
                channelCount = 1,
                channels = emptyList()
            ),
            channels = listOf(
                ModelAnimationChannel3D(
                    targetNodeIndex = 1,
                    path = ModelAnimationChannelPath3D.TRANSLATION,
                    sampler = ModelAnimationValueSampler3D.Vec3Sampler(
                        times = listOf(0.0, 1.0),
                        values = listOf(Vec3(1.0, 0.0, 0.0), Vec3(3.0, 0.0, 0.0)),
                        interpolation = "LINEAR"
                    )
                )
            ),
            durationSeconds = 1.0
        )

        val worldMatrices = sampleModelNodeWorldMatrices3D(
            nodes = nodes,
            sceneNodeIndices = listOf(0),
            animations = listOf(clip),
            clipIndex = 0,
            timeSeconds = 1.5
        )

        assertEquals(Vec3(5.0, 0.0, 0.0), worldMatrices[0].transformPoint(Vec3(0.0, 0.0, 0.0)))
        assertEquals(Vec3(7.0, 0.0, 0.0), worldMatrices[1].transformPoint(Vec3(0.0, 0.0, 0.0)))
    }

    @Test
    fun skinsTexturedLitSourceVertexWithWeightedMatrices() {
        val vertex = SkinnedTexturedLitVertexSource3D(
            position = Vec3(1.0, 0.0, 0.0),
            normal = Vec3(1.0, 0.0, 0.0),
            color = Color.fromHex("ff8000"),
            u = 0.25f,
            v = 0.75f,
            joints = SkinJointIndicesSource3D(intArrayOf(0, 1, 0, 0)),
            weights = SkinJointWeightsSource3D(doubleArrayOf(0.25, 0.75, 0.0, 0.0))
        )

        val skinned = vertex.toTexturedVertex(
            skinMatrices = listOf(
                ModelMatrix3D.identity(),
                testMatrix(translation = Vec3(3.0, 0.0, 0.0))
            )
        )

        assertEquals(Vec3(3.25, 0.0, 0.0), skinned.position)
        assertEquals(Vec3(1.0, 0.0, 0.0), skinned.normal)
        assertEquals(Color.fromHex("ff8000"), skinned.color)
        assertEquals(0.25f, skinned.u)
        assertEquals(0.75f, skinned.v)
    }

    private fun testNode(
        children: List<Int> = emptyList(),
        transform: ModelNodeTransform3D = testTransform()
    ): ModelNode3D {
        return ModelNode3D(
            name = null,
            meshIndex = null,
            skinIndex = null,
            children = children,
            transform = transform,
            matrix = transform.matrix()
        )
    }

    private fun testTransform(
        translation: Vec3 = Vec3(0.0, 0.0, 0.0),
        rotation: ModelQuaternion3D = ModelQuaternion3D(0.0, 0.0, 0.0, 1.0),
        scale: Vec3 = Vec3(1.0, 1.0, 1.0)
    ): ModelNodeTransform3D {
        return ModelNodeTransform3D(
            translation = translation,
            rotation = rotation,
            scale = scale
        )
    }

    private fun testMatrix(translation: Vec3): ModelMatrix3D {
        return ModelMatrix3D.fromTransform(
            translation = translation,
            rotation = ModelQuaternion3D(0.0, 0.0, 0.0, 1.0),
            scale = Vec3(1.0, 1.0, 1.0)
        )
    }
}
