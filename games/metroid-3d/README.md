# Metroid 3D

Experimental third-person 3D platformer/control demo for Kengine.

## Assets

- `assets/models/metroid3d/Super Mario 64 Bob-Omb Battlefield.glb`: first world target for the GLB loader.
- `assets/models/metroid3d/Super Mario 64 Bob-Omb Battlefield/`: source texture/package folder for the world.
- `assets/models/metroid3d/TorvusBog3D.glb`: large world target for later streaming/culling work.
- `assets/models/metroid3d/Samus64.glb`: future GLB Samus target.
- `assets/models/metroid3d/Samus Super Smash Bros N64/samus.obj`: current bootstrap Samus model loaded with the existing OBJ path.
- `assets/models/metroid3d/*.glb`: creatures/props for later scene population.

## Run

```shell
./gradlew :games:metroid-3d:runDebugExecutableMacosArm64
```

## Controls

- Keyboard: `WASD` moves Samus relative to the camera, arrow keys orbit the camera, `T` cycles camera distance, `X`/space jumps, and `F` shoots.
- Controller: left stick moves Samus relative to the camera, right stick smoothly orbits the camera, Triangle cycles camera distance, Cross jumps, and Square shoots.
- Aim mode is intentionally disabled while the baseline third-person controls are tuned. The planned aiming pass is a Zelda-style lock/aim stance that starts from the current camera direction instead of snapping the view.

## Build Plan

1. Bootstrap the game loop with a follow camera, Samus OBJ model, Bob-Omb Battlefield GLB geometry, third-person movement, jump, basic projectile shooting, and first-pass terrain collision.
2. Add material/texture support for embedded GLB images and external texture files.
3. Improve third-person platformer physics: slope limits, ledges, jump arcs, gravity tuning, and world bounds.
4. Load Samus from `Samus64.glb`, then add animation support if the asset contains animation channels.
5. Add lock-on/cursor ray tests, camera smoothing, collision debug overlays, and enemy/prop placement.
6. Move to `TorvusBog3D.glb` once culling/partitioning is available.
