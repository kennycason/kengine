# Mario N64 Physics Follow-Up

Status: rendering works; the playable physics/controller milestone is not implemented yet.

Last reviewed: 2026-08-24

This document records the current state of `games/mario-n64` and `kengine-n64`, the issues found during the code and build review, and a concrete path to walking and jumping through the rendered Bob-omb Battlefield world.

The review covered the hand-written game, backend, build, and runtime code in `games/mario-n64` and `kengine-n64`; the relevant command/input APIs in `kengine-core`; the existing desktop terrain/controller code in `kengine-3d`; generated mesh statistics; recent commit history; screenshots; JVM compilation; and a Docker ROM build.

## Executive Summary

The textured Bob-omb Battlefield renderer is real and produces a stable ROM, but the current controls are a noclip camera rather than a character controller. There is no collision query, velocity, gravity, grounded state, jump state, wall response, body shape, Mario model, or third-person follow camera.

The shortest reliable route to a playable build is:

1. Make the N64 build trustworthy by fixing the currently suppressed linker failures and adding an ABI smoke test.
2. Generate a compact collision mesh and XZ spatial grid from the same DAE used by the renderer.
3. Implement a fixed-point, allocation-free kinematic controller with ground snapping, steps, gravity, jumping, and wall sliding.
4. Separate the player body from the camera and add useful collision diagnostics.
5. Treat a visible, animated, third-person Mario as a later rendering milestone.

Do not begin with a general rigid-body engine. A purpose-built kinematic character controller is smaller, more deterministic, and better suited to the N64 target and the current game.

## What Exists Today

### Game behavior

`Mario64Game` starts at approximately:

- X: `-3000`
- Y: `2500`
- Z: `-3000`

The directional inputs directly mutate camera X/Z. A raises Y, Z lowers Y, and the C buttons change yaw/pitch. Drawing emits one `DRAW_WORLD_3D` command whose player coordinates are used as the camera coordinates.

This is useful for inspecting the world, but it is noclip movement. In particular, it has none of the state a physics controller needs:

- no horizontal or vertical velocity;
- no gravity or terminal velocity;
- no grounded state or supporting triangle;
- no player radius, height, capsule, or cylinder;
- no floor, slope, step, wall, or edge tests;
- no jump edge detection;
- no separation between player position and camera position.

### Actual N64 rendering path

The active path is:

```text
Mario64Game.draw
  -> portable DRAW_WORLD_3D command
  -> KengineN64Runtime command copy
  -> kengine-n64/src/main/c/main.c
  -> libdragon OpenGL renderer
  -> generated C world mesh and textures
```

`KENGINE_N64_USE_GL` is enabled. The C host finds the 3D world command, clears the buffers, enables depth testing, establishes its projection/view state, and draws the generated mesh.

Two consequences are worth remembering:

- The command's `projectionDistance` is currently ignored by the GL renderer because the projection is hard-coded in C.
- The GL fast path bypasses the normal portable clear command and its clear behavior is hard-coded.

### Performance baseline and overlay

A manual ares test on 2026-08-24 confirmed that the ROM boots, renders the textured world correctly, accepts free-camera movement, and does not crash. [Screenshot 12](../games/mario-n64/dev_log/screenshot12.png) shows good texture, depth, and perspective output. Movement remained the expected noclip camera because character physics has not been implemented.

The same test initially reported subjectively very low performance, roughly a few frames per second. The original active GL renderer submitted 2,144 triangles every frame through immediate-mode calls, disabled back-face culling, and had no world-level visibility culling.

The GL path now displays the existing two-line performance overlay:

- `FPS`: measured presentation rate from libdragon;
- `CMD=a+b`: copied render commands plus dropped commands;
- `K`: Kotlin step and command-copy time in milliseconds;
- `R`: GL/RDP render time in milliseconds;
- `F`: frame count;
- `H`: used and total heap in KiB.

For a safe CPU-written overlay, the diagnostic path waits for GL/RDP completion before drawing the text and presenting the surface. This makes `R` include completion time, but it can reduce CPU/RDP overlap compared with the previous asynchronous `rdpq_detach_show` path. Treat the overlay build as a measurement build until the HUD is moved to a native GL/RDP text path or made toggleable.

The first instrumented immediate-mode measurement reported:

