# Mario 3D

Experimental third-person 3D platformer/control demo for Kengine.

## Assets

- `assets/models/Super Mario 64 Bob-Omb Battlefield.glb`: textured GLB world mesh used for rendering and terrain collision.
- `assets/models/Mario 64 Model.glb`: small textured Mario player mesh. This file has no skins or animation clips, so it is useful for static textured character rendering but not skeletal playback.
- `assets/models/Animated Goomba Super Mario Bros.glb`: Goomba enemy mesh used for terrain-following patrols and node-transform animation playback.
- `assets/models/Super Mario 64 Bowser.glb`: small textured Bowser landmark/enemy mesh placed on the mountain summit. This file also has no skins or animation clips.

The larger `Mario 64 Odyssey All Animations 2025.glb` source file remains outside the repo at `~/code/mario64-assets/assets/models/` because it is larger than GitHub's regular file limit. That file is the one that contains a skin and many animation clips.

## Run

```shell
./gradlew :games:mario-3d:runDebugExecutableMacosArm64
```

## Controls

- Keyboard: `WASD` moves Mario relative to the camera, arrow keys orbit the camera, `T` cycles camera distance, and `X`/space jumps.
- Controller: left stick moves Mario relative to the camera, right stick smoothly orbits the camera, Triangle cycles camera distance, and Cross jumps. L2 is reserved for crouch once GLB animation playback is available.

## Build Plan

1. Keep the platformer baseline stable: third-person follow camera, camera-relative movement, jump physics, and height-aware terrain collision.
2. Move terrain collision and third-person follow camera helpers into `kengine-3d` once the Mario demo validates their shape.
3. Add skeletal skinning for the large external Mario source GLB now that node-transform animation playback works for simple files like Goomba.
4. Extract only the needed Mario clips into repo-safe files once skeletal playback works.
5. Add enemy collision and stomp/bump interactions.
6. Add richer collision volumes, slope limits, ledges, world bounds, and debug overlays.
