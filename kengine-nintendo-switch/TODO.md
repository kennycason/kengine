# kengine-nintendo-switch TODO

## Current Checkpoint

- C-only Switch homebrew build opens successfully in Ryujinx.
- Kotlin-linked Switch build compiles and packages as `build/switch/kengine-nintendo-switch.nro`.
- The first Kotlin crash was fixed at the generated C API wrapper layer:
  - Old failure: invalid read at `0x28`.
  - Old bad instruction: wrapper used `mrs ..., tpidr_el0`.
  - Current wrapper disassembly now uses `mrs ..., tpidrro_el0`.
- Latest run still crashes, but it moved:
  - Latest Ryujinx log: `~/Library/Logs/Ryujinx/Ryujinx_1.3.3_2026-07-28_05-05-13.log`.
  - Latest guest fault: invalid access at `0x109`.
  - Latest PC: `kengine-nintendo-switch:0x1c7f8`.
  - Mapped symbol: `kotlin::mm::ThreadSuspensionData::setState(kotlin::ThreadState)`.
  - Call path includes `ScopedRunnableState`, `_konan_function_0_impl`, then `kotlin_add_probe`.

Conclusion: we are not stuck in a loop. We fixed the generated API wrapper TLS issue and exposed the next TLS issue inside the Kotlin runtime/native support objects.

## Next Steps

1. Trace every remaining `tpidr_el0` instruction in `build/switch/kengine-nintendo-switch.elf`.
2. Split those instructions into:
   - libnx/devkitPro/system code that is expected or harmless.
   - Kotlin runtime/generated code that must use `tpidrro_el0`.
3. Patch the Kotlin fork so Switch TLS flags apply to the Kotlin runtime C/C++/bitcode build, not only exported C API glue:
   - `-mtp=tpidrro_el0`
   - `-ftls-model=local-exec`
   - target feature `+tpidrro-el0`
4. Rebuild the minimum Kotlin/Native distribution pieces needed for the Switch app.
5. Rebuild the Kotlin NRO.
6. Disassemble-check before testing in Ryujinx:
   - Generated wrapper should keep using `tpidrro_el0`.
   - Kotlin runtime call path around `ThreadSuspensionData::setState` should no longer depend on `tpidr_el0`.
7. Re-run `build/switch/kengine-nintendo-switch.nro` in Ryujinx.

## Useful Commands

```bash
jenv exec ./kengine-kotlin/build-kotlin-native-dist.sh --task :kotlin-native:distCompiler --no-local-properties
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:clean :kengine-nintendo-switch:buildSwitchNro
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:buildSwitchCOnlyNro
```

Disassembly checks:

```bash
/opt/devkitpro/devkitA64/bin/aarch64-none-elf-objdump -d kengine-nintendo-switch/build/switch/kengine-nintendo-switch.elf | rg -n -C 4 "tpidr|_konan_function_0_impl|ScopedRunnableState|ThreadSuspensionData"
/opt/devkitpro/devkitA64/bin/aarch64-none-elf-addr2line -e kengine-nintendo-switch/build/switch/kengine-nintendo-switch.elf -f -C 0x1c7f8 0x26b50 0x26a64 0x0914
```

Ryujinx logs:

```bash
ls -lt ~/Library/Logs/Ryujinx | head
perl -pe 's/\0//g' ~/Library/Logs/Ryujinx/Ryujinx_*.log | rg -n "Invalid memory|Guest stack trace|kengine-nintendo-switch|PC:|X\\["
```

## Known Risk

Full Kotlin `:kotlin-native:dist` previously hit a host JVM/LLVM-stubs crash while generating macOS caches. The current working path is to rebuild `:kotlin-native:distCompiler` or the smallest runtime/compiler task set needed for the Switch prototype.
