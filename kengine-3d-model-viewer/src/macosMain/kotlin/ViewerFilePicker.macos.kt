import platform.AppKit.NSModalResponseOK
import platform.AppKit.NSOpenPanel
import platform.UniformTypeIdentifiers.UTType

actual fun chooseViewerModelFile(): String? {
    val panel = NSOpenPanel.openPanel()
    panel.title = "Load 3D Model"
    panel.message = "Choose a GLB or OBJ model."
    panel.canChooseFiles = true
    panel.canChooseDirectories = false
    panel.allowsMultipleSelection = false
    panel.allowedContentTypes = listOfNotNull(
        UTType.typeWithFilenameExtension("glb"),
        UTType.typeWithFilenameExtension("obj")
    )

    if (panel.runModal() != NSModalResponseOK) {
        return null
    }
    return panel.URL?.path
}
