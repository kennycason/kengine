import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    fun gltfFileUsesAutoMode() {
        val preset = viewerModelPresetForFile("/tmp/models/My Scene.gltf")

        assertEquals("My Scene", preset.label)
        assertEquals("/tmp/models/My Scene.gltf", preset.modelPath)
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

    @Test
    fun unsupportedFormatStatusListsSupportedFormats() {
        assertEquals(
            "Error loading model: Unsupported model file: ship.fbx. Supported formats: GLB, GLTF, OBJ.",
            viewerModelLoadStatus(IllegalArgumentException("Unsupported model file: ship.fbx"))
        )
    }

    @Test
    fun longLoadStatusIsShortButDetailsKeepTheFullMessage() {
        val details =
            "GLTF buffer 'triangle mesh.bin' was not found: /tmp/kengine/model folder/triangle mesh.bin referenced by /tmp/kengine/model folder/scene.gltf"

        val status = viewerModelLoadStatus(IllegalArgumentException(details))

        assertTrue(status.startsWith("Error loading model: GLTF buffer"))
        assertTrue(status.endsWith("..."))
        assertEquals(details, viewerModelLoadDetails(IllegalArgumentException(details)))
    }
}
