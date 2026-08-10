# N64 Demo

Hardware-focused 2D/3D diagnostics for Kengine's Nintendo 64 backend.

## Run

```shell
./gradlew :games:n64-demo:runN64 -Pkengine.enableNintendo64=true
```

## Asset Pipeline

The ROM uses redistributable Kenney Space Kit OBJ fixtures. Local Mario/Metroid assets can be inspected as private compatibility benchmarks, but they are not baked into the default ROM data.

Generate the baked demo model arrays:

```shell
./gradlew :games:n64-demo:generateN64DemoModelAssets
```

Write a hardware-budget report for candidate OBJ/GLB assets:

```shell
./gradlew :games:n64-demo:preflightN64DemoAssets
```

The report is written to:

```text
games/n64-demo/build/reports/n64-demo-assets/preflight.md
```

Generate N64-sized RGBA16-style texture preview PNGs:

```shell
./gradlew :games:n64-demo:downresN64DemoTextures
```

The previews are written to:

```text
games/n64-demo/build/n64-demo-assets/downres/
```

## Current Budgets

- Model projection cache: 512 vertices.
- Filled model triangles: 96 per frame.
- Overlay edges: 22 per frame.
- Texture preflight target: 64px max dimension, 8px minimum dimension, RGBA16 byte estimates.

Large worlds such as Bob-Omb Battlefield should go through chunking/LOD before being considered for hardware rendering.
