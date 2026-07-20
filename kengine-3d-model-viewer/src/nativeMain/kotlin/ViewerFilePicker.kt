expect fun chooseViewerModelFile(): String?

fun viewerModelPresetForFile(path: String): ViewerModelPreset {
    val fileName = path.substringAfterLast('/').substringAfterLast('\\')
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val mode = when (extension) {
        "obj" -> ViewerModelMode.STATIC
        "glb" -> ViewerModelMode.AUTO
        else -> throw IllegalArgumentException("Unsupported model file: $fileName")
    }
    return ViewerModelPreset(
        label = fileName.substringBeforeLast('.', fileName).ifBlank { "Selected model" },
        modelPath = path,
        mode = mode,
        targetSize = DEFAULT_TARGET_SIZE
    )
}
