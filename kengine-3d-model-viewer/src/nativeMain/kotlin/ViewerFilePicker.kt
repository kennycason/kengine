expect fun chooseViewerModelFile(): String?

private const val VIEWER_SUPPORTED_MODEL_FORMATS = "GLB, GLTF, OBJ"

fun viewerModelPresetForFile(path: String): ViewerModelPreset {
    val fileName = path.substringAfterLast('/').substringAfterLast('\\')
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val mode = when (extension) {
        "obj" -> ViewerModelMode.STATIC
        "glb", "gltf" -> ViewerModelMode.AUTO
        else -> throw IllegalArgumentException("Unsupported model file: $fileName")
    }
    return ViewerModelPreset(
        label = fileName.substringBeforeLast('.', fileName).ifBlank { "Selected model" },
        modelPath = path,
        mode = mode,
        targetSize = DEFAULT_TARGET_SIZE
    )
}

fun viewerModelLoadStatus(error: Throwable): String {
    return "Error loading model: ${viewerModelLoadSummary(error)}"
}

fun viewerModelLoadDetails(error: Throwable): String {
    return viewerModelLoadSummary(error, maxLength = Int.MAX_VALUE)
}

private fun viewerModelLoadSummary(
    error: Throwable,
    maxLength: Int = 96
): String {
    val rawMessage = firstUsefulErrorMessage(error)
    val message = if (rawMessage.isUnsupportedFormatError()) {
        "$rawMessage. Supported formats: $VIEWER_SUPPORTED_MODEL_FORMATS."
    } else {
        rawMessage
    }
    return message.limitLength(maxLength)
}

private fun firstUsefulErrorMessage(error: Throwable): String {
    var current: Throwable? = error
    var remaining = 6
    while (current != null && remaining > 0) {
        val message = current.message?.trim()
        if (!message.isNullOrBlank()) {
            return message
        }
        current = current.cause
        remaining -= 1
    }
    return "unknown error"
}

private fun String.isUnsupportedFormatError(): Boolean {
    return startsWith("Unsupported model file:") ||
        startsWith("Unsupported 3D model format for asset:")
}

private fun String.limitLength(maxLength: Int): String {
    if (length <= maxLength) {
        return this
    }
    return take(maxLength).trimEnd() + "..."
}
