import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ViewerFilePickerTest {
    @Test
    fun glbFileUsesAutoMode() {
        val preset = viewerModelPresetForFile("/tmp/models/My Demo Model.glb")

        assertEquals("My Demo Model", preset.label)
        assertEquals("/tmp/models/My Demo Model.glb", preset.modelPath)
        assertEquals(ViewerModelMode.AUTO, preset.mode)
        assertEquals(DEFAULT_TARGET_SIZE, preset.targetSize)
    }

    @Test
    fun objFileUsesStaticMode() {
        val preset = viewerModelPresetForFile("C:\\models\\ship.obj")

        assertEquals("ship", preset.label)
        assertEquals("C:\\models\\ship.obj", preset.modelPath)
        assertEquals(ViewerModelMode.STATIC, preset.mode)
        assertEquals(DEFAULT_TARGET_SIZE, preset.targetSize)
    }

    @Test
    fun unsupportedFileExtensionFails() {
        assertFailsWith<IllegalArgumentException> {
            viewerModelPresetForFile("/tmp/models/ship.fbx")
        }
    }
}
