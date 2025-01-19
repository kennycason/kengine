# Osc3x Synth


<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/images/osc3x-7.png" width="48%"/> <img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/images/osc3x-2.png" width="48%"/>

<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/images/osc3x-3.png" width="48%"/> <img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/images/osc3x-4.png" width="48%"/>

## Build and Run

From within IntelliJ, run the `main` function within `Osc3xGUILauncher.kt`.

From the terminal:

```shell
./gradlew :games:osc3x-synth:clean :games:osc3x-synth:build
cd games/osc3x-synth
./build/bin/native/releaseExecutable/osc3x-synth.kexe
```

Note: Images are not bundled into the kexe yet and are included with relative paths.
