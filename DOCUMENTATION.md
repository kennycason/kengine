# Kengine Documentation

Kengine is a lightweight, Kotlin Native game framework built on top of SDL, designed for easy/simple game development.

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
            fillScreen(0u, 0u, 0u)
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


## Overview by Section

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

Sprites represent drawable objects on the screen. They can be created from textures or sprite sheets and drawn with transformations like scaling and rotation.

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

#### Time

The `ClockContext` handles game time, providing delta times for updates and total elapsed time since the game started.

Example: Using the `ClockContext`

```kotlin
useClockContext {
    logger.info { "Total Time: $totalTimeSec seconds" }
    logger.info { "Delta Time: $deltaTimeSec seconds" }
}
```

#### Entities

Entities represent objects in the game world, from players to obstacles. The Entity class provides a base for managing position, velocity, and actions.

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

#### Event Handling

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


#### useState

useState is a utility for managing state in your Kengine applications. 
It allows you to track and update values while notifying any subscribed listeners about changes. 
This state management mechanism is designed for lightweight use cases and integrates seamlessly with the Context system for broader application state management.

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
Inspired by React’s Context API, it provides a flexible and extensible way to share functionality or state across different parts of your application without tightly coupling them.

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
This function is inspired by React’s useMemo hook, but tailored for Kotlin Native and integrated with Kengine’s State.

```kotlin
val count = useState(0)
var computedValue = useMemo({ count.get() * 2 }, count)

expectThat(computedValue.get()).isEqualTo(0)

count.set(2) // trigger update
computedValue = useMemo({ count.get() * 2 }, count) // retrieve updated value

expectThat(computedValue.get()).isEqualTo(4)
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