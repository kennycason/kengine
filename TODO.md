# Kengine TODO

## Roadmap (Priority Order)

### High Impact
1. **Entity system improvements** — Component-based composition, entity registry with typed queries. Current inheritance hierarchy doesn't scale.
2. **Physics-based particles** — Connect particle system to kengine-physics for collision-aware particles. Current system is visual-only.
3. **Asset embedding** — Compile assets into the binary for single-file distribution. `buildIncludeBinaryArgsForAssets()` exists in KengineAssetCopier but is unused.

### Medium Impact
4. **Menu / UI system** — Reusable menu components for title screens, options, HUDs.
5. **Logger file support** — Write logs to file in addition to stdout.
6. **Font handling redesign** — Font caching, configurable defaults, bitmap font support.

### Lower Priority
7. **WASM target** — Branch exists with no work. Would require SDL3 replacement with WebGL/Canvas.
8. **Linux/Windows packaging polish** — Tarball and MSYS2 packaging exist but less tested than macOS.
