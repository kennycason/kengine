object KengineHostTarget {
    val name: String = when {
        PlatformConfig.isMacOS && System.getProperty("os.arch") == "aarch64" -> "macosArm64"
        PlatformConfig.isMacOS -> "macosX64"
        PlatformConfig.isLinux && System.getProperty("os.arch") == "aarch64" -> "linuxArm64"
        PlatformConfig.isLinux -> "linuxX64"
        PlatformConfig.isWindows -> "mingwX64"
        else -> throw IllegalStateException("Host OS [${PlatformConfig.hostOs}] is not supported in Kotlin/Native.")
    }

    val taskSuffix: String = name.replaceFirstChar { it.uppercase() }

    fun binPath(vararg parts: String): String =
        listOf("bin", name, *parts).joinToString("/")
}
