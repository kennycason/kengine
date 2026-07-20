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

- The inspector panel can cycle bundled model presets, open a native `.glb`/`.gltf`/`.obj` file picker with `LOAD`, cycle animation clips, play/pause/stop playback, adjust animation speed, switch lighting/background presets, tune ambient/diffuse strength, toggle axes, and reset the view.
- Mouse drag: orbit camera.
- Up/Down arrows: zoom.
- Left/Right arrows: pan target.
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

- Add camera preset/save controls.
- Add a one-click "show in importer" handoff for source formats once `kengine-3d-importer` has converter adapters.
- Add drag-and-drop model loading once Kengine has a small native drop-target story.
