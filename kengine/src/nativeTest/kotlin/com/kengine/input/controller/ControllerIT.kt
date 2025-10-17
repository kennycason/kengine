package com.kengine.input.controller

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.input.controller.controls.Buttons
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.useTimer
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Integration test for controller input.
 *
 * This test initializes the controller system and runs for a few seconds,
 * logging controller events to verify the dual-mode system works correctly.
 *
 * Run with environment variable to test different modes:
 * - KENGINE_CONTROLLER_MODE=joystick (default)
 * - KENGINE_CONTROLLER_MODE=gamepad
 *
 * The test runs for 5 seconds, during which you can:
 * - Verify controller is detected (check logs - no duplicates should appear)
 * - Press buttons and see debug logs for button events
 * - Move analog sticks and see debug logs for axis events
 * - Hot-plug controllers and verify add/remove events work
 *
 * For manual visual testing, uncomment the button logging in the update() method.
 */
class ControllerIT {

    @Ignore
    fun `controller initialization test - joystick and gamepad modes`() {
        createGameContext(
            title = "Controller Test - Mode: ${ControllerConfig.mode}",
            width = 800,
            height = 600,
            logLevel = Logger.Level.INFO  // Set to DEBUG to see all button/axis events
        ) {
            GameRunner(frameRate = 60) {
                object : Game {
                    init {
                        logger.info { "=".repeat(60) }
                        logger.info { "Controller Integration Test Starting" }
                        logger.info { "Mode: ${ControllerConfig.mode}" }
                        logger.info { "=".repeat(60) }

                        useTimer(5000L) {
                            logger.info { "=".repeat(60) }
                            logger.info { "Controller Test Complete" }
                            logger.info { "=".repeat(60) }
                            isRunning = false
                        }
                    }

                    override fun update() {
                        // Log controller state periodically for manual verification
                        val controllerId = getControllerContext().controller.getFirstControllerId()
                        if (controllerId != null) {
                            // Uncomment to see button presses in logs:
                            // if (getControllerContext().controller.isButtonPressed(Buttons.A)) {
                            //     logger.info { "Button A pressed!" }
                            // }
                            // if (getControllerContext().controller.isButtonPressed(Buttons.START)) {
                            //     logger.info { "Button START pressed!" }
                            // }

                            // Check axis values (will log if stick is moved)
                            val leftX = getControllerContext().controller.getAxisValue(controllerId, 0)
                            val leftY = getControllerContext().controller.getAxisValue(controllerId, 1)
                            if (leftX != 0f || leftY != 0f) {
                                logger.info { "Left stick: X=$leftX, Y=$leftY" }
                            }
                        }
                    }

                    override fun draw() {
                        useSDLContext {
                            // Simple visual feedback - green if controller detected, red if not
                            val controllerId = getControllerContext().controller.getFirstControllerId()
                            if (controllerId != null) {
                                fillScreen(Color.darkGreen)
                            } else {
                                fillScreen(Color.darkRed)
                            }

                            flipScreen()
                        }
                    }

                    override fun cleanup() {
                        logger.info { "Test cleanup complete" }
                    }
                }
            }
        }
    }
}
