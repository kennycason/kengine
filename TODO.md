# Kengine TODO

## Status Overview

| Area | Status | Notes |
|------|--------|-------|
| Playdate gate | Done | Gated behind `-Pkengine.playdate=true` in settings.gradle.kts |
| macOS .app bundling | Done | KenginePackagingPlugin: `./gradlew :games:<name>:packageMac` |
| Tiled map perf | Done | ~1.7ms/render avg (was 6-7ms), pre-resolved cells, UIntArray, layer opacity/offset |
| Tween/easing system | Partial | MoveAction (linear only), TimerAction, IntervalAction — no easing curves |
| Particle system | Partial | Visual-only (position/velocity/gravity/color), no physics integration |
| Entity system | Minimal | Inheritance-based Entity/SpriteEntity/Actor, not a true ECS |
| Physics tests | None | kengine-physics module works but has zero test files |
| Scene management | None | No scene graph, transitions, or scene stack |
| TMX/TSX support | Done | XML (.tmx/.tsx) and JSON (.tmj/.tsj) both supported |
| Asset embedding | None | Assets copied to build output, not compiled into binary |

## Roadmap (Priority Order)

### High Impact
1. **Tween/easing system** — Add easing curves (quad, cubic, elastic, bounce, etc.) to MoveAction and a general-purpose PropertyTween. Essential for game feel.
2. **Scene management** — Scene stack with push/pop, scene transitions (fade, slide), lifecycle hooks (enter/exit/pause/resume).
3. **Physics module tests** — Zero coverage on a working module is a ticking time bomb. Cover Body, Shape, collision callbacks, world stepping.

### Medium Impact
4. **Entity system improvements** — Component-based composition, entity registry with typed queries. Current inheritance hierarchy doesn't scale.
5. **Physics-based particles** — Connect particle system to kengine-physics for collision-aware particles.
6. **Asset embedding** — Compile assets into the binary for single-file distribution. `buildIncludeBinaryArgsForAssets()` exists in KengineAssetCopier but is unused.

### Lower Priority
7. **WASM target** — Branch exists with no work. Would require SDL3 replacement with WebGL/Canvas.
8. **Linux/Windows packaging polish** — Tarball and MSYS2 packaging exist but less tested than macOS.
