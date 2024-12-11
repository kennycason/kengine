# Image Shuffle

## Controls

- **Arrows:** Slide tiles
- **R:** Shuffle

<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/image-shuffle/image_shuffle.png" />

## Build and Run

From within IntelliJ, run the `main` function within `HellowWorldLauncher.kt`.

From the terminal:

```shell
./gradlew :games:image-shuffle:clean :games:image-shuffle:build
cd games/image-shuffle
./build/bin/native/releaseExecutable/image-shuffle.kexe
```

Note: Images are not bundled into the kexe yet and are included with relative paths.