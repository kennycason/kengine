# Kengine 3D Model Viewer

Top-level Kengine tooling app for exercising `kengine-3d` model loading and rendering outside a game.

This viewer opens an `SDL_GPU_3D` window, loads bundled demo models or selected local `.glb`/`.gltf`/`.obj` files, renders through `Scene3D`, previews animation clips when the model is animated, and provides orbit camera plus a clickable `kengine-3d-ui` inspector.

## Run

```shell
./gradlew :kengine-3d-model-viewer:runDebugExecutableMacosArm64
```

The default asset is Mario's animated GLB from this module's bundled `assets/` directory.

To load another model:

```shell
./gradlew :kengine-3d-model-viewer:linkDebugExecutableMacosArm64
cd kengine-3d-model-viewer
./build/bin/macosArm64/debugExecutable/kengine-3d-model-viewer.kexe --model "models/Super Mario 64 Bowser.glb" --mode static
```

## Options

- `--model <path>`: model path relative to `--asset-root`, or an absolute path.
- `--asset-root <path>`: source asset root. Defaults to `assets`.
- `--mode <mode>`: `auto`, `static`, `node`, or `skinned`. Defaults to `skinned`.
- `--target-size <size>`: normalized model target size. Defaults to `2.2`.
- `--clip <name>`: animated clip name to preview when mode is `auto`, `node`, or `skinned`.

Supported model formats are `.glb`, `.gltf`, and `.obj`.

## Controls

- The inspector panel uses `VIEW`, `ANIM`, `LIGHT`, and `ASSET` panes. Model selection and `LOAD` stay available, while pane controls cover camera/debug view controls, animation playback, lighting/background tuning, and asset diagnostics/preflight.
- Mouse drag: orbit camera.
- Up/Down arrows: zoom.
- Left/Right arrows: pan target.
- `1` / `2` / `3` / `4`: camera presets: front, three-quarter, side, top.
- `P`: preflight the active model with `kengine-3d-importer`.
- `A`: preflight every model preset and print a batch asset-health report.
- `M` / `N`: next/previous built-in model preset.
- `C` / `V`: next/previous animation clip.
- Space: pause/resume animation.
- `Z` / `X`: decrease/increase animation playback speed.
- `B`: cycle background preset.
- `L`: cycle lighting preset.
- `J` / `K`: decrease/increase ambient light.
- `U` / `I`: decrease/increase diffuse light.
- `G`: toggle axes.
- `R`: reset viewer controls and camera.
- `H` or `F1`: print controls.
- `T`: print current status.
- Escape: quit.

## Next

- Viewer polish:
  - Save/restore named camera views once persistence exists.
  - Add deeper focused panes for materials, skins, bounds, and load diagnostics.
  - Add drag-and-drop model loading once Kengine has a small native drop-target story.
- Material rendering:
  - Add PBR-lite rendering for metallic/roughness/specular/emissive slots already captured in model metadata.
  - Add alpha-mode handling for transparent/cutout materials.
  - Keep displacement as metadata until the renderer has a deliberate tessellation/parallax story.
- Renderer foundations:
  - Add indexed mesh buffers for imported geometry.
  - Add model bounds, frustum culling, and viewer bounds visualization.
  - Add instancing support for repeated scene objects.
  - Add reusable picking/raycast helpers for tools and games.
- Mario cleanup:
  - Continue moving game-local glue behind `kengine-3d` asset, scene, animation, and controller helpers.
  - Use model-viewer and importer preflight as the asset validation path before Mario consumes new models.
