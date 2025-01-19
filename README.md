# Kengine

Kotlin Native Game Engine (`KNGN`?)

Kengine is a lightweight, game framework built in Kotlin Native + SDL3, designed for easy/simple game development.

The project is still in early dev mode, contributors welcome. 

This has also been an experiment with ChatGPT + Claude to help with coding + design mixed with some other hobby projects.

# Table of Contents

- [Kengine](#kengine)
  - [Introduction](#introduction)
  - [Example Games](#example-games)
    - [Hello, World](#hello-world)
    - [Boxxle](#boxxle)
    - [Osc3x Synth](#osc3x-synth)
  - [Documentation](#documentation)
    - [Graphics](#graphics)
      - [Textures](#textures)
      - [Sprites](#sprites)
      - [Animated Sprites](#animated-sprites)
      - [Geometry](#geometry)
    - [Time](#time)
    - [Input](#input)
      - [Mouse Input](#mouse-input)
      - [Keyboard Input](#keyboard-input)
      - [Controller Input](#controller-input)
    - [Entities](#entities)
    - [Event Handling](#event-handling)
- [Tiled Map](#tiled-map)
- [Functional Hooks](#functional-hooks)
  - [useState](#usestate)
  - [useContext](#usecontext)
  - [useEffect](#useeffect)
  - [useMemo](#usememo)
  - [useReducer](#usereducer)
  - [Logging](#logging)
  - [Actions](#actions)
  - [Math Utilities](#math-utilities)
- [Unit Testing](kengine-test/)
- [Dev](#dev)
  - [Project structure](#project-structure)
  - [Project structure for a Kengine game](#project-structure-for-a-kengine-game)
  - [Installation](#installation)
- [Roadmap](#roadmap)


## Introduction

A Simple Example

```kotlin
fun main() {
    useContext(
        GameContext.create(
            title = "Bouncing Ball Game",
            width = 800,
            height = 600
        )
    ) {
        GameRunner(frameRate = 60) {
            BouncingBallGame()
        }
    }
}

class BouncingBallGame : Game {
    private val ball = BallSpriteEntity()

    override fun update() {
        ball.update()
    }

    override fun draw() {
        useSDLContext {
            fillScreen(0u, 0u, 0u) // or fillScreen(Color.black)
            ball.draw()
            flipScreen()
        }
    }

    override fun cleanup() {
        ball.cleanup()
    }
}

class BallSpriteEntity : SpriteEntity(
    sprite = Sprite.fromFilePath("assets/sprites/ball.bmp"),
    p = Vec2(400.0, 300.0),  // initial position
    v = Vec2(30.0, 30.0) // initial velocity (pixels/sec)
), Logging {
    private var bounceCounter = 0

    override fun update() {
        val clock = getContext<ClockContext>()

        // update position
        p += v * clock.deltaTimeSec

        // handle screen boundaries
        useSDLContext {
            if (p.x < 0 || p.x + width > screenWidth) {
                direction.x *= -1
                bounceCounter++
            }
            if (p.y < 0 || p.y + height > screenHeight) {
                direction.y *= -1
                bounceCounter++
            }
        }

        useKeyboardContext {
            if (keyboard.isRPressed()) {
                logger.info { "Reset ball" }
                p.set(400.0, 300.0)
                p.set(30.0, 30.0)
            }
        }

        logger.info { "Wall bounces: $bounceCounter" }
    }
}
```


## Example Games

### [Hello, World](helloworld/)

A sample game app to help get started.

### [Boxxle](boxxle/)

A more robust example with `keyboard` and `controller` support.

Controls:
- **WASD or Arrows:** Movement
- **R:** Reset level
- **Return:** Next level
- **Space:** Previous level

There are 41 levels total.

<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/games/boxxle/screenshot.png" width="50%" />

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

### [Osc3x Synth](games/osc3x-synth/)

Explore sound synthesis with a3x Oscillator and a variety of visual effects. This also showcases the `UI` library and state handling via `useState`.

<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/games/osc3x-synth/screenshot.png" width="48%"/><img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/games/osc3x-synth-v2/screenshot.png" width="48%"/>

[Video of Synth on IG](https://www.instagram.com/p/DEnebatzN3V/?igsh=MTZ0ZTJ1ZDE4ejVuag==)


## Documentation

### Graphics

#### Textures

`Textures` are central to rendering 2D graphics in Kengine. They are managed using the `TextureManager`, which caches textures for efficient reuse.

Example: Loading and Using a Texture

```kotlin
useTextureContext {
    addTexture("ball", "assets/sprites/ball.bmp")
    val ballTexture = getTexture("ball")
}
```

#### Sprites

Sprites represent drawable objects on the screen. They can be created from textures or sprite sheets and drawn with transformations like scaling and
rotation.

Example: Drawing a Sprite

```kotlin
val sprite = Sprite.fromFilePath("assets/sprites/ball.bmp")
sprite.draw(x = 100.0, y = 200.0)
```

#### Animated Sprites

Animated sprites cycle through a sequence of images (frames) to create animations. The AnimatedSprite class makes this process straightforward.

Example:

```kotlin
val animatedSprite = AnimatedSprite(
    sprites = listOf(
        Sprite.fromFilePath("samus01.bmp"),
        Sprite.fromFilePath("samus02.bmp"),
        Sprite.fromFilePath("samus03.bmp")
    ),
    frameRate = 10
)

animatedSprite.draw(Vec2(100.0, 200.0))
```

Another example loading from a `SpriteSheet`.

```kotlin
val spriteSheet = SpriteSheet.fromFilePath("assets/sprites/metroid.bmp", tileWidth = 32, tileHeight = 32)
private val animatedMetroid = AnimatedSprite.fromSpriteSheet(spriteSheet, frameDurationMs = 200L)
```

#### Geometry

Kengine provides simple geometry drawing utilities such as circles, rectangles, and lines, through the `GeometryContext`.

Example:

```kotlin
useGeometryContext {
    drawRectangle(50, 50, 100, 200, 0xFFu, 0x00u, 0x00u, 0xFFu)
    drawCircle(200, 200, 50, 0x00u, 0xFFu, 0x00u, 0xFFu)
    drawLine(0, 0, 300, 300, 0x00u, 0x00u, 0xFFu, 0xFFu)
}
```

### Time

The `ClockContext` handles game time, providing delta times for updates and total elapsed time since the game started.

Example: Using the `ClockContext`

```kotlin
useClockContext {
    logger.info { "Total Time: $totalTimeSec seconds" }
    logger.info { "Delta Time: $deltaTimeSec seconds" }
}
```

### Input

#### Mouse Input

The `MouseContext` provides utilities to handle mouse input events, including button presses, cursor position, and timing.

Example: Handling Mouse Input

```kotlin
useMouseContext {
    if (mouse.isLeftPressed() || mouse.isRightPressed()) {
        p.x = mouse.getCursor().x - width / 2
        p.y = mouse.getCursor().y - height / 2
    }
}
```

Mouse Functions

| Function                         | Description                                                      |
|----------------------------------|------------------------------------------------------------------|
| `mouse.isLeftPressed()`          | Returns `true` if the **left mouse button** is pressed.          |
| `mouse.isRightPressed()`         | Returns `true` if the **right mouse button** is pressed.         |
| `mouse.isMiddlePressed()`        | Returns `true` if the **middle mouse button** is pressed.        |
| `mouse.getCursor()`              | Returns the current cursor position as `Vec2(x, y)`.             |
| `mouse.timeSinceLeftPressed()`   | Returns time (ms) since the **left mouse button** was pressed.   |
| `mouse.timeSinceRightPressed()`  | Returns time (ms) since the **right mouse button** was pressed.  |
| `mouse.timeSinceMiddlePressed()` | Returns time (ms) since the **middle mouse button** was pressed. |

#### Keyboard Input

The KeyboardContext provides utilities for handling keyboard input, including key presses and timings.

Example: Handling Keyboard Input

```kotlin
useKeyboardContext {
    if (keyboard.isWPressed()) {
        logger.info { "Moving up!" }
    }

    if (keyboard.isReturnPressed()) {
        logger.info { "Return key pressed!" }
    }
}
```

Keyboard Functions

| Function                            | Description                                                   |
|-------------------------------------|---------------------------------------------------------------|
| `keyboard.isAPressed()`             | Returns `true` if the **A key** is pressed.                   |
| `keyboard.isSpacePressed()`         | Returns `true` if the **Space key** is pressed.               |
| `keyboard.isReturnPressed()`        | Returns `true` if the **Return/Enter key** is pressed.        |
| `keyboard.isEscapePressed()`        | Returns `true` if the **Escape key** is pressed.              |
| `keyboard.isLeftPressed()`          | Returns `true` if the **Left Arrow key** is pressed.          |
| `keyboard.isRightPressed()`         | Returns `true` if the **Right Arrow key** is pressed.         |
| `keyboard.timeSinceAPressed()`      | Returns time (ms) since the **A key** was pressed.            |
| `keyboard.timeSinceSpacePressed()`  | Returns time (ms) since the **Space key** was pressed.        |
| `keyboard.timeSinceReturnPressed()` | Returns time (ms) since the **Return/Enter key** was pressed. |

#### Controller Input

The ControllerContext handles input from game controllers, supporting PlayStation, Xbox, Nintendo Switch, and generic gamepads.

Example: Handling Controller Input

```kotlin
useControllerContext {
    if (controller.isButtonPressed(Buttons.A)) {
        logger.info { "Jump button pressed!" }
    }

    val axisValue = controller.getAxisValue(0) // Read the left stick horizontal axis
    logger.info { "Axis value: $axisValue" }
}
```

Supported Controllers

- PlayStation 4 (DualShock 4)
- PlayStation 5 (DualSense)
- Xbox One
- Xbox Series X/S
- Nintendo Switch Pro Controller
- Logitech
- Ouya
- Steam Controller (needs more testing)
- Generic Gamepads (fallback mapping)

Controller Functions

| Function                                               | Description                                                |
|--------------------------------------------------------|------------------------------------------------------------|
| `controller.isButtonPressed(Buttons.A)`                | Returns `true` if the **A button** is pressed.             |
| `controller.isButtonPressed(Buttons.B)`                | Returns `true` if the **B button** is pressed.             |
| `controller.isButtonPressed(Buttons.START)`            | Returns `true` if the **Start/Options button** is pressed. |
| `controller.isButtonPressed(Buttons.DPAD_UP)`          | Returns `true` if the **D-Pad Up** is pressed.             |
| `controller.getAxisValue(0)`                           | Gets the value of the **Left Stick X-Axis** (-1.0 to 1.0). |
| `controller.getAxisValue(1)`                           | Gets the value of the **Left Stick Y-Axis** (-1.0 to 1.0). |
| `controller.isHatDirectionPressed(0, HatDirection.UP)` | Returns `true` if the **D-Pad Up** direction is pressed.   |

Controller Buttons Overview

Controller Button Mapping Table:

| Button                     | Code Example                                         | PlayStation 5                        | Xbox Series X                      |
|----------------------------|------------------------------------------------------|--------------------------------------|------------------------------------|
| **Buttons.A**              | `controller.isButtonPressed(Buttons.A)`              | X                                    | A                                  |
| **Buttons.B**              | `controller.isButtonPressed(Buttons.B)`              | Circle (O)                           | B                                  |
| **Buttons.X**              | `controller.isButtonPressed(Buttons.X)`              | Square (□)                           | X                                  |
| **Buttons.Y**              | `controller.isButtonPressed(Buttons.Y)`              | Triangle (△)                         | Y                                  |
| **Buttons.L1**             | `controller.isButtonPressed(Buttons.L1)`             | L1                                   | LB (Left Bumper)                   |
| **Buttons.R1**             | `controller.isButtonPressed(Buttons.R1)`             | R1                                   | RB (Right Bumper)                  |
| **Buttons.L2**             | `controller.getAxisValue(4)`                         | L2 Trigger Axis                      | LT (Left Trigger)                  |
| **Buttons.R2**             | `controller.getAxisValue(5)`                         | R2 Trigger Axis                      | RT (Right Trigger)                 |
| **Buttons.L3**             | `controller.isButtonPressed(Buttons.L3)`             | L3 (Left Stick Button)               | LS (Left Stick Button)             |
| **Buttons.R3**             | `controller.isButtonPressed(Buttons.R3)`             | R3 (Right Stick Button)              | RS (Right Stick Button)            |
| **Buttons.START**          | `controller.isButtonPressed(Buttons.START)`          | Options                              | Menu (Start)                       |
| **Buttons.SELECT**         | `controller.isButtonPressed(Buttons.SELECT)`         | Create (Share)                       | View (Back)                        |
| **Buttons.DPAD_UP**        | `controller.isButtonPressed(Buttons.DPAD_UP)`        | D-Pad Up                             | D-Pad Up                           |
| **Buttons.DPAD_DOWN**      | `controller.isButtonPressed(Buttons.DPAD_DOWN)`      | D-Pad Down                           | D-Pad Down                         |

Controller Axes Overview

| Axis Name                 | Code Example                              | PlayStation 5                        | Xbox Series X                      |
|---------------------------|-------------------------------------------|--------------------------------------|------------------------------------|
| **Left Stick X**          | `controller.getAxisValue(0)`              | Left Stick Horizontal Axis           | Left Stick Horizontal Axis         |
| **Left Stick Y**          | `controller.getAxisValue(1)`              | Left Stick Vertical Axis             | Left Stick Vertical Axis           |
| **Right Stick X**         | `controller.getAxisValue(2)`              | Right Stick Horizontal Axis          | Right Stick Horizontal Axis        |
| **Right Stick Y**         | `controller.getAxisValue(3)`              | Right Stick Vertical Axis            | Right Stick Vertical Axis          |
| **Left Trigger Axis**     | `controller.getAxisValue(4)`              | L2 Trigger Axis                      | LT Trigger Axis                    |
| **Right Trigger Axis**    | `controller.getAxisValue(5)`              | R2 Trigger Axis                      | RT Trigger Axis                    |


### Entities

Entities represent objects in the game world, from players to obstacles. The Entity class provides a base for managing position, velocity, and
actions.

Example: Creating a Custom Entity

```kotlin
class MyEntity : Entity(width = 32, height = 32) {

    override fun update() {
        p.x += 1.0
    }

    override fun draw() {
        // custom drawing logic
    }

    override fun cleanup() {
        // cleanup resources
    }
}
```

### Event Handling

The `EventContext` enables decoupled communication between components using events.

Example: Publishing and Subscribing to Events

```kotlin
useEventContext {
    subscribe("player_died") { data: String ->
        logger.info { "Player died because: $data" }
    }
    publish("player_died", "Fell off a cliff")
}
```


# Tiled Map

## Overview

The **TiledMapLoader** is a utility for loading and rendering maps in the **Tiled** map format. Currently, it only supports the `.tmj` (Tiled Map JSON) and `.tsj` (Tiled Tileset JSON) file formats. Maps and tilesets in the `.tmx` and `.tsx` formats are **not supported**.

## Key Features

- Loads Tiled maps (`.tmj`) and external tilesets (`.tsj`).
- Supports multiple tiled and object layers.
- Supports animated tiles and tile flipping/rotations.
- Scrollable maps with customizable controls for navigation.
- Render time for a 4-layer map with animations & rotations is ~5-7ms/render. Goal is <1ms.
---

## Example Usage

### Loading and Drawing a Map

Here is an example of loading and rendering a map:

<img src="https://raw.githubusercontent.com/kennycason/kengine/refs/heads/main/images/tiled_map.gif" width="65%" />

```kotlin
val tiledMap = TiledMapLoader()
    .loadMap("src/nativeTest/resources/ninjaturdle/lungs_25.tmj")

object : Game {
    override fun update() {
        tiledMap.update() // update animated tiles
    }

    override fun draw() {
        tiledMap.draw() // render all layers of the map
      
        // or draw layers by name
        tileMap.draw("bg")
        tileMap.draw("main")
        // draw player/enemies
        tileMap.draw("fg")
    }

    override fun cleanup() {
    }
}
```

### Functional Hooks

Inspired from React Hooks

#### useState

useState is a utility for managing state in your Kengine applications.
It allows you to track and update values while notifying any subscribed listeners about changes.
This state management mechanism is designed for lightweight use cases and integrates seamlessly with the Context system for broader application state
management.

Creating and Using a State Variable

```kotlin
val count = useState(0)

val callback = { newValue: Int ->
    println("Count changed to $newValue")
}

count.subscribe(callback)
count.set(1) // Output: Count changed to 1

count.unsubscribe(callback)
count.set(2) // No output
```

#### useContext

The Context class in your framework serves as a foundational building block for managing scoped, singleton-like components in your application.
Inspired by React’s Context API, it provides a flexible and extensible way to share functionality or state across different parts of your application
without tightly coupling them.

```kotlin
class SimpleContext : Context() {
    var count: Int = 1
}

val simpleContext = SimpleContext()
ContextRegistry.register(simpleContext)

useContext<SimpleContext> {
    expectThat(count).isEqualTo(1)
    count = 5
    expectThat(count).isEqualTo(5)
}
```

Context can be integrated with State to efficiently share and manage state across multiple classes.

```kotlin
class StatefulContext : Context() {
    val count = useState(0)
}

val statefulContext = StatefulContext()
ContextRegistry.register(statefulContext)

useContext<StatefulContext> {
    expectThat(count.get()).isEqualTo(0)
    count.set(42)
    expectThat(count.get()).isEqualTo(42)

    var countUpdated = false
    var countReceived = 0
    count.subscribe {
        countUpdated = true
        countReceived = it
    }
    count.set(64)
    expectThat(countUpdated).isTrue()
    expectThat(count.get()).isEqualTo(64)
    expectThat(countReceived).isEqualTo(64)
}
```

#### useEffect

useEffect is a utility that allows you to manage side effects in response to changes in state variables.
It subscribes to the provided state dependencies and automatically triggers the effect whenever any of the dependencies change.
The effect can also include a cleanup mechanism, which is executed when dependencies change or when the effect is removed.

Simple Side Effect

In this example, useEffect is used to log a message whenever the count state changes:

```kotlin
val count = useState(0)

useEffect({
    println("The count has changed: ${count.get()}")
}, count)

count.set(1)  // Logs: "The count has changed: 1"
count.set(2)  // Logs: "The count has changed: 2"
```

#### useMemo

useMemo is a utility function for caching expensive computations based on dependencies.
It ensures that a computed value is only recalculated when one of its dependencies changes.

```kotlin
val count = useState(0)
var computedValue = useMemo({ count.get() * 2 }, count)

expectThat(computedValue.get()).isEqualTo(0)

count.set(2) // trigger update
computedValue = useMemo({ count.get() * 2 }, count) // retrieve updated value

expectThat(computedValue.get()).isEqualTo(4)
```

#### useReducer

The useReducer hook is another great addition to the state management toolbox, especially for handling complex state logic.
It provides a predictable way to update state by defining actions and a reducer function.

This example demonstrates using useReducer with simple String actions:

```kotlin
val (count, dispatch) = useReducer(0) { state: Int, action: String ->
    when (action) {
        "increment" -> state + 1
        "decrement" -> state - 1
        else -> state
    }
}

expectThat(count.get()).isEqualTo(0)

dispatch("increment")
expectThat(count.get()).isEqualTo(1)

dispatch("decrement")
expectThat(count.get()).isEqualTo(0)
```

Here’s a more robust example, showcasing useReducer with object-based actions:

```kotlin
data class User(val name: String, val age: Int)
abstract class UserAction
data class UpdateName(val name: String) : UserAction()
data class IncrementAge(val by: Int) : UserAction()

val initialUser = User("John", 25)
val (user, dispatch) = useReducer(initialUser) { state: User, action: UserAction ->
    when (action) {
        is UpdateName -> state.copy(name = action.name)
        is IncrementAge -> state.copy(age = state.age + action.by)
        else -> throw IllegalStateException()
    }
}

expectThat(user.get().name).isEqualTo("John")
expectThat(user.get().age).isEqualTo(25)

dispatch(UpdateName("Jane"))
expectThat(user.get().name).isEqualTo("Jane")

dispatch(IncrementAge(5))
expectThat(user.get().age).isEqualTo(30)
```

#### Logging

The Logger provides utility functions for debugging and monitoring game state.

Example:

```kotlin
logger.info { "Game started!" }
logger.error { "An error occurred." }
logger.error(e) { "An error occurred." }
```

#### Actions

Actions provide a way to script entity behavior over time, such as movements or animations.

Example: Moving an Entity

```kotlin
useActionContext {
    moveTo(entity, Vec2(200.0, 300.0), speed = 100.0) {
        logger.info { "Entity reached its destination!" }
    }
}
```

#### Math Utilities

Kengine includes math utilities such as Vec2 and Rect for vector and rectangle operations.

Example: Using Vectors

```kotlin
val position = Vec2(10.0, 20.0)
val direction = Vec2(1.0, 0.0)
val newPosition = position + direction * 5.0
logger.info { "New Position: $newPosition" }
```





## Dev

### Project structure

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
