# kengine-nintendo-switch TODO

## Current Checkpoint

- C-only Switch homebrew build opens successfully in Ryujinx.
- Kotlin-linked Switch game builds compile, package as game-facing NROs, and launch successfully in Ryujinx.
- The current Kotlin probe now exercises:
  - repeated C-to-Kotlin calls at startup,
  - basic Kotlin allocation through `IntArray`,
  - Kotlin string creation returned to C as `const char*`,
  - C-side `DisposeString`,
  - periodic C-to-Kotlin calls from the libnx app loop.
- The current Switch lifecycle shell now exercises:
  - shared `com.kengine.Game` lifecycle contract from `:kengine-core`,
  - shared `com.kengine.PortableGame` and `com.kengine.input.InputState` from `:kengine-core`,
  - shared `com.kengine.render.RenderContext`, `RenderCommandBuffer`, and `RenderCommandType` from `:kengine-core`,
  - the pure Kotlin `:games:nintendo-switch-demo` game module,
  - regular `:kengine` usage of that same contract through an `api(project(":kengine-core"))` dependency,
  - Kotlin runtime startup from libnx,
  - per-frame Kotlin `update` and `draw` calls from the libnx app loop,
  - Kotlin-side game object state and virtual dispatch,
  - periodic runtime snapshots returned to C as strings,
  - Kotlin cleanup before framebuffer teardown and process exit.
- The Kotlin-linked NRO now uses the libnx software framebuffer instead of the text console:
  - C owns `Framebuffer` setup and presentation.
  - Kotlin owns the moving game-state square, palette, size, and render context.
  - The libnx host translates Switch buttons into shared `InputState`.
  - Kotlin copies each frame's render command list into a C-owned buffer with one ABI call.
  - `:games:nintendo-switch-demo:buildSwitchNro` now copies the backend output to `games/nintendo-switch-demo/build/switch/nintendo-switch-demo.nro`.
  - `:games:hextris-switch:buildSwitchNro` now builds a separate backend artifact and copies it to `games/hextris-switch/build/switch/hextris-switch.nro`.
  - C executes the copied render commands such as vertical gradient and filled rectangles.
  - The same `:games:nintendo-switch-demo` game also runs on desktop through `PortableGameAdapter` and `RenderContextSdlRenderer` in `:kengine`.
  - The desktop adapter maps arrow keys, WASD, D-pad, left stick, and controller face buttons into the same shared `InputState`.
  - libnx input is passed to Kotlin as a compact mask.
  - D-pad / left stick moves the square, `A`/`X`/`Y` exercise palette changes, `B` pulses size, `L`/`R` alter manual movement speed, `Minus` maps to select, and `Minus + Plus` exits.
  - Shared render commands now include filled rectangles, vertical gradients, lines, sprite draws, and bitmap text across both SDL and Switch framebuffer hosts.
  - Desktop sprite commands resolve through `PortableSpriteRegistry` into the existing Kengine `SpriteContext` / `TextureManager` path.
  - Desktop sprite-sheet commands resolve through the same registry into existing `SpriteSheet` tile selection using the shared render-command `frame` field.
  - Switch sprite commands currently render through a software-pattern sprite fallback; the game-facing API is ready for a later BMP/PNG decoder or prepacked pixel implementation.
  - Switch text commands fetch the frame-local Kotlin string by command index and render it with a built-in 5x7 software font.
  - Switch game selection now uses a generated Kotlin `PortableGame` factory per backend NRO task instead of hardcoding one game class in `KengineSwitchRuntime`.
  - The Switch command buffer is now sized for denser game screens such as Hextris' 15x25 board.
- The first Kotlin crash was fixed at the generated C API wrapper layer:
  - Old failure: invalid read at `0x28`.
  - Old bad instruction: wrapper used `mrs ..., tpidr_el0`.
  - Follow-up wrapper disassembly used `mrs ..., tpidrro_el0`.
