# Kengine Sound

Kengine sound APIs cover SDL3_mixer playback on native targets plus common, platform-neutral synthesis helpers.

## Procedural SFX

`com.kengine.sound.procedural` can render short 16-bit PCM or WAV clips from sweep/noise specs:

```kotlin
val wav = ProceduralSfx.renderWavPcm16Le(BlockPuzzleProceduralSfx.rotate())
```

The procedural path is meant for creating declared audio assets. Runtime backends such as Nintendo Switch still expect games to declare sound assets explicitly through `kenginePortableAssets.sound(...)`.
