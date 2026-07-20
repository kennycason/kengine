# Mario 3D

Experimental third-person 3D platformer/control demo for Kengine.

## Assets

- `assets/models/Super Mario 64 Bob-Omb Battlefield.glb`: textured GLB world mesh used for rendering and terrain collision.
- `assets/models/Mario 64 Model.glb`: small textured Mario player mesh. This file has no skins or animation clips, so it is useful for static textured character rendering but not skeletal playback.
- `assets/models/Mario64Animated.glb`: repo-safe split from the large Odyssey source GLB. It keeps Mario's skinned mesh/skeleton/textures and the idle, walk, run, jump, fall, land, hip-drop, punch, damage, crouch, and brake clips.
- `assets/models/Animated Goomba Super Mario Bros.glb`: Goomba enemy mesh used for terrain-following patrols and node-transform animation playback.
- `assets/models/Super Mario 64 Bowser.glb`: small textured Bowser landmark/enemy mesh placed on the mountain summit. This file also has no skins or animation clips.

The larger `Mario 64 Odyssey All Animations 2025.glb` source file remains outside the repo at `~/code/mario64-assets/assets/models/` because it is larger than GitHub's regular file limit. That file is the one that contains a skin and many animation clips.

To regenerate the split animated Mario asset:

```shell
python3 tools/extract_glb_animations.py "$HOME/code/mario64-assets/assets/models/Mario 64 Odyssey All Animations 2025.glb" games/mario-3d/assets/models/Mario64Animated.glb
```

## Run

```shell
./gradlew :games:mario-3d:runDebugExecutableMacosArm64
```

## Controls

- Keyboard: `WASD` moves Mario relative to the camera, Shift or `B` runs, arrow keys orbit the camera with non-inverted Y look, `T` cycles camera distance, and `X`/space jumps.
- Controller: left stick moves Mario relative to the camera, Square runs, right stick smoothly orbits the camera with non-inverted Y look, Triangle cycles camera distance, and Cross jumps.

## Build Plan

1. Keep the platformer baseline stable: third-person follow camera, camera-relative movement, jump physics, and height-aware terrain collision.
2. Continue moving validated helpers into reusable Kengine modules; controller axis calibration/deadzone shaping, model asset descriptors/path resolution, cached parse-once static and animated source upload, collision reuse, static Bowser model loading, debug collider/contact drawing, third-person camera controls, animation state playback, ordered scene submission, animated model facades, per-instance skinned render state, and actor-to-node render sync now use reusable engine APIs.
3. Keep Mario on the default `AUTO` animated skinning path, which now uses GPU joint-palette skinning for this asset and keeps CPU skinning as the engine fallback for larger skeletons.
4. Add action-state controls for the already extracted hip-drop, punch, damage, crouch, and brake clips.
5. Add richer collision volumes, slope limits, ledges, world bounds, and debug overlays.