```text
FPS=7 CMD=2+0 K=2 R=148
F=124 H=4650/6957K
```

This isolated the bottleneck to rendering: the 148 ms render phase predicts approximately 6.8 FPS, Kotlin takes only 2 ms, and no commands are dropped.

The static world geometry is now compiled once into a libdragon GL display list and replayed with one `glCallList` per frame. The first test of that build reported approximately:

```text
FPS=18 CMD=2+0 K=2 R=53
```

That is about a 2.6x frame-rate improvement. Rendering still dominates, but its measured time fell from 148 ms to roughly 53 ms, Kotlin remains 2 ms, and no commands are dropped.

The imported render mesh has since been compacted before code generation. The DAE contained 1,043 pairs of exact, same-winding triangles with the same positions, UVs, and material. Removing those redundant pairs and compacting identical baked vertices changes the submitted world from 6,432 vertices / 2,144 triangles to 1,623 vertices / 1,100 triangles without intentionally changing visible geometry. This compact build still needs a manual FPS and artifact comparison.

### Camera-dependent surface artifacts

The display-list build showed jagged green/black regions appearing and disappearing during camera movement. See [debug image 1](../games/mario-n64/debug/img.png), [debug image 2](../games/mario-n64/debug/img_1.png), and [debug image 3](../games/mario-n64/debug/img_2.png).

The exact duplicate coplanar faces in the DAE were a plausible contributor, so the compact build removes them. This is not yet considered the confirmed fix: exact duplicates with matching UVs and material should ordinarily produce the same color. If the artifacts remain, investigate display-list texture/state transitions, UV fixed-point interpolation and clipping, and depth precision before adding visibility culling. Keep culling disabled until the complete surface set is stable.

### Lighting and depth readability

Static ambient plus directional lighting will help separate slopes and surfaces that currently blend together. The local SM64 source is available at `/Users/kenny/code/n64/sm64`; the original Bob-omb Battlefield model uses an ambient/directional `Lights1` definition and stores normals in its vertices.

The current `Area1.dae` does not expose a `NORMAL` stream. Its color streams are mostly constant values, and the N64 renderer currently modulates textures with white, so it has no comparable surface shading. The next low-risk visual step is generator-time face-normal lighting with an ambient floor and one fixed directional light, emitted as per-face vertex color and compiled into the display list. That adds depth cues without runtime normal transforms or allocations. If the flat result is too faceted, follow it with crease-aware smooth vertex normals. Real-time shadows are not needed for the playable milestone.

### Recent history

The last meaningful rendering milestone was on 2026-08-13:

- `8bcea9e` — `mario64 map rendering via libdragon`
- `c7fd115` — `add libdragon to build`

No later commit implements physics. The latest screenshots are consistent with the current result: a textured world that can be viewed, without a grounded character controller.

## Verified Build and Runtime Findings

### Docker ROM build

The following completed with Gradle reporting `BUILD SUCCESSFUL`:

```sh
./gradlew :kengine-n64:buildMarioN64Z64Docker -Pkengine.enableNintendo64=true --stacktrace
```

During the 2026-08-22 review, the existing, prebuilt, and rebuilt ROMs matched:

```text
SHA-256: ba3f4767540b9f1ecc642986c9d3841f2687f17a1d302392c431cca9dec405ce
Size:    606,208 bytes
```

The clean-link rebuild on 2026-08-24 produced another 606,208-byte ROM with SHA-256 `8ea982228db7c8f8c1e810a178246ad9a3b1d85c0ab5f5cbf5e83a181321302e`. The current Docker setup pulls the `latest` image and installs libdragon's `unstable` branch, so hashes from different dates do not establish reproducibility. Pin both inputs before treating cross-date ROM hashes as a deterministic-build check.

The later FPS-overlay build is also 606,208 bytes and has SHA-256 `017fc45625c1502ffa564774d5f7845100df6241192648fd5fad3f734173e031`.

The display-list experiment build is 622,592 bytes and has SHA-256 `d94fb93e6641531307cdc1dfa74272f7fff808f1f73f4c38bcc6a0ba20c7ba84`.

