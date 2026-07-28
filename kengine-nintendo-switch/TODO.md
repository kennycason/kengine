# kengine-nintendo-switch TODO

## Current Checkpoint

- C-only Switch homebrew build opens successfully in Ryujinx.
- Kotlin-linked Switch build compiles and packages as `build/switch/kengine-nintendo-switch.nro`.
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
  - Narrow disassembly scan through the Kotlin/runtime address range found no `tpidr_el0`, `tpidrro_el0`, or `__aarch64_read_tp` instructions.
  - Strict disassembly search found no exact `tpidr_el0` instructions in `build/switch/kengine-nintendo-switch.elf`.

Conclusion: we are not stuck in a loop. The known generated-glue, stale-runtime-bitcode, and direct-Kotlin-TLS failure modes have been addressed in the built ELF. The next signal needs to come from launching the rebuilt NRO in Ryujinx.

## Next Steps

1. Launch the rebuilt `build/switch/kengine-nintendo-switch.nro` in Ryujinx.
2. If it still crashes, copy the new Ryujinx failure into `kengine-nintendo-switch/error.log`.
3. Map the new guest PC and stack addresses with `addr2line`.
4. Decide whether the next failure is:
   - another Kotlin runtime portability issue,
   - emulated TLS initialization/destructor behavior,
   - libnx/devkitPro integration,
   - or our C/Kotlin boundary code.
5. Once the hello-world Kotlin call runs, replace the probe with the thinnest kengine loop entry point.

## Useful Commands

```bash
jenv exec ./kengine-kotlin/build-kotlin-native-dist.sh
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:clean :kengine-nintendo-switch:buildSwitchNro
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:buildSwitchCOnlyNro
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
