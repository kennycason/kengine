object PlatformConfig {
    val hostOs: String = System.getProperty("os.name")
    val isMacOS: Boolean = hostOs == "Mac OS X"
    val isLinux: Boolean = hostOs == "Linux"
    val isWindows: Boolean = hostOs.startsWith("Windows")

    // MSYS2 mingw64 root — standard location on GitHub Actions and local installs
    private val msys2Root: String = System.getenv("MSYS2_ROOT") ?: "C:/msys64"
    private val mingw64: String = "$msys2Root/mingw64"

    val includePaths: List<String> = when {
        isMacOS -> listOf("/opt/homebrew/include", "/usr/local/include")
        isLinux -> listOf("/usr/local/include", "/usr/include", "/usr/include/x86_64-linux-gnu")
        isWindows -> listOf("$mingw64/include")
        else -> listOf("/usr/local/include")
    }

    val libPaths: List<String> = when {
        isMacOS -> listOf("/opt/homebrew/lib", "/usr/local/lib")
        isLinux -> listOf("/usr/local/lib", "/usr/lib/x86_64-linux-gnu", "/usr/lib")
        isWindows -> listOf("$mingw64/lib")
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
            "-Wl,--allow-shlib-undefined",
            "-lpthread", "-ldl", "-lm"
        ) else emptyList()

    val windowsSystemLibs: List<String>
        get() = if (isWindows) listOf(
            "-lmingw32", "-lole32", "-loleaut32", "-limm32",
            "-lwinmm", "-lgdi32", "-lsetupapi", "-lversion"
        ) else emptyList()

    fun sharedLibLinkerOpts(vararg libs: String): List<String> =
        libPathOpts + libs.map { "-l$it" } +
            macFrameworks + linuxSystemLibs + windowsSystemLibs + rpathOpts

    val sharedLibExt: String = when {
        isMacOS -> "dylib"
        isWindows -> "dll"
        else -> "so"
    }
}
