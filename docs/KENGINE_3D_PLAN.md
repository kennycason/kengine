# Kengine 3D Plan

Status: GPU window, primitive rendering, depth proof, mesh rendering, Rubik's cube, OBJ mesh import, directional lighting, UV texture sampling, image-backed GPU textures, GLB static/skinned/animated loading, basic 3D collision, camera controls, animation helpers, and scene submission started

## Summary

`kengine-3d` should start as a small native module built on SDL3's `SDL_GPU` API, not as a full engine rewrite and not as a second windowing stack.

SDL3 is still not a 3D engine. It does not provide cameras, scene graphs, model loading, animation, materials, lighting, or physics. It does provide a modern low-level GPU API for 3D graphics and compute:

- SDL GPU overview: https://wiki.libsdl.org/SDL3/CategoryGPU
- GPU device creation: https://wiki.libsdl.org/SDL3/SDL_CreateGPUDevice
- Claiming an SDL window for a GPU device: https://wiki.libsdl.org/SDL3/SDL_ClaimWindowForGPUDevice

The practical direction is:

- Keep SDL3 for windowing, events, input, timing, audio, and networking.
- Add a 3D render backend that owns an SDL GPU device and swapchain.
- Keep the existing SDL renderer path for 2D games.
- Do not try to mix SDL's 2D renderer and SDL GPU rendering on the same window until we deliberately design and test that path.

## Current Implementation

The first GPU window slice has been added:

- `RenderBackend` selects the existing SDL renderer path or the new SDL GPU path.
- `SDLContext` exposes the SDL window and only creates `SDL_Renderer` for the 2D backend.
- `kengine-3d` provides `GpuContext`, which creates an `SDL_GPUDevice`, claims/releases the SDL window, clears the swapchain, and can create depth-enabled render passes.
- `kengine-3d` provides `PrimitiveRenderer3D`, a small debug primitive renderer for engine-owned triangle/quad boilerplate.
- `kengine-3d` provides the first camera abstractions: `Camera3D`, fixed `PerspectiveCamera`, `OrbitCamera3D`, `OrbitCameraController3D`, `ThirdPersonCamera3D`, and `ThirdPersonCameraController3D`.
- `ThirdPersonCameraController3D` supports configurable Y-look inversion, distance stops, zoom bounds, look smoothing, and follow smoothing while leaving actual keyboard/controller bindings in the game.
- `kengine-3d` provides the first mesh path: `GpuMesh`, `CubeFaceColors`, `Vertex3D`, `MeshRenderer3D`, `Mat4`, and `Transform3D`.
- `kengine-3d` provides the first texture/material path: `GpuTexture`, `TextureVertex3D`, `TexturedGpuMesh`, and `TexturedMeshRenderer3D`. `GpuTexture` can create procedural RGBA textures or upload image files loaded through SDL_image.
- `kengine-3d` provides the first lit mesh path: `LitGpuMesh`, `LitVertex3D`, `LitMeshRenderer3D`, and `DirectionalLight3D`.
- `kengine-3d` provides the first textured lit mesh path: `TexturedLitGpuMesh`, `TexturedLitVertex3D`, and `TexturedLitMeshRenderer3D`.
- `kengine-3d` provides `ObjMeshLoader`, a lightweight Wavefront OBJ path that reads vertices/UVs/faces, triangulates polygons, resolves `.mtl` diffuse colors, normalizes imported geometry, and emits `GpuMesh`, `LitGpuMesh`, or `TexturedLitGpuMesh` data.
- `kengine-3d` provides `ModelLoader3D`, `ParsedModel3D`, `Model3D`, `ModelPart3D`, `Material3D`, and `ModelRenderer3D` as the first reusable model/material facade over the lower-level OBJ/GLB paths.
- `kengine-3d` provides `Scene3D`, scene item types, and `SceneRenderer3D` for ordered per-frame submission of static models, animated models, and mesh primitives through one renderer bundle.
- `kengine-3d` provides `GpuResourceScope3D`, a small cleanup scope for GPU-backed resources owned by demos and games.
- `kengine-3d` provides `GlbMeshLoader`, a Kotlin-side GLB 2.0 path that loads static textured/lit meshes, CPU-updated skinned textured meshes, node-transform animation clips, embedded textures, and CPU-side lit vertices for collision.
- `kengine-3d` provides `AnimationClipSet3D`, `AnimationClipMap3D`, `AnimationPose3D`, and `AnimationPlayer3D` for reusable clip lookup, pose values, and stateful animation time advancement.
- `kengine-3d` provides `AnimatedModel3D`, `AnimatedModelLoader3D`, and `AnimatedModelInstance3D` as the first format-neutral facade over node-animated and CPU-skinned model playback, per-instance transform/pose state, and pre-render pose preparation.
- `kengine-3d` provides `TerrainMeshCollider3D`, `StaticMeshCollider3D`, `Collision3D`, `KinematicCharacterController3D`, and `TerrainActorController3D` for the current 3D gameplay collision baseline.
- `games:kengine-3d-demos` opens a GPU-backed window, uses mouse-drag orbit camera controls plus arrow-key zoom/pan, and renders separated rows of multiple primitives, a rotating color cube, a rotating textured cube, clean color-lit Kenney Space Kit ship/turret OBJ models, and a separate UV-textured meteor OBJ model using a PNG loaded from disk.
- `games:mario-3d` is now the main 3D platformer validation bed: textured GLB world rendering, terrain/static collision from parsed model vertices, reusable third-person camera controls, skinned Mario animation, animated enemies, controller input, debug drawing, and a simple Bowser encounter.
- `games:rubiks-cube-3d` renders a 27-cubie Rubik's cube with per-face colors, mouse orbit, mouse-picked face turns, keyboard face turns, animated slice rotations, scramble, and reset.

