# Kengine

A light game library in Kotlin Native + SDL

## Example Games

### [Hello, World](helloworld/)

A sample game app to help get started.

### [Boxxle](boxxle/) 

A more robust example

Controls:
- **WASD or Arrows:** Movement
- **R:** Reset level
- **Return:** Next level
- **Space:** Previous level

There are 40 levels total. 

<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/boxxle/screenshot.png" />

```kotlin
fun main() {
    useContext(
        GameContext.create(
            title = "Boxxle",
            width = 800,
            height = 600
        )
    ) {
        GameRunner(frameRate = 60) {
            BoxxleGame()
        }
    }
}
```

View `GameLauncher.kt` and `BoxxleGame` or `DemoGame` for more examples.

## Project structure

```shell
kengine/
├── kengine/                       // kengine core code
├── boxxle/                        // boxxle - a more robust example game
└── helloworld/                    // a simple example, a good starting point.
```

## Project structure for a Kengine game

```shell
<game_name>/
├── build.gradle.kts               
├── gradle.properties    
├── assets/                        // game assets (shared across platforms)
│   ├── sprites/                   // images and sprite sheets
│   └── sounds/                    // sound files     
└── src/
    ├── nativeMain/
    │   └── kotlin/                // game-specific code
    └── test/                      // Unit and integration tests
```

## Installation 

Install OpenJDK 17.0

Install SDL via Brew (on Mac)
```shell
brew install sdl2 sdl2_mixer
```

Build the project
```shell
./gradlew clean build
```

## Roadmap

- Embed data files in executable binary
- Rect type (x,y,width,height)
- Animated Sprites
- GameLoop updates (improve handling of delta timestamps, decouple update/draw calls)
- Sound / Sound Manager
- TiledMapLoader
- Shape Drawing
- TTF support
- Logger -> File
- Menu system
- GUI
- Box2D
- Networking
- fix -Wno-c99-designator



