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
- Core `kengine` provides `CalibratedControllerAxes`, `ControllerAxisInputSettings`, `digitalAxis`, and `snapAxis` so demos can keep simple per-game controls while reusing controller neutral calibration and deadzone shaping.
- `kengine-3d` provides the first mesh path: `GpuMesh`, `CubeFaceColors`, `Vertex3D`, `MeshRenderer3D`, `Mat4`, and `Transform3D`.
- `kengine-3d` provides `DebugRenderer3D` for reusable 3D debug lines, rays, wire spheres, wire capsules, wire AABBs, collider overloads, and contact point/normal markers.
- `kengine-3d` provides the first texture/material path: `GpuTexture`, `GpuTextureAsset3D`, `GpuTextureCache3D`, `MaterialDescriptor3D`, `GpuTextureDescriptor3D`, `GpuSamplerDescriptor3D`, `GpuTextureUploadDescriptor3D`, `GpuResourceOwnership3D`, `TextureVertex3D`, `TexturedGpuMesh`, and `TexturedMeshRenderer3D`. `GpuTexture` can create procedural RGBA textures or upload image files loaded through SDL_image while keeping texture source, shape, sampler policy, upload layout, shared-cache reuse, material upload, and owned/borrowed cleanup explicit.
- `kengine-3d` provides the first lit mesh path: `LitGpuMesh`, `LitVertex3D`, `LitMeshRenderer3D`, and `DirectionalLight3D`.
- `kengine-3d` provides textured lit mesh paths: `TexturedLitGpuMesh`, `TexturedLitVertex3D`, and `TexturedLitMeshRenderer3D` for regular textured-lit meshes, plus `SkinnedTexturedLitVertex3D`, `SkinnedTexturedLitGpuMesh`, and `SkinnedTexturedLitMeshRenderer3D` as the first shader-skinning path.
- `kengine-3d` provides `GpuShader3D` helpers plus generated `Kengine3DShaderSources`, `Kengine3DShaderArtifacts`, and `Kengine3DShaderPrograms` catalogs so renderers share backend-aware SDL GPU shader artifact selection, generated Metal libraries with MSL fallback, stage-aware error messages, shader cleanup behavior, shader resource declarations, and engine-owned shader source files under `src/nativeMain/shaders`.
- `kengine-3d` provides `GpuPipeline3D` helpers so renderers share graphics pipeline descriptors, vertex input layouts, depth settings, and SDL GPU pipeline creation.
- `kengine-3d` provides `GpuDraw3D` and `GpuUniforms3D` helpers so renderers share uniform packing, vertex-buffer binding, texture-sampler binding, and primitive draw submission.
- `kengine-3d` provides `GpuRendererPreset3D`, `Kengine3DVertexLayouts`, and `Kengine3DRendererPresets` so built-in renderer/material styles pair shader programs with matching pipeline layouts in one catalog.
- `kengine-3d` provides `GpuUpload3D` and `GpuVertexBuffer3D` helpers so textures and mesh classes share upload transfer buffers, copy-pass submission, vertex packing, GPU vertex-buffer creation, texture uploads, and mutable vertex-buffer updates.
- `kengine-3d` provides `ObjMeshLoader`, a lightweight Wavefront OBJ path that reads vertices/UVs/faces, triangulates polygons, resolves `.mtl` diffuse colors, normalizes imported geometry, and emits `GpuMesh`, `LitGpuMesh`, or `TexturedLitGpuMesh` data.
- `kengine-3d` provides `ModelLoader3D`, `ModelAsset3D`, `ModelAssetLoader3D`, `ModelAssetPathResolver3D`, `ModelSourceCache3D`, `ParsedModel3D`, `ModelPartSource3D`, `Model3D`, `ModelInfo3D`, `ModelPart3D`, `MaterialDescriptor3D`, `Material3D`, and `ModelRenderer3D` as the first reusable model/material facade over the lower-level OBJ/GLB paths. Callers can parse a CPU model source once, optionally keep it in an explicit `ModelSourceCache3D`, reuse it for collision, and upload it into GPU-backed `Model3D` resources with an optional long-lived `GpuTextureCache3D`; default loads still own and clean up their per-load texture cache.
- `kengine-3d` provides `Scene3D`, `Node3D`, scene item types, and `SceneRenderer3D` for ordered per-frame submission of static models, animated models, and mesh primitives through one renderer bundle. `Node3D` includes fluent visibility, transform, position/yaw, and animated-pose helpers for simple actor-to-node sync.
- `kengine-3d` provides `GpuResourceScope3D`, a small cleanup scope for GPU-backed resources owned by demos and games.
- `kengine-3d` provides `GlbMeshLoader`, a Kotlin-side GLB 2.0 path that loads static textured/lit CPU source parts, fills engine-owned animated lit/skinned CPU source descriptors, uploads textured/lit meshes, CPU-updated per-instance skinned textured meshes, GPU-ready skinned textured source vertices, node-transform animation clips, embedded textures with parsed sampler wrap/filter intent, per-load or caller-owned texture deduping, model-owned cached texture cleanup for default loads, and CPU-side lit vertices for collision.
- `kengine-3d` provides `AnimationClipSet3D`, `AnimationClipMap3D`, `AnimationPose3D`, `AnimationPosePreparation3D`, `AnimationPlayer3D`, and `AnimationStateController3D` for reusable clip lookup, pose values, explicit pose preparation, stateful animation time advancement, and state-to-pose playback.
- `kengine-3d` provides `AnimatedModel3D`, `AnimatedModelSource3D`, `AnimatedModelSourceCache3D`, `AnimatedModelAsset3D`, `AnimatedModelLoader3D`, `AnimatedModelInstanceRenderState3D`, and `AnimatedModelInstance3D` as the first format-neutral facade over node-animated, CPU-skinned, and GPU joint-palette model source parsing, engine-owned source descriptors, source caching, upload, playback, per-instance transform/pose/render state, and pre-render pose preparation.
- `kengine-3d` provides `TerrainMeshCollider3D`, `StaticMeshCollider3D`, `Collision3D`, `KinematicCharacterController3D`, and `TerrainActorController3D` for the current 3D gameplay collision baseline.
- `games:kengine-3d-demos` opens a GPU-backed window, uses mouse-drag orbit camera controls plus arrow-key zoom/pan, and renders separated rows of multiple primitives, a rotating color cube, a rotating textured cube, clean color-lit Kenney Space Kit ship/turret OBJ models, and a separate UV-textured meteor OBJ model using a PNG loaded from disk.
- `games:mario-3d` is now the main 3D platformer validation bed: textured GLB world rendering, terrain/static collision from parsed model vertices, reusable third-person camera controls, reusable controller axis calibration, default `AUTO` GPU joint-palette skinned Mario animation, asset-bound scene nodes, animated enemies, debug drawing, and a simple Bowser encounter.
- `games:rubiks-cube-3d` renders a 27-cubie Rubik's cube with per-face colors, mouse orbit, mouse-picked face turns, keyboard face turns, animated slice rotations, scramble, and reset.
- `kengine-3d-model-viewer` is a top-level 3D tooling app that opens an SDL GPU window, loads static or animated model assets through the reusable source/upload path, previews animated clips, and provides orbit camera controls outside a game demo.