Implementation note: the SDL GPU declarations are already generated by the core `SDL3/SDL.h` cinterop in `kengine`; a separate `sdl3_gpu.def` produced an effectively empty binding and is not needed.

Current shader note: the 3D renderers use inline MSL shader source for the macOS proof. Before treating the renderer as cross-platform, add a repeatable shader build/package path for MSL, SPIR-V, and DXIL.

## Recommended Stack

### First Choice: SDL_GPU

Use SDL3's built-in GPU API as the first backend.

Reasons:

- It is already part of our SDL3 dependency set.
- It is a C API, so Kotlin/Native cinterop is straightforward compared with C++ libraries.
- It works with the SDL window/event/input stack we already use.
- It keeps packaging closer to what we already do for macOS, Linux, and Windows.
- It provides modern GPU concepts: devices, shaders, buffers, textures, pipelines, render passes, command buffers, and swapchains.

Tradeoffs:

- It is low-level. We must build engine concepts on top.
- Shaders need a real workflow. SDL GPU backends use different shader formats for Metal, Vulkan, and D3D12.
- It is newer than older rendering libraries, so examples and ecosystem are thinner.

### Asset Loading: OBJ Now, glTF 2.0 Next

The first native model-loading step is Wavefront OBJ:

- Simple text format.
- Easy to parse directly in Kotlin/Native.
- Good for proving imported mesh data, face triangulation, material-color handoff, asset packaging, and renderer integration.
- Can emit simple colored `GpuMesh` data, normal-bearing `LitGpuMesh` data, or UV-bearing `TexturedLitGpuMesh` data.
- Already exercised by `games:kengine-3d-demos` with CC0 Kenney Space Kit ship and turret models.

Keep this OBJ path intentionally small. It should be a practical importer for static low-poly meshes, not a replacement for a full scene format.

The next material step is to parse `.mtl` `map_Kd` entries, load those image files with `GpuTexture.fromFile`, and bind textures per material instead of assigning one texture to an entire mesh.

Next, add glTF 2.0 as the main 3D asset format.

Start with static glTF meshes first, then materials/textures, then animation.

Good first loader:

- `cgltf`: https://github.com/jkuhlmann/cgltf

Reasons:

- Single-file C loader.
- Friendly to Kotlin/Native via cinterop or a tiny C wrapper.
- Focused on glTF instead of every possible 3D format.

Avoid starting with Assimp unless we need many legacy formats. Assimp is powerful, but heavier and C++ based.

### Alternatives To Reconsider Later

