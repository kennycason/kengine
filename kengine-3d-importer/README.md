# Kengine 3D Importer

Top-level tooling module for deciding how model assets should enter `kengine-3d`.

Runtime loading stays in `kengine-3d` and currently supports:

- `.glb`
- `.gltf`
- `.obj`

The importer module owns the offline path for source formats that should not be parsed directly by games. The first slice is a tested planner plus CLI: it identifies runtime-ready files, marks `.fbx` and USD-family files as GLB conversion candidates, and returns a clear unsupported-format message for everything else.

## Run

```shell
./gradlew :kengine-3d-importer:linkDebugExecutableMacosArm64
./kengine-3d-importer/build/bin/macosArm64/debugExecutable/kengine-3d-importer.kexe models/source.fbx
```

Use `--output` to choose the planned GLB path:

```shell
./kengine-3d-importer/build/bin/macosArm64/debugExecutable/kengine-3d-importer.kexe models/source.usdz --output models/source.glb
```

## Next

- Add external converter adapters behind this module, starting with a Blender-backed GLB export path.
- Add validation that the converted GLB can be inspected by `ModelLoader3D`.
- Add batch import manifests once game/tool asset folders need repeatable conversion.