- The second Kotlin TLS failure was fixed at the Kotlin runtime bitcode layer:
  - Last pre-fix Ryujinx log: `~/Library/Logs/Ryujinx/Ryujinx_1.3.3_2026-07-28_05-52-37.log`.
  - Fault: invalid access at `0x109`.
  - PC: `kengine-nintendo-switch:0x1c7f8`.
  - Mapped symbol: `kotlin::mm::ThreadSuspensionData::setState(kotlin::ThreadState)`.
  - Cause: Kotlin runtime bitcode in `kotlin-native/dist/konan/targets/switch_arm64/native` was stale and still used `mrs ..., tpidr_el0`.
- Rebuilt `:kotlin-native:runtime:switch_arm64Runtime`, refreshed the local Kotlin/Native dist with `:kotlin-native:switch_arm64CrossDistRuntime`, then rebuilt the NRO.
- The latest Ryujinx close-on-launch has been traced to Kotlin runtime TLS slot access:
  - Last pre-fix Ryujinx log: `~/Library/Logs/Ryujinx/Ryujinx_1.3.3_2026-07-28_12-46-34.log`.
  - macOS crash report: `~/Library/Logs/DiagnosticReports/Ryujinx-2026-07-28-124653.ips`.
  - Fault: invalid access at `0x109`.
  - PC: `kengine-nintendo-switch:0x1c7c4`.
  - Mapped symbol: `kotlin::mm::ThreadSuspensionData::setState(kotlin::ThreadState)`.
  - Cause: Kotlin `ThreadRegistry::currentThreadDataNode_` was still being accessed through direct Switch TLS at `tpidrro_el0 + offset`; the slot value read as `1`, producing bad pointer `0x109`.
- Updated the Kotlin fork to use Clang emulated TLS for the experimental Switch target:
  - `native/utils/src/org/jetbrains/kotlin/konan/target/ClangArgs.kt`: `switch_arm64` C/C++ runtime compile flags now use `-femulated-tls`.
  - `kotlin-native/konan/konan.properties`: `clangFlags.switch_arm64` now includes `-femulated-tls` for Kotlin IR object generation.
  - Attempted `-mtp=soft` first, but Kotlin's bundled Clang rejects that AArch64 mode; emulated TLS is the supported Clang path.
- Current ELF verification:
  - Kotlin runtime TLS variables are now emitted as `__emutls_v.*` / `__emutls_t.*`.
  - `_konan_function_0_impl`, `ScopedRunnableState`, and `getCurrentFrame` call `__emutls_get_address` for Kotlin TLS state.
  - Narrow disassembly scan through the Kotlin/runtime wrapper range found no `tpidr_el0`, `tpidrro_el0`, or `__aarch64_read_tp` instructions.
  - Strict disassembly search found no exact `tpidr_el0` instructions in the linked Switch game ELF.
  - Broad linked-ELF scans still find `tpidrro_el0` in libnx/newlib symbols such as `armGetTls`, mutexes, applet, and filesystem helpers. That is expected platform TLS, not the Kotlin `ThreadRegistry::currentThreadDataNode_` failure mode.

Conclusion: we are not stuck in a loop. The known generated-glue, stale-runtime-bitcode, and direct-Kotlin-TLS failure modes have been addressed in the built ELF, and the Kotlin-linked NRO now launches in Ryujinx. The latest build runs one pure Kotlin game from `:games:nintendo-switch-demo` through shared input and render-context contracts on both Kengine/SDL desktop and C/libnx Switch hosts.

## Next Steps

1. Launch the rebuilt `games/nintendo-switch-demo/build/switch/nintendo-switch-demo.nro` in Ryujinx and confirm the current 2D command-transfer build still shows:
   - a moving software-rendered square over a changing background,
   - visible `KENGINE SWITCH` HUD text,
   - responsive movement from the D-pad / left stick,
   - faster color changes while holding `A`,
   - color changes while holding `X` or `Y`,
   - size pulsing while holding `B`,
   - slower/faster manual movement while holding `L`/`R`,
   - exit only when pressing `Minus + Plus`.
