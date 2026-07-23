import com.kengine.three.ModelFormat3D
import com.kengine.three.ModelInfo3D
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewerModelModeTest {
    @Test
    fun autoModeTreatsSkinWithoutAnimationsAsStatic() {
        val info = modelInfo(skinCount = 1, animationCount = 0)

        assertEquals(ViewerModelMode.STATIC, viewerAutoModeForModel(info))
    }

    @Test
    fun autoModeTreatsSkinnedAnimationsAsSkinned() {
        val info = modelInfo(skinCount = 1, animationCount = 2)

        assertEquals(ViewerModelMode.SKINNED, viewerAutoModeForModel(info))
    }

    @Test
    fun autoModeTreatsNodeAnimationsAsNodeAnimated() {
        val info = modelInfo(skinCount = 0, animationCount = 2)

        assertEquals(ViewerModelMode.NODE_ANIMATED, viewerAutoModeForModel(info))
    }

    private fun modelInfo(
        skinCount: Int,
        animationCount: Int
    ): ModelInfo3D {
        return ModelInfo3D(
            assetPath = "models/test.glb",
            format = ModelFormat3D.GLB,
            vertexCount = 0,
            skinCount = skinCount,
            animationCount = animationCount
        )
    }
}
