# Kengine Development Guidelines

## Project Overview
Kengine is a **Kotlin/Native 2D game engine** built on SDL3. It uses a React-inspired functional hooks architecture for state management and context-based dependency injection. The engine compiles to native binaries on macOS (arm64/x64), Linux (arm64/x64), and Windows (MINGW x64).

## Module Structure
```
kengine/              Core engine (graphics, input, entity, UI, maps, particles, math)
kengine-reactive/     Hooks system (useState, useEffect, useContext, useMemo, useReducer)
kengine-test/         Fluent assertion testing framework
kengine-network/      Networking via SDL3_net
kengine-sound/        Audio synthesis/playback via SDL3_mixer
kengine-physics/      2D physics via Chipmunk bindings
kengine-playdate/     Playdate console port (ARM32, blocked: Kotlin/Native can't target Cortex-M7)
games/                8 example games (antfarm, boxxle, helloworld, hextris, etc.)
sdl3/                 SDL3 submodules and build script (build_sdl.sh)
packaging/            Icons and packaging resources (kengine.icns, kengine.png)
buildSrc/             Custom Gradle plugins (PlatformConfig, KengineAssetPlugin, SdlDylibPlugin, KenginePackagingPlugin)
```

## Key Architecture Concepts
- **Game interface**: implement `update()`, `draw()`, `cleanup()`
- **GameRunner**: bootstraps the game loop with a target frame rate
- **GameContext**: central singleton holding all subsystem contexts
- **Context pattern**: each subsystem (SDL, graphics, input, clock, etc.) is a Context; accessed via `useXxxContext {}` or `getXxxContext()`
- **Hooks**: React-like `useState`, `useEffect`, `useMemo`, `useReducer` from kengine-reactive
- **SDL3 C interop**: Kotlin/Native cinterop .def files in `kengine/src/nativeInterop/cinterop/`

## Build Commands
- Build project: `./gradlew clean build`
- Build (skip tests): `./gradlew clean build -x allTests -x macosArm64Test -x jvmTest -x jsTest`
- Run all tests: `./gradlew allTests`
- Run native tests: `./gradlew macosArm64Test`
- Run single test: `./gradlew macosArm64Test --tests "com.kengine.test.YourTestClass.yourTestMethod"`
- Compile test binary: `./gradlew :kengine:linkDebugTestMacosArm64`
- Run test binary directly: `./kengine/build/bin/macosArm64/debugTest/test.kexe`
- Run tests excluding ITs: `./kengine/build/bin/macosArm64/debugTest/test.kexe "--ktest_filter=*-*IT.*:*TiledMapLoaderTest.*"`
- Build SDL3 from source: `bash sdl3/build_sdl.sh`
- Run a specific game: `./gradlew :games:helloworld:runDebugExecutableMacosArm64`
- Package macOS .app: `./gradlew :games:helloworld:packageMac`
- Package Linux tarball: `./gradlew :games:helloworld:packageLinuxTarball`
- Package Windows dir: `./gradlew :games:helloworld:packageWindows`

## Test Environment
Tests requiring SDL use dummy drivers:
```
SDL_VIDEODRIVER=dummy SDL_AUDIODRIVER=dummy ./kengine/build/bin/macosArm64/debugTest/test.kexe
```
Tests are under `<module>/src/nativeTest/kotlin/`. Integration tests are suffixed `IT` and excluded from CI by default.

## Dependencies
- **Kotlin**: 2.1.10 (Multiplatform)
- **JDK**: 17
- **kotlinx-serialization-json**: 1.8.0
- **kotlinx-coroutines-core**: 1.8.0
- **SDL3**: Built from source (submodules in sdl3/)
- **Chipmunk**: System package (physics)

## Platform-Specific Notes
- **macOS**: SDL3 libs from Homebrew (SDL3, SDL3_image, SDL3_ttf, SDL3_mixer) + source build for SDL3_net. Frameworks: Cocoa, IOKit, CoreVideo, CoreAudio
- **Linux**: All SDL3 libs built from source. Needs X11/Wayland/audio dev packages. Uses LD_LIBRARY_PATH=/usr/local/lib
- **Windows**: MSYS2/MINGW64. Requires Konan sysroot patching (CRT lib replacement). DLLs must be adjacent to executables

## Code Style Guidelines
- **Naming**: camelCase for variables/functions, PascalCase for classes, UPPER_SNAKE_CASE for constants
- **Organization**: group related functionality in packages (entity, graphics, input, sound)
- **Error handling**: use logger for reporting errors, return null or default values when appropriate
- **Imports**: avoid wildcard imports, organize stdlib first then third-party
- **Functional style**: prefer functional hooks (useContext, useState) over inheritance
- **Nullability**: favor non-nullable types, use safe call operators (`?.`) when needed
- **Testing**: new functionality should have corresponding tests

## Code Comments
- Comments should explain WHY code exists, not WHAT it does
- Never add comments describing changes made to the code (use git history)
- Configuration files should not include change tracking comments
- Do not add trivial comments that repeat what the code already clearly states
- Write KDoc for public APIs, focusing on usage not implementation details

## Source Layout Convention
Each module follows this structure:
```
<module>/
  build.gradle.kts
  src/
    nativeMain/kotlin/com/kengine/...    # Main source
    nativeTest/kotlin/com/kengine/...    # Tests
    nativeInterop/cinterop/              # C interop .def files (if applicable)
  README.md                              # Module-specific docs (if applicable)
```

## CI/CD
GitHub Actions (`.github/workflows/build.yml`) runs on push/PR to main:
- **macOS** (macos-14, arm64): Homebrew + source-built SDL3
- **Linux** (ubuntu-24.04): All SDL3 from source
- **Windows** (windows-latest, MSYS2): Source-built SDL3 + Konan sysroot patching

## Current Status & Known Issues
- **WASM target**: Branch `wasm-target` exists but contains no work. Would require SDL3 replacement with WebGL/Canvas
- **Playdate**: Blocked — Kotlin/Native linuxArm32Hfp emits ARMv4, Playdate requires ARMv7E-M (Cortex-M7). Opt-in via `-Pkengine.playdate=true`
- **Nintendo Switch**: Blocked on libnx SDL3 support (currently SDL2 only)
- **Windows CI**: Fragile due to Konan/MINGW CRT compatibility; required 9 iterations to stabilize
