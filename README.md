# Kengine

A Reactive, Kotlin Native Game Engine

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

There are 41 levels total. 

<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/games/boxxle/screenshot.png" />

```kotlin
fun main() {
    createGameContext(
        title = "Boxxle",
        width = 800,
        height = 600
    ) {
        GameRunner(frameRate = 60) {
            BoxxleGame()
        }
    }
}
```

View `GameLauncher.kt` and `BoxxleGame` or `DemoGame` for more examples.

## Drawing Sprites & Shapes

```kotlin
private val sprite = Sprite("sprites/bulbasaur.bmp")
useSDLContext {
    // clear screen
    fillScreen(0u, 0u, 0u)

    useGeometryContext {
        // basic shapes, color format rgba
        drawRectangle(10, 10, 10, 10, 0xFFu, 0xFFu, 0xFFu, 0xFFu)
        fillRectangle(20, 20, 10, 10, 0xFFu, 0x00u, 0x00u, 0xFFu)
        drawCircle(30, 30, 10, 0x00u, 0xFFu, 0x00u, 0xFFu)
        fillCircle(40, 40, 10, 0x00u, 0x00u, 0xFFu, 0xFFu)

        // draw overlapping, transparent red, blue, and green circles.
        fillCircle(screenWidth / 2 - 32, screenWidth / 2 - 16, 64, 0xFFu, 0x00u, 0x00u, 0x77u)
        fillCircle(screenWidth / 2 + 32, screenWidth / 2 - 16, 64, 0x00u, 0xFFu, 0x00u, 0x77u)
        fillCircle(screenWidth / 2, screenWidth / 2 + 38, 64, 0x00u, 0x00u, 0xFFu, 0x77u)
        
        // draw a single pixel
        drawPixel(50, 50, 0xFFu, 0xFFu, 0xFFu, 0xFFu)
        
        // overlapping lines
        drawLine(150, 50, 300, 50, 0xFFu, 0u, 0u, 0xFFu)
        drawLine(160, 150, 310, 20, 0x57u, 0x23u, 0x5Eu, 0xFFu)
    }

    sprite.draw()

    flipScreen()
}
```

## Reading Keyboard & Mouse Events

```kotlin
useKeyboardContext {
    if (keyboard.isLeftPressed()) {
        logger.info { "pressed left" }
    }
    if (keyboard.isSpacePressed()) {
        logger.info { "pressed space" }
    }
}
useMouseContext {
    if (mouse.isLeftPressed() || mouse.isRightPressed()) {
        logger.info { "Clicked mouse @ (${mouse.getCursor().x}, ${mouse.getCursor().y})" }
    }
}
useControllerContext {
    if (controller.isButtonPressed(Buttons.DPAD_LEFT)) {
        logger.info { "pressed left" }
    }
    if (controller.isButtonPressed(Buttons.DPAD_RIGHT)) {
        logger.info { "pressed space" }
    }
}
```

## Project structure

```shell
kengine/
├── kengine/                       // kengine core code
├── kengine-test/                  // kengine test framework
└── games/
    ├── boxxle/                    // boxxle - clone of the Gameboy classic
    ├── helloworld/                // a simple example, a good starting point.
    ├── image-shuffle/             // image tile shuffle game
    └── physics-demo/              // demonstration of physics engine (chipmunk)
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
    └── nativeTest/                // unit and integration tests
        └── kotlin/                // game-specific test code
```

## Installation 

Install OpenJDK 17.0+

Install Chipmunk2D via Brew (on Mac)
```shell
brew install chipmunk
```


Install 3DL3. SDL3 is not yet released on brew and must be manually installed.

[SDL3 Installation Guide](/sdl3/README.md)

Build the project
```shell
./gradlew clean build
```

Misc Gradle cache/refresh dependencies
```shell
rm -rf ~/.gradle/caches
rm -rf ~/.gradle/wrapper
./gradlew wrapper --refresh-dependencies
./gradlew clean build --refresh-dependencies
```

## Roadmap
- Binary
  - Embed data files in executable
- GameLoop
  - Decouple update/draw calls
- TiledMapLoader 
  - Performance enhancements
  - Support TMX (XML format)
- Logger file support
- Menu system
- GUI
- fix @OptIn(ExperimentalForeignApi::class) (-opt-in being ignored by compiler in multi-module project in IJ)
- Add Vec2 versions of functions that take (x,y) parameters, ditto for Rect2 and (x,y,w,h)
- Redesign font handling + caching/config
- Playdate integration (WIP struggling to target cortex-m7 arch)
