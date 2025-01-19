# Osc3x Synth


<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/games/osc3x-synth-v2/screenshot.png" width="66%"/>

## Build and Run

From within IntelliJ, run the `main` function within `Osc3xSynthsLauncher.kt`.

From the terminal:

```shell
./gradlew :games:osc3x-synth:clean :games:osc3x-synth:build
cd games/osc3x-synth
./build/bin/native/releaseExecutable/osc3x-synth.kexe
```

Note: Images are not bundled into the kexe yet and are included with relative paths.
