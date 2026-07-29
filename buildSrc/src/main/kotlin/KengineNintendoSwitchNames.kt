fun kengineNintendoSwitchTaskPrefix(artifactBaseName: String): String {
    return artifactBaseName
        .split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotEmpty() }
        .joinToString("") { part ->
            part.replaceFirstChar { char -> char.uppercase() }
        }
        .ifEmpty { "Game" }
}

fun kengineNintendoSwitchBuildTaskName(artifactBaseName: String): String {
    return "build${kengineNintendoSwitchTaskPrefix(artifactBaseName)}Nro"
}
