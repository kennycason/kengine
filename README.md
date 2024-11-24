# Kengine SDL

A light game library in Kotlin Native + SDL

WIP

### Boxxle - Clone of the Gameboy classic

Controls:
- **WASD or Arrows:** Movement
- **R:** Reset level
- **Return:** Next level
- **Space:** Previous level

There are 40 levels total. 

<img src="https://raw.githubusercontent.com/kennycason/kengine-sdl/refs/heads/main/images/boxxle01.png" />

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


## Installation 

Install SDL via Brew (on Mac)
```shell
brew install sdl2 sdl2_mixer
```

Build the project
```shell
./gradlew clean build
```

## Roadmap

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



