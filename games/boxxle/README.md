# Boxxle - Clone of the Gameboy classic

## Controls

- **WASD or Arrows:** Movement
- **R:** Reset level
- **Return:** Next level
- **Space:** Previous level

There are 41 levels total

<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/boxxle/screenshot.png" />

## Build and Run

From within IntelliJ, run the `main` function within `BoxxleLauncher.kt`.

From the terminal:

```shell
./gradlew clean build
./boxxle/build/bin/native/releaseExecutable/boxxle.kexe
```


Build only the `boxxle` module, without rebuilding `kengine` module:

```shell
./gradlew :boxxle:clean :boxxle:build
```