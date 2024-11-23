# Kengine SDL

A light game library in Kotlin Native + SDL

### A Sample Game

Press Up, Down, Left, Right keys to move Bulbasaur while the Pokeballs bounce around in the background.
Left or Right clicking will move Bulbasaur to the cursor.

<img src="https://raw.githubusercontent.com/kennycason/kengine-sdl/refs/heads/main/images/kengine_demo.png" />

```kotlin
fun main() {
    AppContext.create(title = "Kengine Demo", width = 800, height = 600)
    useContext(AppContext.get(), cleanup = true) {
        val gameScreen = GameScreen()

        GameLoop(frameRate = 60) { elapsedSeconds ->
            gameScreen.update(elapsedSeconds)
            gameScreen.draw(elapsedSeconds)
        }

        gameScreen.cleanup()
    }
}
```

```kotlin
class DemoGameScreen {
    private val bulbasaur = BulbasaurEntity()

    fun update(elapsedSeconds: Double) {
        bulbasaur.update(elapsedSeconds)
    }

    fun draw(elapsedSeconds: Double) {
        val sdlContext = SDLContext.get()

        // clear screen (black)
        SDL_SetRenderDrawColor(sdlContext.renderer, 0u, 0u, 0u, 255u)
        SDL_RenderClear(sdlContext.renderer)
        
        bulbasaur.draw(elapsedSeconds)

        // render to screen
        SDL_RenderPresent(sdlContext.renderer)
    }

    fun cleanup() {
        bulbasaur.cleanup()
    }

}
```

```kotlin
class BulbasaurEntity : SpriteEntity(
    sprite = Sprite("images/bulbasaur.bmp")
) {
    private val speed = 100.0
    private var state = State.INIT

    private enum class State {
        INIT,
        READY
    }

    override fun update(elapsedSeconds: Double) {
        super.update(elapsedSeconds)
        when (state) {
            State.INIT -> init()
            State.READY -> ready(elapsedSeconds)
        }
    }

    private fun ready(elapsedSeconds: Double) {
        v.x *= 0.9
        v.y *= 0.9
        useContext(KeyboardContext.get()) {
            if (keyboard.isLeftPressed()) {
                v.x = -speed * elapsedSeconds
            }
            if (keyboard.isRightPressed()) {
                v.x = speed * elapsedSeconds
            }
            if (keyboard.isUpPressed()) {
                v.y = -speed * elapsedSeconds
            }
            if (keyboard.isDownPressed()) {
                v.y = speed * elapsedSeconds
            }
        }
        useContext(MouseContext.get()) {
            if (mouse.isLeftPressed() || mouse.isRightPressed()) {
                p.x = mouseInput.getCursor().x - width / 2
                p.y = mouseInput.getCursor().y - height / 2
                Logger.info { "Move Bulbasaur to mouse cursor $p" }
            }
        }
        p.x += v.x
        p.y += v.y
    }

    private fun init() {
        useContext(SDLContext.get()) {
            p.x = screenWidth / 2.0 - width / 2.0
            p.y = screenHeight / 2.0 - height / 2.0
        }
        state = State.READY
    }
}
```

## Roadmap

- SDL_Quit Handler
- Rect type (x,y,width,height)
- Animated Sprites
- Sprite Sheets
- Sound / Sound Manager
- TiledMapLoader
- Shape Drawing
- TTF support
- Logger -> File
- Menu system
- GUI
- Box2D
- Networking



