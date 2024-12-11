# A Simple Game using Kengine

A simple demonstration of various features.

## Controls

- **WASD or Arrows:** Movement
- **Space:** Bulbasaur roars and the Pidgeys will fly faster.

<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/helloworld/helloworld.png" />

## Build and Run

From within IntelliJ, run the `main` function within `HellowWorldLauncher.kt`.

From the terminal:

```shell
./gradlew :games:helloworld:clean :games:helloworld:build
cd games/helloworld
./build/bin/native/releaseExecutable/helloworld.kexe
```

Note: Images are not bundled into the kexe yet and are included with relative paths.