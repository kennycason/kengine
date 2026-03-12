object PlatformConfig {
    val hostOs: String = System.getProperty("os.name")
    val isMacOS: Boolean = hostOs == "Mac OS X"
    val isLinux: Boolean = hostOs == "Linux"

    val includePaths: List<String> = when {
        isMacOS -> listOf("/opt/homebrew/include", "/usr/local/include")
        isLinux -> listOf("/usr/local/include", "/usr/include", "/usr/include/x86_64-linux-gnu")
        else -> listOf("/usr/local/include")
    }

    val libPaths: List<String> = when {
        isMacOS -> listOf("/opt/homebrew/lib", "/usr/local/lib")
        isLinux -> listOf("/usr/local/lib", "/usr/lib/x86_64-linux-gnu", "/usr/lib")
        else -> listOf("/usr/local/lib")
    }

    val compilerOpts: List<String>
        get() = includePaths.map { "-I$it" }

    val libPathOpts: List<String>
        get() = libPaths.map { "-L$it" }

    val rpathOpts: List<String>
        get() = when {
            isMacOS -> listOf(
                "-Wl,-rpath,@executable_path/Frameworks",
                "-Wl,-rpath,/usr/local/lib",
                "-Wl,-rpath,/opt/homebrew/lib"
            )
            isLinux -> listOf(
                "-Wl,-rpath,\$ORIGIN/lib",
                "-Wl,-rpath,/usr/local/lib"
            )
            else -> emptyList()
        }

    val macFrameworks: List<String>
        get() = if (isMacOS) listOf(
            "-framework", "Cocoa",
            "-framework", "IOKit",
            "-framework", "CoreVideo",
            "-framework", "CoreAudio",
            "-framework", "AudioToolbox"
        ) else emptyList()

    val linuxSystemLibs: List<String>
        get() = if (isLinux) listOf(
            "-lpthread", "-ldl", "-lm"
        ) else emptyList()

    fun sharedLibLinkerOpts(vararg libs: String): List<String> =
        libPathOpts + libs.map { "-l$it" } + macFrameworks + linuxSystemLibs + rpathOpts

    val sharedLibExt: String = if (isMacOS) "dylib" else "so"
}