| Option | Notes |
| --- | --- |
| `bgfx` | Mature, cross-platform, render-backend agnostic. It can integrate with SDL-style windowing, but brings C++ build/link complexity. Useful if SDL_GPU becomes too limiting. |
| `sokol_gfx` | Small C API and language-binding friendly. It is attractive, but overlaps with SDL_GPU and would add another graphics abstraction. |
| OpenGL | Fastest prototype path, but weak long-term choice, especially on macOS. |
| Vulkan/Metal/D3D directly | Too much API surface for Kengine right now. |

## Interop With Existing Kengine

### Reusable As-Is

These systems should work with `kengine-3d` with little or no change:

- Game loop
- SDL event polling
- Keyboard, mouse, and controller input
- Clock and frame timing
- Action/effect scheduling
- Logging
- Audio via `kengine-sound`
- Networking via `kengine-network`
- Asset copying conventions

### Needs A Boundary

The current 2D renderer stack uses SDL's render API:

- `SDLContext` creates an `SDL_Renderer`.
- `SpriteBatch` uses `SDL_RenderGeometry`.
- `TextureContext`, `SpriteContext`, `FontContext`, `GeometryContext`, and `ViewContext` are all oriented around `SDL_Renderer`.

For 3D, the first version should use a separate GPU rendering path:

- `SDL_Window`
- `SDL_GPUDevice`
- GPU swapchain
- GPU textures and buffers
- GPU render passes

Do not assume an `SDL_Texture` can be reused as an `SDL_GPUTexture`. Treat 2D textures and 3D textures as separate resource types until we intentionally bridge them.

## Required Core Refactor

`SDLContext` currently privately owns the window and lazily creates an SDL renderer:

- `kengine/src/nativeMain/kotlin/com/kengine/sdl/SDLContext.kt`

For 3D we need a window-only or backend-aware path.

Recommended direction:

```kotlin
enum class RenderBackend {
    SDL_RENDERER_2D,
    SDL_GPU_3D
}
```

Then `SDLContext` should:

- Expose the `SDL_Window` pointer safely to internal modules.
- Create `SDL_Renderer` only for the 2D backend.
- Avoid forcing renderer creation during cleanup.
- Destroy only the resources that were actually created.

This preserves existing 2D behavior while letting `kengine-3d` claim the same SDL window for a GPU swapchain.

## Proposed Module Shape

Add a module:

```text
kengine-3d/
  build.gradle.kts
  src/nativeMain/kotlin/com/kengine/three/
    GpuContext.kt
    GpuFrame.kt
    PrimitiveRenderer3D.kt
    Camera3D.kt
    PerspectiveCamera.kt
    OrbitCamera3D.kt
    OrbitCameraController3D.kt
    Transform3D.kt
    Mat4.kt
    Vertex3D.kt
    LitVertex3D.kt
    TexturedLitVertex3D.kt
    GpuMesh.kt
    LitGpuMesh.kt
    TexturedLitGpuMesh.kt
    MeshRenderer3D.kt
    LitMeshRenderer3D.kt
    TexturedLitMeshRenderer3D.kt
    DirectionalLight3D.kt
    Material.kt
    Texture3D.kt
    ObjMeshLoader.kt
    Model.kt
    Scene3D.kt
  src/nativeInterop/cinterop/
    cgltf.def              # later, optional
```

Initial dependencies:

- `kengine`
- `kengine-reactive`
- SDL3

Likely native-only targets at first:

- `macosArm64`
- `linuxX64`
- `mingwX64`

We can add common JVM/JS metadata later if publishing shape requires it, but actual rendering should start native-only.

## Proposed Runtime Context

`GpuContext` should be a Kengine `Context` that owns:

- `SDL_GPUDevice`
- window claim/release
- swapchain setup
- command buffer acquisition
- render pass helpers
- optional depth target management
- resource cleanup

Sketch:

```kotlin
class GpuContext private constructor(
    private val sdl: SDLContext
) : Context() {
    val device: CPointer<SDL_GPUDevice>

    fun render(
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        enableDepth: Boolean = false,
        block: (GpuFrame) -> Unit
    )

    override fun cleanup()
}
```

Keep this thin. Higher-level types should sit above it.

## Minimal Public API Goal

A first usable 3D API might look like:

```kotlin
useGpuContext {
    val camera = PerspectiveCamera(
        fovDegrees = 60f,
        near = 0.1f,
        far = 100f
    )

    val cube = GpuMesh.cube(this)
    val meshes = MeshRenderer3D(this)

    render(0.05f, 0.06f, 0.08f, 1f, enableDepth = true) { frame ->
        meshes.draw(frame, cube, Transform3D(position = Vec3(0.0, 0.0, -4.0)), camera)
    }
}
```

