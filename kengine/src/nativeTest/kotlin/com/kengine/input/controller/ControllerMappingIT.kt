package com.kengine.input.controller

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.getCurrentMilliseconds
import kotlin.test.Test

/**
 * Interactive Controller Button Mapping Test
 * 
 * This test helps you discover which SDL button indices correspond to physical buttons.
 * Similar to controller_mapper.py but for SDL3 Gamepad API.
 * 
 * ALWAYS runs in GAMEPAD mode to test SDL's standardized mappings.
 * 
 * Instructions:
 * - Press each requested button when prompted
 * - Press SPACE to skip a button if it doesn't exist on your controller
 * - Press R to restart the current button test
 * - Results will be printed at the end
 */
class ControllerMappingIT {
    
    data class ButtonTest(
        val name: String,
        val description: String,
        var sdlButtonIndex: Int? = null,
        var completed: Boolean = false
    )

    @Test
    fun `interactive controller button mapping - gamepad mode`() {
        println("=" .repeat(60))
        println("🎮 INTERACTIVE CONTROLLER BUTTON MAPPING TEST")
        println("=" .repeat(60))
        println()
        println("⚠️  IMPORTANT: You must run this test in GAMEPAD mode!")
        println()
        println("Run with:")
        println("  export KENGINE_CONTROLLER_MODE=gamepad")
        println("  ./gradlew :kengine:nativeTest --tests \"com.kengine.input.controller.ControllerMappingIT\"")
        println()
        println("The test will detect and report which mode you're in.")
        println()
        println("Instructions:")
        println("  - Press each requested button when prompted")
        println("  - Press SPACE to skip if button doesn't exist")
        println("  - Press R to restart current test")
        println("  - Test will automatically end after all buttons")
        println()
        
        val tests = mutableListOf(
            ButtonTest("DPAD_LEFT", "D-Pad LEFT"),
            ButtonTest("DPAD_UP", "D-Pad UP"),
            ButtonTest("DPAD_RIGHT", "D-Pad RIGHT"),
            ButtonTest("DPAD_DOWN", "D-Pad DOWN"),
            ButtonTest("L1", "Left shoulder (L1/LB)"),
            ButtonTest("R1", "Right shoulder (R1/RB)"),
            ButtonTest("L2", "Left trigger (L2/LT) - if it's a button"),
            ButtonTest("R2", "Right trigger (R2/RT) - if it's a button"),
            ButtonTest("SELECT", "Select/Back/Minus"),
            ButtonTest("START", "Start/Options/Plus"),
            ButtonTest("Y", "Y button (top face)"),
            ButtonTest("X", "X button (left face)"),
            ButtonTest("B", "B button (right face)"),
            ButtonTest("A", "A button (bottom face)")
        )
        
        var currentTestIndex = 0
        var testComplete = false
        val pressedButtons = mutableSetOf<Int>()
        var lastPressTime = 0L
        var spaceWasPressed = false
        var rWasPressed = false
        
        createGameContext(
            title = "Controller Mapper - Mode: ${ControllerConfig.mode}",
            width = 800,
            height = 600,
            logLevel = Logger.Level.DEBUG
        ) {
            GameRunner(frameRate = 60) {
                object : Game {
                    private var frameCount = 0
                    private val validSetup: Boolean
                    
                    init {
                        println()
                        println("🔍 Detected Mode: ${ControllerConfig.mode}")
                        println()
                        
                        validSetup = when {
                            ControllerConfig.mode != ControllerMode.GAMEPAD -> {
                                println("❌ ERROR: Test is running in ${ControllerConfig.mode} mode!")
                                println("   This test MUST run in GAMEPAD mode.")
                                println()
                                println("   Please run:")
                                println("   export KENGINE_CONTROLLER_MODE=gamepad")
                                println("   ./gradlew :kengine:nativeTest --tests \"com.kengine.input.controller.ControllerMappingIT\"")
                                println()
                                isRunning = false
                                false
                            }
                            getControllerContext().controller.getFirstControllerId() == null -> {
                                println("❌ ERROR: No controller detected!")
                                println("   Please connect a controller and restart the test")
                                isRunning = false
                                false
                            }
                            else -> {
                                println("✅ Controller detected (ID: ${getControllerContext().controller.getFirstControllerId()})")
                                println("✅ Mode is correct: GAMEPAD")
                                println()
                                printCurrentTest()
                                true
                            }
                        }
                    }
                    
                    private fun printCurrentTest() {
                        if (currentTestIndex < tests.size) {
                            val test = tests[currentTestIndex]
                            println("📍 Test ${currentTestIndex + 1}/${tests.size}: Press ${test.description}")
                            println("   (SPACE=skip, R=restart)")
                        }
                    }

                    override fun update() {
                        if (!validSetup) return
                        
                        frameCount++
                        
                        if (testComplete) {
                            // Give a moment to see the results before closing
                            if (frameCount > 180) {  // 3 seconds at 60 FPS
                                isRunning = false
                            }
                            return
                        }
                        
                        if (currentTestIndex >= tests.size) {
                            printResults()
                            testComplete = true
                            return
                        }
                        
                        val currentTest = tests[currentTestIndex]
                        val controllerId = getControllerContext().controller.getFirstControllerId() ?: return
                        
                        // Check keyboard for skip/restart (with edge detection to prevent multi-triggering)
                        useKeyboardContext {
                            val spacePressed = keyboard.isSpacePressed()
                            val rPressed = keyboard.isRPressed()
                            
                            // Only trigger on press edge (transition from not-pressed to pressed)
                            if (spacePressed && !spaceWasPressed && !currentTest.completed) {
                                println("   ⏭️  Skipped ${currentTest.name}")
                                currentTest.completed = true
                                currentTestIndex++
                                pressedButtons.clear()
                                println()
                                printCurrentTest()
                            }
                            spaceWasPressed = spacePressed
                            
                            if (rPressed && !rWasPressed && !currentTest.completed) {
                                println("   🔄 Restarting ${currentTest.name}")
                                currentTest.completed = false
                                currentTest.sdlButtonIndex = null
                                pressedButtons.clear()
                            }
                            rWasPressed = rPressed
                        }
                        
                        // Check all buttons (0-20 for gamepad)
                        if (!currentTest.completed) {
                            val currentTime = getCurrentMilliseconds()
                            
                            for (buttonIndex in 0..20) {
                                val isPressed = getControllerContext().controller.isButtonPressed(controllerId, buttonIndex)
                                
                                if (isPressed && !pressedButtons.contains(buttonIndex)) {
                                    // Debounce: only register if enough time has passed since last press
                                    if (currentTime - lastPressTime > 300L) {
                                        logger.info { "🔘 Button pressed: index $buttonIndex" }
                                        currentTest.sdlButtonIndex = buttonIndex
                                        currentTest.completed = true
                                        pressedButtons.add(buttonIndex)
                                        lastPressTime = currentTime
                                        
                                        println("   ✅ ${currentTest.name}: SDL Button $buttonIndex")
                                        currentTestIndex++
                                        pressedButtons.clear()
                                        println()
                                        printCurrentTest()
                                        break
                                    }
                                }
                            }
                        }
                    }

                    override fun draw() {
                        useSDLContext {
                            if (testComplete) {
                                fillScreen(Color.darkGreen)
                            } else if (currentTestIndex < tests.size) {
                                fillScreen(Color.darkBlue)
                            } else {
                                fillScreen(Color.black)
                            }
                            flipScreen()
                        }
                    }
                    
                    private fun printResults() {
                        println()
                        println("=" .repeat(60))
                        println("🎯 BUTTON MAPPING RESULTS")
                        println("=" .repeat(60))
                        println("Controller mode: GAMEPAD")
                        println()
                        
                        // Print results table
                        println("Button Mappings (Physical → SDL Index):")
                        println("-" .repeat(40))
                        
                        for (test in tests) {
                            if (test.completed && test.sdlButtonIndex != null) {
                                val padding = " ".repeat(maxOf(0, 15 - test.name.length))
                                println("  ${test.name}${padding} → SDL Button ${test.sdlButtonIndex}")
                            } else if (test.completed) {
                                val padding = " ".repeat(maxOf(0, 15 - test.name.length))
                                println("  ${test.name}${padding} → (skipped)")
                            }
                        }
                        
                        println()
                        println("=" .repeat(60))
                        println("📋 KENGINE MAPPING CODE")
                        println("=" .repeat(60))
                        println("Copy this to SDLGamepad.kt if mappings are incorrect:")
                        println()
                        
                        // Generate const values
                        tests.filter { it.completed && it.sdlButtonIndex != null }.forEach { test ->
                            val constName = when(test.name) {
                                "DPAD_LEFT" -> "BUTTON_DPAD_LEFT"
                                "DPAD_UP" -> "BUTTON_DPAD_UP"
                                "DPAD_RIGHT" -> "BUTTON_DPAD_RIGHT"
                                "DPAD_DOWN" -> "BUTTON_DPAD_DOWN"
                                "L1" -> "BUTTON_LEFT_SHOULDER"
                                "R1" -> "BUTTON_RIGHT_SHOULDER"
                                "SELECT" -> "BUTTON_BACK"
                                "START" -> "BUTTON_START"
                                "Y" -> "BUTTON_NORTH"
                                "X" -> "BUTTON_WEST"
                                "B" -> "BUTTON_EAST"
                                "A" -> "BUTTON_SOUTH"
                                else -> "BUTTON_${test.name}"
                            }
                            println("const val $constName = ${test.sdlButtonIndex}  // ${test.description}")
                        }
                        
                        println()
                        println("✅ Mapping test complete!")
                        println("   Window will close in 3 seconds...")
                    }

                    override fun cleanup() {
                        println("\n👋 Test finished")
                    }
                }
            }
        }
    }
}

