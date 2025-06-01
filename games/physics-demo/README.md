# Physics Demo using Kengine

A simple demonstration of falling circles and boxes.


<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/physics-demo/falling_shapes.png" />

## Build and Run

From within IntelliJ, run the `main` function within `PhysicsDemoGameLauncher.kt`.

From the terminal:

```shell
./gradlew :games:physics-demo:clean :games:physics-demo:build
./games/physics-demo/build/bin/native/releaseExecutable/physics-demo.kexe
```

Note: Images are not bundled into the kexe yet and are included with relative paths.
