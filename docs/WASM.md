# WASM Target Status

## Current State: Not Started

The `wasm-target` branch exists but contains **zero commits ahead of main**. No WASM configuration, code, or build targets have been implemented.

## What Exists Today

- **JS target** is configured in `kengine/build.gradle.kts` (IR mode, browser + nodejs) but does not include SDL3 bindings (cinterop is native-only)
- **Kotlin/WASM** is supported by Kotlin 2.1.10 via `wasmJs {}` and `wasmWasi {}` targets

## Challenges

### SDL3 Cannot Run in WASM Directly
The engine relies on Kotlin/Native cinterop with SDL3 C libraries. WASM runs in a browser sandbox without access to native shared libraries. Options:

1. **Emscripten + SDL3**: SDL3 supports Emscripten compilation, which translates SDL calls to WebGL/WebAudio. However, Kotlin/WASM does not support Emscripten-style linking — it targets the browser JS environment, not a full POSIX-like Emscripten runtime.

2. **Abstraction layer**: Create a platform-agnostic rendering/input API in `commonMain`, with:
   - `nativeMain` implementation using SDL3 cinterop (current code)
   - `wasmJsMain` implementation using Canvas2D/WebGL via Kotlin/JS interop
   This is the most viable path but requires significant refactoring.

3. **Kotlin/WASM + JS interop**: Use `wasmJs` target with `@JsExport`/`@JsImport` to call browser Canvas/WebGL/WebAudio APIs directly from WASM. This avoids SDL3 entirely for the web target.

## Recommended Approach

### Phase 1: Extract Platform Abstraction
- Define interfaces in `commonMain` for: Renderer, InputManager, AudioPlayer, WindowManager
- Move current SDL3 code behind these interfaces in `nativeMain`
- No behavioral changes — just refactor to enable future targets

### Phase 2: Add wasmJs Target
- Add `wasmJs { browser() }` target to relevant modules
- Implement `wasmJsMain` source sets with Canvas2D/WebGL backends
- Start with kengine-reactive (pure Kotlin, no SDL dependency — should work immediately)
- Then kengine core with a minimal renderer

### Phase 3: Feature Parity
- Input handling (keyboard, mouse, touch — gamepad via Gamepad API)
- Audio (Web Audio API)
- Asset loading (fetch API)
- Testing in browser

## Quick Win: kengine-reactive

The `kengine-reactive` module (hooks system) has **zero native dependencies**. It could be compiled to WASM today by adding:

```kotlin
// in kengine-reactive/build.gradle.kts
wasmJs {
    browser()
}
```

This would validate the WASM build pipeline without tackling the SDL3 abstraction problem.
