# Kotlin Native + SDL Demo

### GameLauncher.kt

```kotlin
fun main() {
    val sdlContext = SDLContext.create(title = "Kengine Demo", width = 800, height = 600)
    val gameScreen = GameScreen()

    GameLoop(frameRate = 60) { delta ->
        gameScreen.update(delta)
        gameScreen.draw(delta)
    }

    gameScreen.cleanup()
    sdlContext.cleanup()
}
```

<img src="https://raw.githubusercontent.com/kennycason/kengine-sdl/refs/heads/main/images/kengine_demo.png" />
