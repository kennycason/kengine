# Osc3x Synth


<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/images/osc3x-2.png" width="50%"/> <img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/images/osc3x-7.png" width="50%"/>

## Build and Run

From within IntelliJ, run the `main` function within `HellowWorldLauncher.kt`.

From the terminal:

```shell
./gradlew :games:image-shuffle:clean :games:image-shuffle:build
cd games/image-shuffle
./build/bin/native/releaseExecutable/image-shuffle.kexe
```

Note: Images are not bundled into the kexe yet and are included with relative paths.