This is intentionally small. We should not start with a full scene graph.

## Math Layer

Add or extend math primitives:

- `Vec2`
- `Vec3`
- `Vec4`
- `Quat`
- `Mat4`
- `Transform3D`
- projection helpers
- view matrix helpers

Keep these in Kotlin unless performance proves otherwise.

Potential location:

- `kengine/src/nativeMain/kotlin/com/kengine/math`
- or a new common math package if we want 2D and 3D to share it

## Shader Strategy

SDL GPU needs shaders in backend-specific formats. We need a repeatable shader pipeline before loading real models.

Initial approach:

- Keep a small set of built-in shaders for MVP demos.
- Store precompiled shader artifacts in module resources or generated source.
- Use a Gradle task later to compile shaders for Metal, Vulkan, and D3D12.

Candidates to investigate:

- SDL_shadercross: https://github.com/libsdl-org/SDL_shadercross
- SDL shader tools: https://github.com/libsdl-org/SDL_shader_tools

Do not block the first triangle/cube on a perfect shader pipeline. Start with the smallest reliable cross-platform set.

## Milestones

### Milestone 1: GPU Window Proof

- Done: Add `RenderBackend`.
- Done: Refactor `SDLContext` so `SDL_Renderer` is optional.
- Done: Add `kengine-3d`.
- Done: Add `GpuContext`.
- Done: Create and destroy `SDL_GPUDevice`.
- Done: Claim/release the existing SDL window.
- Done: Add `games:kengine-3d-demos`.

Success criteria:

- Done: Opens a window.
- Done: Clears the swapchain.
- Done: Exits cleanly on macOS.

### Milestone 2: Triangle

- Done: Move primitive shader/pipeline boilerplate into `kengine-3d`.
- Done: Create inline macOS shader source for the first proof.
- Deferred: Create shader module/package workflow.
- Done: Create the first GPU vertex buffer in the mesh path.
- Done: Create graphics pipeline.
- Done: Render a colored triangle.
- Done: Verify macOS first.

Success criteria:

- Done: Renders a nonblank triangle.
- Done: Existing 2D games still link.

### Milestone 2.5: Debug Primitives And Depth

- Done: Add `GpuFrame` so renderers can use command buffers safely inside a frame.
- Done: Add optional depth texture management in `GpuContext`.
- Done: Add `PrimitiveRenderer3D`.
- Done: Render multiple primitive shapes with different projected depths.
- Done: Enable depth testing in the primitive pipeline.

Success criteria:

- Multiple overlapping primitives render with depth ordering.
- Demo code does not contain SDL_gpu shader/pipeline boilerplate.

### Milestone 3: Cube And Camera

- Done: Add `Mat4`, `Transform3D`, and `PerspectiveCamera`.
- Done: Add `Vertex3D`, `GpuMesh`, and `MeshRenderer3D`.
- Done: Render a non-indexed cube mesh.
- Done: Promote current depth support into the cube/mesh renderer path.
- Done: Add basic orbit camera input using existing mouse contexts in `games:rubiks-cube-3d`.
- Next: Add indexed mesh support.

Success criteria:

- Done: Rotating cube demo.
- Done: Depth testing works.

### Milestone 3.5: Rubik's Cube Demo

- Done: Add `games:rubiks-cube-3d`.
- Done: Add `CubeFaceColors` so each cubie can assign visible stickers and dark internal faces.
- Done: Add `MeshRenderer3D.draw` overload for direct model matrices.
- Done: Build cubelet state with integer grid positions and per-cubie orientation matrices.
- Done: Animate face/slice turns before committing cubie positions.
- Done: Add keyboard face turns plus scramble/reset.
- Done: Add mouse-drag orbit.
- Done: Add ray casting against cube faces for direct mouse-picked face turns.
- Next: Refine picked swipes so clicking a sticker can select rows/columns, not only the outer face.

Success criteria:

- Done: Renders a full colored 3x3x3 cube.
- Done: Scramble queues multiple animated turns.
- Done: Manual turns update both position and cubie orientation.
- Done: Clicking outside the cube orbits; clicking a visible face and swiping turns that face.

