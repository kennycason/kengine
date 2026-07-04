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

- Keyboard: `WASD` moves Mario relative to the camera, Shift or `B` runs, arrow keys orbit the camera, `T` cycles camera distance, and `X`/space jumps.
- Controller: left stick moves Mario relative to the camera, Square runs, right stick smoothly orbits the camera, Triangle cycles camera distance, and Cross jumps.

## Build Plan

1. Keep the platformer baseline stable: third-person follow camera, camera-relative movement, jump physics, and height-aware terrain collision.
2. Move terrain collision and third-person follow camera helpers into `kengine-3d` once the Mario demo validates their shape.
3. Continue improving CPU skeletal playback, then move skinning to shader/GPU buffers once the API shape is stable.
4. Add action-state controls for the already extracted hip-drop, punch, damage, crouch, and brake clips.
5. Add enemy collision and stomp/bump interactions.
6. Add richer collision volumes, slope limits, ledges, world bounds, and debug overlays.
