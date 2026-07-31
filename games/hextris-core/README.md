# Hextris Core

Shared portable Hextris game logic, rendering commands, audio commands, storage keys, and asset declarations. Platform modules such as `:games:hextris-desktop` and `:games:hextris-switch` instantiate `hextris.HextrisGame` instead of duplicating gameplay code or asset names.

This is the intended Kengine pattern: keep game behavior and portable assets in core, then make each platform module a small host wrapper.

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

    music("theme") {
        id.set("hextris/techno-boss-worm")
        source.set(layout.projectDirectory.file("sound/techno_boss_worm.ogg"))
    }

    sound("rotate") {
        id.set("hextris/rotate")
        source.set(layout.projectDirectory.file("sound/sfx/rotate.wav"))
    }
}
```

The generated `HextrisAssets` object implements Kengine's portable asset catalog, and `HextrisGame.assets` exposes that catalog to host backends. Hextris currently declares one sprite sheet, one music track, and one-shot SFX WAVs.

Run tests:

```bash
./gradlew :games:hextris-core:allTests
```
