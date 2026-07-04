# Mario 3D

Experimental third-person 3D platformer/control demo for Kengine.

## Assets

- `assets/models/Super Mario 64 Bob-Omb Battlefield.glb`: GLB world mesh used for rendering and terrain collision.
- `assets/models/Mario 64 Odyssey Static.glb`: current Mario player mesh, stripped from the full animation source GLB so the repo stays small.
- `assets/models/Animated Goomba Super Mario Bros.glb`: Goomba enemy mesh used for terrain-following patrols.
- `assets/models/Ridley64.glb`: large landmark/enemy mesh placed on the highest sampled terrain point.

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
3. Add GLB animation clip parsing/playback for Mario and Goomba movement. The full source Mario GLB is kept outside the repo at `~/code/mario64-assets/assets/models/` and includes clips such as `Run`, `Walk`, `Jump`, `Fall`, `Land`, and `Squat*`.
4. Add enemy collision and stomp/bump interactions.
5. Add richer collision volumes, slope limits, ledges, world bounds, and debug overlays.
6. Add textured GLB material support so imported worlds and characters render closer to the source assets.
