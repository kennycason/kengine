# Kengine 3D Model Viewer

Top-level Kengine tooling app for exercising `kengine-3d` model loading and rendering outside a game.

This first slice opens an `SDL_GPU_3D` window, loads a model, renders it through `Scene3D`, previews the first animation clip when the model is animated, and provides orbit camera controls.

## Run

```shell
./gradlew :kengine-3d-model-viewer:runDebugExecutableMacosArm64
```

The default asset is Mario's animated GLB from `../games/mario-3d/assets`, relative to this module's Gradle run directory.

To load another model:

```shell
./gradlew :kengine-3d-model-viewer:linkDebugExecutableMacosArm64
cd kengine-3d-model-viewer
./build/bin/macosArm64/debugExecutable/kengine-3d-model-viewer.kexe --model "models/Super Mario 64 Bowser.glb" --mode static
```

## Options

- `--model <path>`: model path relative to `--asset-root`, or an absolute path.
- `--asset-root <path>`: source asset root. Defaults to `../games/mario-3d/assets`.
- `--mode <mode>`: `static`, `node`, or `skinned`. Defaults to `skinned`.
- `--target-size <size>`: normalized model target size. Defaults to `2.2`.
- `--clip <name>`: animated clip name to preview.

## Controls

- Mouse drag: orbit camera.
- Up/Down arrows: zoom.
- Left/Right arrows: pan target.
- Escape: quit.

## Next

The existing Kengine UI components are currently SDL renderer-backed, while this viewer uses the `SDL_GPU_3D` backend. The next viewer slice should add a deliberate GPU-compatible inspector surface instead of mixing the old 2D renderer path into the same window by accident.