2. Launch `games/hextris-switch/build/switch/hextris-switch.nro` in Ryujinx and confirm:
   - a playable falling-block board,
   - square software-rendered block sprites,
   - score/level/lines text,
   - D-pad movement, soft drop, hard drop, rotation, pause, and reset.
3. Add a portable storage/save-data API in `:kengine-core` before expanding into networking or 3D:
   - expose named records instead of arbitrary file paths,
   - validate keys with a conservative namespace,
   - keep initial values small and bounded,
   - add load/save/delete/exists tests,
   - wire a desktop implementation first,
   - wire the Switch host to a mounted filesystem with atomic replacement and required commit behavior.
4. Add one visible save/reload behavior to either `:games:hextris-core` or `:games:nintendo-switch-demo`; Hextris high score persistence is the best real-game smoke test.
5. Add the save/reload case to the Ryujinx checklist:
   - create or update saved state,
   - exit cleanly,
   - relaunch,
   - verify the state reloads,
   - verify corrupt/missing storage falls back safely.
6. After storage works, do a 2D support matrix pass:
   - sprite transparency, tinting, scaling, frame selection, and clipping,
   - text glyph coverage and positioning,
   - audio start/stop/retrigger behavior,
   - input mapping, pause/reset, and lifecycle cleanup.
7. If a Switch launch crashes, copy the new Ryujinx failure into `kengine-nintendo-switch/error.log`.
8. Map the new guest PC and stack addresses with `addr2line`.
9. Decide whether the next failure is:
   - another Kotlin runtime portability issue,
   - emulated TLS initialization/destructor behavior,
   - libnx/devkitPro integration,
   - or our C/Kotlin boundary code.
10. Keep networking and 3D deferred until the 2D/runtime checklist is stable.

## Useful Commands

```bash
JAVA_HOME="$(jenv prefix 17.0)" ./kengine-kotlin/build-kotlin-native-dist.sh
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew :games:nintendo-switch-demo:runDebugExecutableMacosArm64
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew -Pkengine.switch=true :games:nintendo-switch-demo:buildSwitchNro
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew -Pkengine.switch=true :games:hextris-switch:buildSwitchNro
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:clean :kengine-nintendo-switch:buildSwitchNro
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:buildSwitchCOnlyNro
```

Disassembly checks:

```bash
/opt/devkitpro/devkitA64/bin/aarch64-none-elf-objdump -d kengine-nintendo-switch/build/switch/kengine-nintendo-switch.elf | rg -n -C 2 "\\btpidr_el0\\b"
/opt/devkitpro/devkitA64/bin/aarch64-none-elf-objdump -d --start-address=0x0 --stop-address=0x30000 kengine-nintendo-switch/build/switch/kengine-nintendo-switch.elf | rg -n "tpidr_el0|tpidrro_el0|__aarch64_read_tp"
/opt/devkitpro/devkitA64/bin/aarch64-none-elf-objdump -d --start-address=0x267c0 --stop-address=0x26940 kengine-nintendo-switch/build/switch/kengine-nintendo-switch.elf
/opt/devkitpro/devkitA64/bin/aarch64-none-elf-addr2line -e kengine-nintendo-switch/build/switch/kengine-nintendo-switch.elf -f -C 0x1c7c4 0x26894 0x267c0 0x0914
```

Ryujinx logs:

```bash
ls -lt ~/Library/Logs/Ryujinx | head
perl -pe 's/\0//g' ~/Library/Logs/Ryujinx/Ryujinx_*.log | rg -n "Invalid memory|Guest stack trace|kengine-nintendo-switch|PC:|X\\["
```

## Known Risk

Full Kotlin `:kotlin-native:dist` previously hit a host JVM/LLVM-stubs crash while generating macOS caches. The current working path is to rebuild `:kotlin-native:distCompiler` or the smallest runtime/compiler task set needed for the Switch prototype.