The compact-mesh build is 688,128 bytes and has SHA-256 `0017e465b1ab22286829473a118301518d216977964c984df53ada0b0d303560`. Its generated data is much smaller, but bounded Kotlin initializer functions encode more literals in ELF text, so ROM size alone is not a measure of runtime mesh cost.

### Linker failures found during the review

The linker reported multiple definitions of:

- `sleep`
- `posix_memalign`
- `write`

The duplicate definitions involved `kengine-n64/src/main/c/kotlin_stubs.c`, the `kotlin_stubs_o32.c` archive, and libdragonsys. At review time, the generated link command continued because `kengine-n64/build.gradle.kts` included `--noinhibit-exec` alongside `--no-warn-mismatch`.

This made a reported successful build weaker than it appeared.

Resolved on 2026-08-24:

- The redundant host `kotlin_stubs.c` object was removed from the Docker build.
- The O32 Kotlin support object is now the sole owner of `sleep` and `posix_memalign`.
- Libdragonsys is now the sole owner of `write`.
- `--noinhibit-exec` was removed, so linker errors fail the build instead of producing a suspect ELF.
- A full Docker ROM rebuild passed and the link map confirmed the expected owners. It emitted a 606,208-byte ROM with SHA-256 `8ea982228db7c8f8c1e810a178246ad9a3b1d85c0ab5f5cbf5e83a181321302e`.

Remaining ABI follow-up:

1. Reduce or remove `--no-warn-mismatch` once the ABI boundary is understood.
2. Add the ABI smoke test described below.

### ABI boundary

The Kotlin target is configured as `n64_mips32`. The Kotlin O32 stubs are compiled with `-mabi=o32`, while the current libdragon host build uses `-mabi=o64`. Mismatch warnings are presently suppressed.

The current Kotlin/C bridge mostly passes simple integers and pointers, so it can appear to work even when the ABI arrangement is fragile. Add a small, explicit smoke test that crosses the boundary with known integer values, pointer-backed data, return values, and any relevant struct layout. Document which side owns allocations and symbol definitions.

### Runtime allocation constraint

Kotlin is compiled with `-Xbinary=gc=noop`. Per-frame allocation is therefore not acceptable. Physics code should use preallocated primitive arrays and mutable integer fields; it should avoid temporary lists, boxed numbers, data-class churn, and allocation-heavy vector APIs.

### Input loses analog magnitude

The runtime currently sends one input bitmask. The C `translate_input` path quantizes the analog stick into direction bits, so stick magnitude is lost.

Digital movement is sufficient for the first playable milestone. A compact follow-up design can preserve the existing two-argument bridge by packing raw signed stick X/Y into the high bytes of the existing 32-bit input word while retaining buttons in the low bits. `InputState` can then expose continuous axes without breaking existing games.

## Test and Code Problems to Fix

### JVM asset tests

Running:

```sh
./gradlew :games:mario-n64:jvmTest
```

previously failed while compiling generated assets with:

```text
Method too large: mario64/Mario64ModelAssets.<clinit> ()V
```

The generator now emits a final `IntArray` filled by bounded 256-value functions. This avoids the JVM 64 KiB static-initializer limit without allocating temporary chunk arrays. `:games:mario-n64:jvmTest` now compiles and all five current tests pass.

The assertions were partially strengthened:

- The vertex-layout test now uses the declared stride of five `(x, y, z, u, v)` and checks the compact mesh counts.
- The Start/reset test only checks `droppedCommandCount`, not the reset coordinates or controller state.
- Tests currently establish that assets and a render command exist, not that movement or collision behaves correctly.

### Unused Kotlin renderer is internally inconsistent

`Mario64WorldRenderer` has no construction/reference in the active render path and appears to be dead code. It remains internally inconsistent:

- It indexes each vertex with `vertexIndex * 3`, even though the declared vertex stride is five.
- Its fixed capacity happens to contain the compact 1,623-vertex mesh, but it would still read the array incorrectly.

Either remove it to avoid maintaining a misleading second renderer, or repair it and add a focused test before declaring it part of the design. The active C GL renderer uses the correct mesh stride.

## World and Collision Data

The source world is:

```text
games/mario-n64/assets/models/bob-omb-battlefield/Area1.dae
```

The generated rendering data now contains:

| Property | Value |
| --- | ---: |
| Vertex entries | 1,623 (previously 6,432) |
| Vertex stride | 5 (`x, y, z, u, v`) |
| Render triangles | 1,100 (previously 2,144) |
| Materials | 18 |
| Textures | 18 |
| X bounds | -4,096 to 4,096 |
| Y bounds | -192 to 2,147 |
| Z bounds | -4,096 to 4,096 |

The DAE's paired geometry nodes produced 1,043 exact duplicate triangle pairs. Same-winding/material/position/UV deduplication, final baked-vertex compaction, and removal of a quantized degenerate leave 1,100 useful triangles. With `abs(normalY) >= 0.55` as an initial walkable-floor classification, the set is approximately:

- 597 floor/walkable triangles;
- 503 wall or steep-surface triangles.

An exact height query at the current X/Z spawn location `(-3000, -3000)` finds ground at approximately `Y = 485.235`. The current camera Y of 2,500 is therefore roughly 2,015 world units above the ground.

All observed world coordinates fit signed 16-bit storage.

### Recommended broad phase

A 16 by 16 uniform grid over X/Z, with 512-unit cells, is a good fit for this level:

| Metric | Observed value |
| --- | ---: |
| Total triangle-to-cell references | 4,240 |
| Average candidates per cell | 16.56 |
| Maximum candidates in one cell | 66 |
| Maximum floor candidates | 39 |
| Maximum wall candidates | 29 |

This is small enough for a predictable, allocation-free query and avoids scanning all triangles for every movement step.

### Collision asset generator

Generate collision data from the same DAE as the render mesh, but keep the render and collision outputs separate. The collision generator should:

1. Deduplicate triangles by position.
2. Remove degenerate triangles.
3. Compute/classify normals as floor/walkable versus steep/wall.
4. Quantize or validate coordinates for signed 16-bit storage.
5. Build the 16 by 16 XZ cell table and compact triangle-reference lists.
6. Emit primitive arrays with bounded initialization chunks so JVM tests compile.
7. Emit validation metadata such as bounds, triangle counts, and the expected spawn-ground result.

Suggested storage is `ShortArray` for coordinates and triangle references where the validated ranges permit it, plus `IntArray` for cell offsets/counts and any fixed-point values that need more range. Use `Long` intermediates in cross products and barycentric calculations where 32-bit multiplication could overflow.

## Recommended Controller Design

Build a small fixed-point kinematic character controller in common Kotlin. Start game-local or in a narrowly scoped portable package, then promote it to a reusable engine API after the Mario level proves its behavior and constraints.

Do not port the desktop implementation verbatim. The useful reference code is:

- `kengine-3d/src/nativeMain/kotlin/com/kengine/three/KinematicCharacterController3D.kt`
- `kengine-3d/src/nativeMain/kotlin/com/kengine/three/TerrainActorController3D.kt`
- `kengine-3d/src/nativeMain/kotlin/com/kengine/three/StaticMeshCollider3D.kt`

Reuse their gameplay semantics—ground snapping, step up/down, gravity, jumping, and wall sliding—but not their allocation patterns, `Double`/vector-heavy representation, native-only placement, lists, or linear triangle scans.

### Controller state

Keep controller state in mutable primitive fields, for example:

- position X/Y/Z;
- desired or actual horizontal velocity X/Z;
- vertical velocity;
- grounded flag;
- supporting triangle index;
- collision flags and last candidate count for diagnostics;
- previous button mask for jump edge detection.

Define dimensions and tolerances explicitly:

- body radius;
- standing height and camera eye height;
- maximum walkable slope;
- maximum step-up height;
- ground-snap/step-down distance;
- skin width;
- gravity, jump impulse, acceleration, movement speed, and terminal velocity.

The game currently updates once per rendered frame. The first implementation can deliberately target a fixed 60 Hz step. If the runtime later exposes variable frame timing, place the controller behind a deterministic fixed-step accumulator rather than retuning physics around variable deltas.

### Ground query

For a candidate X/Z location:

1. Look up the overlapping grid cell or cells under the player's radius.
2. Examine floor candidates only.
3. Reject triangles whose XZ projection does not contain the query point, allowing a small edge tolerance.
4. Compute exact Y with barycentric or plane interpolation.
5. Select the highest valid supporting surface within the allowed step-up/step-down window.
6. Record the supporting triangle and grounded state.

