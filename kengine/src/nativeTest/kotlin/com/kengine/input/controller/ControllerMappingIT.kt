package com.kengine.input.controller

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.getCurrentMilliseconds
import kotlin.test.Ignore
import kotlin.test.Test

class ControllerMappingIT {

    data class ButtonTest(
        val button: Buttons,
        val description: String,
        var detected: Boolean = false
    )

    @Test
    @Ignore
    fun `interactive controller button mapping`() {
        println("=".repeat(60))
        println("INTERACTIVE CONTROLLER BUTTON MAPPING TEST")
        println("=".repeat(60))
        println()
        println("Mode: will use current KENGINE_CONTROLLER_MODE (default: JOYSTICK)")
        println()
        println("Instructions:")
        println("  - Press each requested button when prompted")
        println("  - Press SPACE to skip if button doesn't exist")
        println("  - Press ESCAPE to end early")
        println()

        val tests = listOf(
            ButtonTest(Buttons.DPAD_LEFT, "D-Pad LEFT"),
            ButtonTest(Buttons.DPAD_UP, "D-Pad UP"),
            ButtonTest(Buttons.DPAD_RIGHT, "D-Pad RIGHT"),
            ButtonTest(Buttons.DPAD_DOWN, "D-Pad DOWN"),
            ButtonTest(Buttons.A, "A button"),
            ButtonTest(Buttons.B, "B button"),
            ButtonTest(Buttons.X, "X button"),
            ButtonTest(Buttons.Y, "Y button"),
            ButtonTest(Buttons.L1, "Left shoulder (L1/LB)"),
            ButtonTest(Buttons.R1, "Right shoulder (R1/RB)"),
            ButtonTest(Buttons.SELECT, "Select/Back/Minus"),
            ButtonTest(Buttons.START, "Start/Options/Plus")
        )

        var currentTestIndex = 0
        var testComplete = false
        var lastPressTime = 0L
        var spaceWasPressed = false

        createGameContext(
            title = "Controller Mapper - ${ControllerConfig.mode}",
            width = 800,
            height = 600,
            logLevel = Logger.Level.DEBUG
        ) {
            GameRunner(frameRate = 60) {
                object : Game {
                    private var frameCount = 0

                    init {
                        println()
                        println("Detected Mode: ${ControllerConfig.mode}")

                        val controllerId = getControllerContext().controller.getFirstControllerId()
                        if (controllerId == null) {
                            println("ERROR: No controller detected! Connect a controller and restart.")
                            isRunning = false
                        } else {
                            println("Controller detected (ID: $controllerId)")
                            println()
                            printCurrentTest()
                        }
                    }

                    private fun printCurrentTest() {
                        if (currentTestIndex < tests.size) {
                            val test = tests[currentTestIndex]
                            println("Test ${currentTestIndex + 1}/${tests.size}: Press ${test.description} (SPACE=skip)")
                        }
                    }

                    override fun update() {
                        frameCount++

                        if (testComplete) {
                            if (frameCount > 180) isRunning = false
                            return
                        }

                        if (currentTestIndex >= tests.size) {
                            printResults()
                            testComplete = true
                            return
                        }

                        useKeyboardContext {
                            if (keyboard.isEscapePressed()) {
                                printResults()
                                testComplete = true
                                return
                            }

                            val spacePressed = keyboard.isSpacePressed()
                            if (spacePressed && !spaceWasPressed) {
                                println("  Skipped ${tests[currentTestIndex].button}")
                                currentTestIndex++
                                println()
                                printCurrentTest()
                            }
                            spaceWasPressed = spacePressed
                        }

                        if (currentTestIndex >= tests.size) return

                        val currentTest = tests[currentTestIndex]
                        val currentTime = getCurrentMilliseconds()

                        useControllerContext {
                            if (controller.isButtonPressed(currentTest.button) && currentTime - lastPressTime > 300L) {
                                lastPressTime = currentTime
                                currentTest.detected = true
                                println("  OK: ${currentTest.button}")
                                currentTestIndex++
                                println()
                                printCurrentTest()
                            }
                        }
                    }

                    override fun draw() {
                        useSDLContext {
                            if (testComplete) fillScreen(Color.darkGreen)
                            else fillScreen(Color.darkBlue)
                            flipScreen()
                        }
                    }

                    private fun printResults() {
                        println()
                        println("=".repeat(60))
                        println("RESULTS (Mode: ${ControllerConfig.mode})")
                        println("=".repeat(60))
                        for (test in tests) {
                            val status = if (test.detected) "OK" else "---"
                            val padding = " ".repeat(maxOf(0, 15 - test.button.name.length))
                            println("  ${test.button.name}${padding} $status    ${test.description}")
                        }
                        println()
                        println("Test complete. Window closing in 3 seconds...")
                    }

                    override fun cleanup() {
                        println("Test finished")
                    }
                }
            }
        }
    }
}
