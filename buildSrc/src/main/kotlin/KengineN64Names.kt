fun kengineN64TaskPrefix(artifactBaseName: String): String {
    return artifactBaseName
        .split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotEmpty() }
        .joinToString("") { part ->
            part.replaceFirstChar { char -> char.uppercase() }
        }
        .ifEmpty { "Game" }
}

fun kengineN64BuildTaskName(artifactBaseName: String): String {
    return "build${kengineN64TaskPrefix(artifactBaseName)}Z64"
}

fun kengineN64DockerBuildTaskName(artifactBaseName: String): String {
    return "build${kengineN64TaskPrefix(artifactBaseName)}Z64Docker"
}