Queries need clear behavior for overlapping surfaces, undersides, out-of-grid positions, and the boundary between adjacent cells.

### Movement and wall response

For each fixed update:

1. Convert input into a desired XZ movement relative to camera yaw.
2. Attempt the full horizontal move.
3. If blocked, resolve the body circle/capsule against nearby steep triangles.
4. Project the remaining movement along the wall tangent to slide.
5. Use a small fixed iteration count, initially three or four, so cost is bounded.
6. Try the ground query at the resolved position, including step-up and ground-snap tolerances.
7. If there is no supporting surface, leave grounded state and apply gravity.
8. Integrate vertical velocity and clamp at the selected floor on landing.

A simple fallback that tries the combined move followed by X-only and Z-only alternatives can help establish basic sliding, but the final response should be based on the nearby steep triangle geometry so diagonal and corner behavior are stable.

### Jump, fall, and reset

- Trigger jump on the A-button rising edge only while grounded.
- Clear grounded state immediately and apply a fixed upward impulse.
- Apply gravity every airborne step and clamp to a terminal fall speed.
- Land on the highest valid floor crossed by the feet during the step; zero downward velocity on landing.
- Leaving a platform edge must clear grounded state and begin falling.
- Start should reset the complete controller state to an explicit spawn X/Z and its computed ground Y.
- Z may remain available as an intentional debug noclip toggle, but it should not move the normal player vertically.

### Body and camera separation

The first playable milestone may remain first-person. Even then, store a body position independently and derive the camera from:

```text
camera = body position + eye height
```

This prevents camera controls from becoming physics state and makes a later third-person camera possible. A visible Mario requires a separate renderer milestone: an additional model draw path/transforms, a Mario asset pipeline, animation, facing state, and a follow camera. None of that exists in `games/mario-n64` today, so it should not block proving locomotion.

## Phased Implementation Plan

### Phase 0: Make builds and tests trustworthy

- [x] Resolve duplicate `sleep`, `posix_memalign`, and `write` definitions.
- [x] Remove `--noinhibit-exec` and make linker errors fatal.
- [ ] Pin the libdragon Docker image and source revision for reproducible builds.
- [ ] Investigate the O32/O64 arrangement; narrow or remove `--no-warn-mismatch`.
- [ ] Add a deterministic Kotlin/C ABI smoke test.
- [x] Split the generated Kotlin asset initializer so JVM tests compile.
- [x] Fix the vertex-stride assertion.
- [ ] Add meaningful Start/reset coordinate and controller-state assertions.
- [ ] Remove or repair and test the unused `Mario64WorldRenderer`.

Exit condition: JVM tests run, the ROM links cleanly, and failures are no longer hidden by build flags.

### Phase 1: Generate collision data

- [ ] Extend the model conversion pipeline with a collision-output mode.
- [ ] Deduplicate position triangles and remove degenerates.
- [ ] Classify floors/walls with an explicit slope threshold.
- [ ] Generate the 16 by 16 XZ grid and compact reference lists.
- [ ] Emit JVM-safe, allocation-free primitive data.
- [ ] Validate counts, bounds, cell coverage, and spawn ground.

Exit condition: a common Kotlin test can query the expected ground around `(-3000, -3000)` without scanning the full mesh.

### Phase 2: Implement the playable controller

- [ ] Add fixed-point controller configuration and mutable state.
- [ ] Add camera-relative horizontal movement.
- [ ] Add ground selection, snapping, step up/down, and slope limits.
- [ ] Add gravity, terminal velocity, jump, landing, and edge falling.
- [ ] Add body-radius wall collision and sliding with bounded iterations.
- [ ] Separate body and camera positions.
- [ ] Make Start reset the full state to grounded spawn.

Exit condition: the player can traverse the level, step onto reachable geometry, collide and slide against walls, jump, land, fall from edges, and reset.

### Phase 3: Improve control and observability

- [ ] Keep digital controls working as the baseline.
- [ ] Preserve raw analog X/Y through the bridge and expose continuous axes.
- [x] Add FPS, Kotlin/render timing, heap, and render-command drop metrics to the active GL path.
- [ ] Add position, vertical velocity, grounded state, support triangle, candidate count, and collision flags to a debug display or rate-limited log.
- [ ] Add collision debug lines and controller query-limit metrics.

