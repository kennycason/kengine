package com.kengine

import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.useControllerContext
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.render.PortableSpriteRegistry
import com.kengine.render.RenderContext
import com.kengine.render.RenderContextSdlRenderer
import com.kengine.sdl.getSDLContext
import com.kengine.sdl.useSDLContext

private const val LEFT_STICK_X_AXIS = 0
private const val LEFT_STICK_Y_AXIS = 1
private const val LEFT_STICK_THRESHOLD = 0.45f

class PortableGameAdapter(
    private val portableGame: PortableGame,
    spriteRegistry: PortableSpriteRegistry = PortableSpriteRegistry(),
    commandCapacity: Int = 256,
    private val renderer: RenderContextSdlRenderer = RenderContextSdlRenderer(spriteRegistry)
) : Game {
    private val input = InputState()
    private val audio = AudioContext()
    private val render = RenderContext(commandCapacity)

    override fun update() {
        input.reset()
        useKeyboardContext {
            press(InputButton.LEFT, keyboard.isLeftPressed() || keyboard.isAPressed())
            press(InputButton.RIGHT, keyboard.isRightPressed() || keyboard.isDPressed())
            press(InputButton.UP, keyboard.isUpPressed() || keyboard.isWPressed())
            press(InputButton.DOWN, keyboard.isDownPressed() || keyboard.isSPressed())
            press(InputButton.A, keyboard.isSpacePressed() || keyboard.isJPressed())
            press(InputButton.B, keyboard.isBPressed() || keyboard.isKPressed())
            press(InputButton.START, keyboard.isReturnPressed() || keyboard.isEscapePressed())
            press(InputButton.X, keyboard.isXPressed() || keyboard.isUPressed())
            press(InputButton.Y, keyboard.isYPressed() || keyboard.isIPressed())
            press(InputButton.L, keyboard.isQPressed() || keyboard.isLShiftPressed())
            press(InputButton.R, keyboard.isEPressed() || keyboard.isRShiftPressed())
            press(InputButton.SELECT, keyboard.isTabPressed() || keyboard.isBackspacePressed())
        }
        useControllerContext {
            val leftStickX = controller.getAxisValue(LEFT_STICK_X_AXIS)
            val leftStickY = controller.getAxisValue(LEFT_STICK_Y_AXIS)

            press(InputButton.LEFT, controller.isButtonPressed(Buttons.DPAD_LEFT) || leftStickX < -LEFT_STICK_THRESHOLD)
            press(InputButton.RIGHT, controller.isButtonPressed(Buttons.DPAD_RIGHT) || leftStickX > LEFT_STICK_THRESHOLD)
            press(InputButton.UP, controller.isButtonPressed(Buttons.DPAD_UP) || leftStickY < -LEFT_STICK_THRESHOLD)
            press(InputButton.DOWN, controller.isButtonPressed(Buttons.DPAD_DOWN) || leftStickY > LEFT_STICK_THRESHOLD)
            press(InputButton.A, controller.isButtonPressed(Buttons.A))
            press(InputButton.B, controller.isButtonPressed(Buttons.B))
            press(InputButton.START, controller.isButtonPressed(Buttons.START))
            press(InputButton.X, controller.isButtonPressed(Buttons.X))
            press(InputButton.Y, controller.isButtonPressed(Buttons.Y))
            press(InputButton.L, controller.isButtonPressed(Buttons.L1))
            press(InputButton.R, controller.isButtonPressed(Buttons.R1))
            press(InputButton.SELECT, controller.isButtonPressed(Buttons.SELECT))
        }
        portableGame.update(input)
        audio.beginFrame()
        portableGame.audio(audio)
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

    private fun press(button: InputButton, pressed: Boolean) {
        if (pressed) {
            input.set(button)
        }
    }
}
