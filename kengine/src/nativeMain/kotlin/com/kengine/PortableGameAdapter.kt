package com.kengine

import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.render.RenderContext
import com.kengine.render.RenderContextSdlRenderer
import com.kengine.sdl.getSDLContext
import com.kengine.sdl.useSDLContext

class PortableGameAdapter(
    private val portableGame: PortableGame,
    commandCapacity: Int = 256,
    private val renderer: RenderContextSdlRenderer = RenderContextSdlRenderer()
) : Game {
    private val input = InputState()
    private val render = RenderContext(commandCapacity)

    override fun update() {
        input.reset()
        useKeyboardContext {
            input.set(InputButton.LEFT, keyboard.isLeftPressed())
            input.set(InputButton.RIGHT, keyboard.isRightPressed())
            input.set(InputButton.UP, keyboard.isUpPressed())
            input.set(InputButton.DOWN, keyboard.isDownPressed())
            input.set(InputButton.A, keyboard.isAPressed())
            input.set(InputButton.B, keyboard.isBPressed())
            input.set(InputButton.START, keyboard.isReturnPressed() || keyboard.isEscapePressed())
        }
        portableGame.update(input)
    }

    override fun draw() {
        val sdl = getSDLContext()
        render.beginFrame(sdl.screenWidth, sdl.screenHeight)
        portableGame.draw(render)
        renderer.render(render)
        useSDLContext {
            flipScreen()
        }
    }

    override fun cleanup() {
        portableGame.cleanup()
    }
}