Implementation note: the SDL GPU declarations are already generated by the core `SDL3/SDL.h` cinterop in `kengine`; a separate `sdl3_gpu.def` produced an effectively empty binding and is not needed.

Current shader note: the 3D renderers now load shader programs through a backend-aware generated Kotlin shader catalog backed by files in `kengine-3d/src/nativeMain/shaders`. `compileKengine3dShaderArtifacts` establishes `build/generated/kengine3dShaders/nativeMain/{metallib,spirv,dxil}`; when Xcode's `metal` and `metallib` tools are available, it fills `metallib` and the generator packages those byte arrays while keeping MSL source as fallback. `reportKengine3dShaderTools` reports optional compiler availability. The runtime can select among shader artifacts reported by `SDL_GetGPUShaderFormats`. Before treating the renderer as cross-platform, wire a cross-compiler into the existing SPIR-V and DXIL artifact directories.

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
- Compile Metal libraries from the checked-in MSL files when Xcode tools are available.
- Use the existing generated `spirv` and `dxil` artifact directories when adding Vulkan and D3D12 compiler tasks.

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
- Done: Add `ModelAsset3D`, `AnimatedModelAsset3D`, `ModelAssetPathResolver3D`, and `ModelAssetLoader3D` so games can declare model assets once, use packaged/source asset fallback, and route static/animated loads through a reusable facade.
- Next: Continue moving direct OBJ/GLB usage in demos behind `ModelAssetLoader3D` where animation-specific APIs are not needed.

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
- Done: Add per-instance CPU-skinned mesh buffers for skinned textured GLB animated model instances.
- Done: Add `SkinnedTexturedLitVertex3D`, `SkinnedTexturedLitGpuMesh`, and `SkinnedTexturedLitMeshRenderer3D` so skinned source vertices and joint matrices can be drawn by a shader path.
- Done: Add `AnimatedModelSkinningMode3D.AUTO` as the default policy, preferring `GPU_JOINT_PALETTE` when the asset fits the renderer's joint limit and falling back to `CPU_VERTEX_BUFFER` otherwise.
- Done: Validate Mario's shader-skinning path visually and let Mario rely on the default `AUTO` mode instead of opting into a renderer path in game code.
- Done: Add skinned GLB support reporting and stricter skinned primitive validation for missing skinning accessors, accessor count mismatches, invalid skin/mesh references, non-triangle primitives, and out-of-range joint indices.