### Milestone 4: Textures, Materials, And Model Facade

- Done: Add procedural RGBA8 texture creation.
- Done: Upload texture pixels to `SDL_GPUTexture`.
- Done: Add sampler support.
- Done: Add `TextureVertex3D` and `TexturedGpuMesh`.
- Done: Add simple unlit textured renderer/material path.
- Done: Render a textured cube in `games:kengine-3d-demos`.
- Done: Load image pixels from Kengine assets.
- Done: Load textured OBJ models with UVs for the demo scene.
- Done: Add reusable `Material3D`, `ModelPart3D`, `Model3D`, `ModelRenderer3D`, `ParsedModel3D`, and `ModelLoader3D` wrappers so simple static models do not need to expose loader-specific return types.
- Next: Continue moving direct OBJ/GLB usage in demos behind `ModelLoader3D` where animation-specific APIs are not needed.

Success criteria:

- Done: Textured cube or plane.
- Done: Textured OBJ model.
- Done: Mario world and Bowser static GLB assets render through the generic `Model3D` facade.

### Milestone 4.5: 3D Gameplay Test Bed

- Done: Add `games:kengine-3d-space-shooter`.
- Done: Add generated sphere meshes for shots and powerups.
- Done: Add a generated terrain mesh that scrolls through the scene.
- Done: Add keyboard flight controls with a terrain floor clamp.
- Done: Add turrets that fire sphere projectiles toward the player.
- Done: Add player beams, turret hit detection, health pickups, beam upgrades, and rectangle HUD meters.
- Done: Use the imported Kenney craft model as the foreground player ship.
- Next: Add imported turret models, a follow camera, richer terrain collision, and controller support.

Success criteria:

- Done: Playable first slice that exercises meshes, textured meshes, spheres, generated terrain, depth, and HUD primitives.

### Milestone 5: GLB Loader

- Done: Add small Kotlin-side GLB 2.0 loader.
- Done: Load mesh positions, normals, UVs, indices.
- Done: Load base color textures from embedded GLB images.
- Done: Load node-transform animation clips for rigid/node animated models.
- Done: Load skinned textured meshes and update skinning on CPU.
- Next: Add external `.gltf` JSON plus external buffer/image support, or explicitly scope the public API to binary `.glb` until needed.
- Next: Move CPU skinning toward shader/GPU buffers once animation API shape is stable.

Success criteria:

- Done: Render static textured GLB world and Bowser models.
- Done: Render skinned animated Mario GLB.
- Done: Render node-animated Goomba GLB.

### Milestone 6: Scene Convenience

- Done: Extract reusable third-person camera/follow controls from `games:mario-3d`.
- Done: Add `AnimationPlayer3D` and clip lookup helpers so games do not hand-roll state-to-clip timing.
- Done: Add lightweight `Scene3D` and `SceneRenderer3D` so games can submit ordered static models and mesh primitives through reusable scene items.
- Done: Lift GLB animated/skinned models behind a format-neutral animated model facade.
- Done: Add `AnimatedModelInstance3D` so scene items can retain per-instance transform, visibility, light override, and animation pose instead of being rebuilt inside render passes.
- Next: Add `Node3D` or entity wrapper if needed.
- Next: Replace CPU-skinned mesh mutation with GPU skinning or per-instance skin buffers so many skinned instances can share one animated asset with different poses.
- Decide whether 3D should integrate with Kengine's current scene system.

## Open Questions

- Should `kengine-3d` be native-only initially, or mirror all KMP targets for publication consistency?
- Should `SDLContext` expose raw SDL pointers publicly, or via internal accessors?
- Do we want 2D overlays on top of 3D in the first version?
- If yes, should overlays use a new GPU-backed 2D renderer instead of SDL_Renderer?
- How should shader compilation run in CI?
- Do we want to support Playdate/WASM separately, or explicitly exclude 3D there?

## Recommendation

Start with SDL_GPU and build the smallest possible vertical slice:

1. Backend-aware `SDLContext`.
2. `GpuContext`.
3. Clear screen.
4. Triangle.
5. Cube.
6. Camera.
7. Texture.
8. glTF.

That path keeps `kengine-3d` compatible with the existing engine architecture while avoiding a large dependency and build-system jump before we know the rendering boundary is right.
