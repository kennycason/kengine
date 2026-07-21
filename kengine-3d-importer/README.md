# Kengine 3D Importer

Top-level tooling module for deciding how model assets should enter `kengine-3d`.

Runtime loading stays in `kengine-3d` and currently supports:

- `.glb`
- `.gltf`
- `.obj`

The importer module owns asset preflight for source formats that should not be parsed directly by games. It validates runtime-ready files with `ModelLoader3D.inspect`, marks `.fbx` and USD-family files as requiring external GLB export, and returns a clear unsupported-format message for everything else. It does not run Blender, Assimp, the FBX SDK, or any other converter.

## Run

```shell
./gradlew :kengine-3d-importer:linkDebugExecutableMacosArm64
./kengine-3d-importer/build/bin/macosArm64/debugExecutable/kengine-3d-importer.kexe models/source.fbx
```

Use `--suggested-output` to choose the suggested runtime GLB path reported for source formats:

```shell
./kengine-3d-importer/build/bin/macosArm64/debugExecutable/kengine-3d-importer.kexe models/source.usdz --suggested-output models/source.glb
```

## Next

- Add batch preflight manifests once game/tool asset folders need repeatable checks.
- Add packaging checks for model folders with external `.gltf` buffers/images or OBJ `.mtl` textures.
- Add optional asset-copy helpers for moving validated runtime models into game/tool asset directories.
