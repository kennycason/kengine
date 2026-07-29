# Hextris Core

Shared portable Hextris game logic, rendering commands, audio commands, and asset declarations. Platform modules such as `:games:hextris-desktop` and `:games:hextris-switch` instantiate `hextris.HextrisGame` instead of duplicating gameplay code or asset names.

The module declares portable assets with `kenginePortableAssets`:

```kotlin
kenginePortableAssets {
    packageName.set("hextris")
    objectName.set("HextrisAssets")

    spriteSheet("blocks") {
        id.set("hextris/block-sprites")
        source.set(layout.projectDirectory.file("assets/sprites/block_sprites.png"))
        tileWidth.set(24)
        tileHeight.set(24)
        columns.set(6)
    }
}
```

The generated `HextrisAssets` object implements Kengine's portable asset catalog, and `HextrisGame.assets` exposes that catalog to host backends.

Run tests:

```bash
jenv exec ./gradlew :games:hextris-core:allTests
```