Success criteria:

- Done: Render static textured GLB world and Bowser models.
- Done: Render skinned animated Mario GLB.
- Done: Render node-animated Goomba GLB.

### Milestone 6: Scene Convenience

- Done: Extract reusable third-person camera/follow controls from `games:mario-3d`.
- Done: Add `AnimationPlayer3D` and clip lookup helpers so games do not hand-roll state-to-clip timing.
- Done: Add `AnimationStateController3D` so games can keep their own state-selection rules while reusing enum-state-to-pose playback.
- Done: Add lightweight `Scene3D` and `SceneRenderer3D` so games can submit ordered static models and mesh primitives through reusable scene items.
- Done: Lift GLB animated/skinned models behind a format-neutral animated model facade.
- Done: Add `AnimatedModelInstance3D` so scene items can retain per-instance transform, visibility, light override, and animation pose instead of being rebuilt inside render passes.
- Done: Add `Node3D` as a lightweight typed wrapper around one scene item, with parent transform/item transform composition, node-level visibility, position/yaw transform helpers, and animated-pose sync helpers.
- Done: Add `DebugRenderer3D` collider overloads and contact markers so games can debug common 3D collision shapes without repeating draw glue.
- Done: Centralize explicit pose preparation in `AnimationPosePreparation3D`, keeping shared-buffer animated model limitations guarded in one place.
- Done: Add `AnimatedModelInstanceRenderState3D` plus per-instance CPU-skinned GLB buffers so many skinned instances can share one animated asset with different poses.
- Done: Add the first additive shader-skinning renderer path and GLB GPU-skinned instance type while preserving the CPU-skinned fallback.
- Done: Route skinned animated model instances through selectable `AUTO`, CPU, or GPU skinning modes.
- Done: Add an early joint-count guard for GPU joint-palette skinning.
- Done: Make `AUTO` the default animated skinning mode for skinned GLB assets.
- Done: Harden promoted shader-skinning errors with stage-aware MSL shader creation messages, detailed pipeline context, and safe shader release on renderer init failures.
- Done: Migrate the shared shader helper across the older primitive, mesh, lit mesh, textured mesh, textured lit mesh, debug, and skinned textured lit renderers.
- Done: Move renderer MSL source into `src/nativeMain/shaders` and generate the internal `Kengine3DShaderSources` catalog during the `kengine-3d` build.
- Done: Generate paired shader program descriptors in `Kengine3DShaderPrograms` so renderers no longer repeat labels, source pairs, uniform buffer counts, or sampler counts.
- Done: Add backend-aware shader artifact descriptors and runtime selection from the active SDL GPU device's supported shader formats.
- Done: Compile/package per-stage METALLIB artifacts when Xcode shader tools are available, while keeping MSL source artifacts as fallback.
- Done: Generalize generated shader artifact packaging for METALLIB, SPIR-V, and DXIL and add `reportKengine3dShaderTools` for optional compiler discovery.
- Done: Add `GpuPipeline3D` descriptors so renderers share vertex layouts, primitive/depth settings, SDL GPU pipeline creation, and pipeline failure context.
- Done: Add `GpuDraw3D` and `GpuUniforms3D` helpers so renderers share uniform packing, vertex-buffer binding, texture-sampler binding, and primitive draw submission.
- Done: Add `GpuRendererPreset3D`, `Kengine3DVertexLayouts`, and `Kengine3DRendererPresets` so built-in renderer/material styles pair shader programs with matching pipeline layouts in one catalog.
- Done: Consolidate repeated GPU vertex-buffer creation, transfer-buffer upload, and mutable mesh update code across mesh classes with `GpuVertexBuffer3D`.
- Done: Consolidate texture transfer-buffer upload and copy-pass helpers where they overlap with the vertex-buffer upload path.
- Done: Add texture, sampler, and upload descriptors so texture creation defaults, address modes, filters, and upload metadata are not hard-coded in `GpuTexture`.
- Done: Add a small texture asset/cache layer so file, embedded GLB, procedural fallback, and repeated material textures share one simple loading path.
- Done: Add texture ownership controls so materials and GLB parts can borrow cached textures without releasing them directly.
- Done: Expose a long-lived `GpuTextureCache3D` option through GLB, model, animated model, and asset loaders for cross-model texture reuse.
- Done: Introduce `MaterialDescriptor3D` so imported models and game-authored materials can share one material upload path backed by optional texture-cache ownership.
- Done: Separate parsed CPU model sources from uploaded GPU model resources so inspection, collision, rendering, and caching can share one model data flow.
- Done: Add an optional `ModelSourceCache3D` to `ModelAssetLoader3D` so repeated source loads can reuse CPU data explicitly.
- Done: Lift animated GLB parsing behind a cacheable `AnimatedModelSource3D` shape so skeletons, clips, and skinned source meshes follow the same source/upload split as static models.
- Done: Collapse GLB-specific animated source wrappers behind engine-owned source descriptors so future animation formats can reuse the same upload surface.
- Done: Move shared animation runtime primitives for nodes, skins, matrices, and clip sampling out of `GlbMeshLoader` so GLB is only a file parser.
- Done: Add an optional model asset bundle/preload helper so games can declare static and animated model assets once and warm source/texture caches up front.
- Done: Add asset-bound scene and collider helpers so loaded bundles can create terrain/static colliders and scene nodes without repeating lookup glue in games.
- Done: Continue shrinking Mario startup by moving game-local actor setup and actor-to-node sync behind small builders backed by asset-bound scene nodes and animation controllers.
- Done: Add the first top-level `kengine-3d-model-viewer` executable so model loading, animation preview, scene rendering, and orbit camera behavior can be validated outside a game demo.
- Next: Add a GPU-compatible inspector surface to `kengine-3d-model-viewer` for model metadata, animation clip selection, material/texture diagnostics, and camera controls.
- Next: Wire SDL_shadercross/dxc/glslang into the existing SPIR-V and DXIL artifact directories.
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