Exit condition: movement is tunable on a controller and collision failures can be diagnosed on the emulator without guessing.

### Phase 4: Add third-person Mario presentation

- [ ] Add a portable or N64-specific model draw command with transforms.
- [ ] Add a legal, runtime-sized Mario model/texture pipeline.
- [ ] Add a follow/orbit camera with obstruction handling as needed.
- [ ] Add facing, idle/run/jump/fall/land animation state.

Exit condition: a visible animated character follows the already-proven physics body. This phase is deliberately outside the first physics MVP.

## Acceptance Tests

### Collision generator

- [ ] No emitted collision triangle is degenerate.
- [ ] Every index and coordinate is within its declared representation.
- [ ] Every triangle appears in every grid cell overlapped by its XZ bounds.
- [ ] Generated bounds match the expected `-4096..4096`, `-192..2147`, `-4096..4096` world envelope.
- [ ] A ground query at spawn returns approximately `Y = 485.235` within the chosen fixed-point tolerance.
- [ ] Repeated generation is byte-for-byte deterministic.

### Character controller

- [ ] A falling body lands on a floor, becomes grounded, and zeroes downward velocity.
- [ ] Jump only starts while grounded and only on the input edge.
- [ ] A complete jump arc lands without tunneling through the starting floor.
- [ ] A reachable step is climbed and a step above the configured limit is blocked.
- [ ] Motion into a wall retains the tangential component and slides.
- [ ] Walking off an edge clears grounded state and starts falling.
- [ ] Start restores the exact spawn state and supporting floor.
- [ ] Out-of-grid movement follows a defined safe behavior.
- [ ] Controller updates allocate no runtime objects.

### Build and integration

- [x] `:games:mario-n64:jvmTest` compiles and passes.
- [x] The N64 linker reports no duplicate-definition errors.
- [ ] Any remaining ABI warning has a documented, tested justification; ideally there are none.
- [x] The instrumented renderer reports `CMD=2+0` during manual traversal.
- [x] Rebuilt ROM runs in a libdragon-compatible emulator configuration such as ares/LLE.
- [ ] Long-running movement remains stable with the no-op GC runtime.

## Definition of the First Playable Milestone

The physics milestone is complete when the N64 build can:

- spawn the body on the terrain instead of high above it;
- move relative to camera yaw;
- remain attached to ordinary slopes and small steps;
- collide with and slide along walls;
- fall when leaving cliffs or platforms;
- jump and land deterministically;
- reset to a known valid spawn with Start;
- run without per-frame allocation, command drops, or a suppressed link failure.

A visible Mario model, animation, advanced camera obstruction, enemies, moving platforms, water, and general rigid-body physics are not required for this milestone.

## Useful Commands

JVM tests:

```sh
./gradlew :games:mario-n64:jvmTest
```

Build the game ROM through the game module:

```sh
./gradlew :games:mario-n64:buildN64Z64 -Pkengine.enableNintendo64=true
```

Build the reviewed Docker backend target:

```sh
./gradlew :kengine-n64:buildMarioN64Z64Docker -Pkengine.enableNintendo64=true --stacktrace
```

Run through the configured N64 emulator task:

```sh
./gradlew :games:mario-n64:runN64 -Pkengine.enableNintendo64=true
```

Use an emulator/configuration that supports libdragon custom RSP microcode; see [Nintendo 64 Work](NINTENDO_64.md#emulator-compatibility) for the HLE/LLE caveat.

## Immediate Next Slice

1. Manually test the compact-mesh ROM from the same camera paths as the three debug images. Record `FPS`, `K`, `R`, and `H`, and confirm whether the moving surface patches remain.
2. If geometry is stable, add generator-time ambient plus directional face shading and compare readability and performance. If the artifacts remain, diagnose render state/UV/depth first.
3. Generate the deduplicated collision triangles and 16 by 16 grid from the same DAE.
4. Add a deterministic `groundHeightAt(x, z)` query and tests, including the spawn result.
5. Replace vertical noclip at startup with a separate body whose Y is initialized from the spawn ground plus the configured eye offset.

That order validates the newly compacted render input before visual work, then proves the collision representation, grid lookup, fixed-point math, and first visible terrain contact before wall/capsule resolution is added.
