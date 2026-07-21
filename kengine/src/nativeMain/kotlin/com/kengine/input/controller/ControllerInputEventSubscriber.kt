package com.kengine.input.controller

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapper
import com.kengine.input.controller.controls.ControllerMapping
import com.kengine.input.controller.controls.HatDirection
import com.kengine.log.Logging
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.useSDLEventContext
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import sdl3.*
import kotlin.math.abs

private const val TRIGGER_BUTTON_PRESS_THRESHOLD = 0.45f

internal fun isMappedButtonPressed(
    button: Buttons,
    state: ControllerInputEventSubscriber.ControllerState,
    mapping: ControllerMapping
): Boolean {
    val primaryButtonIndex = mapping.gamepadMappings[button]
    if (primaryButtonIndex != null && isPhysicalButtonPressed(primaryButtonIndex, state, mapping)) {
        return true
    }

    return mapping.alternateGamepadMappings[button].orEmpty().any { buttonIndex ->
        isPhysicalButtonPressed(buttonIndex, state, mapping)
    } || mapping.buttonAxisMappings[button].orEmpty().any { axisIndex ->
        isTriggerAxisPressed(axisIndex, state, mapping)
    }
}

private fun isPhysicalButtonPressed(
    buttonIndex: Int,
    state: ControllerInputEventSubscriber.ControllerState,
    mapping: ControllerMapping
): Boolean {
    return when (mapping.buttonMappings[buttonIndex]) {
        ButtonType.REGULAR -> state.buttons.getOrNull(buttonIndex) == true
        ButtonType.HAT_UP -> state.isHatDirectionPressed(0, HatDirection.UP)
        ButtonType.HAT_DOWN -> state.isHatDirectionPressed(0, HatDirection.DOWN)
        ButtonType.HAT_LEFT -> state.isHatDirectionPressed(0, HatDirection.LEFT)
        ButtonType.HAT_RIGHT -> state.isHatDirectionPressed(0, HatDirection.RIGHT)
        null -> state.buttons.getOrNull(buttonIndex) == true
    }
}

private fun isTriggerAxisPressed(
    axisIndex: Int,
    state: ControllerInputEventSubscriber.ControllerState,
    mapping: ControllerMapping
): Boolean {
    if (mapping.axisMappings[axisIndex] != AxisType.TRIGGER) {
        return false
    }
    return (state.axes.getOrNull(axisIndex) ?: 0.0f) >= TRIGGER_BUTTON_PRESS_THRESHOLD
}

/**
 * Handles controller input events and maintains state for all connected controllers.
 * Supports both SDL Joystick API (with custom mappings) and SDL Gamepad API (standardized).
 * Provides both specific controller access and convenience methods for accessing
 * the first active controller.
 */
class ControllerInputEventSubscriber(
    private val mode: ControllerMode,
    val deadzone: Float = 0.05f
) : Logging {
    private val controllerStates = mutableMapOf<UInt, ControllerState>()
    private val controllerMappings = mutableMapOf<UInt, ControllerMapping>()
    private var initialized = false
    
    // Track seen devices to prevent duplicates
    private val seenDeviceKeys = mutableSetOf<String>()
    private val instanceToDeviceKey = mutableMapOf<UInt, String>()
    
    // Track opened SDL resources so we can close them properly
    private val openedJoysticks = mutableMapOf<UInt, CPointer<cnames.structs.SDL_Joystick>>()
    private val openedGamepads = mutableMapOf<UInt, CPointer<cnames.structs.SDL_Gamepad>>()
    private var lastActiveControllerId: UInt? = null

    /**
     * Gets the ID of the first connected controller, if any exist.
     * Prefer an axis-capable controller because SDL can expose a single physical
     * controller as multiple logical devices.
     * @return The first controller ID, or null if no controllers are connected
     */
    fun getFirstControllerId(): UInt? {
        return getFirstControllerIdWithAxes() ?: controllerStates.keys.firstOrNull()
    }

    /**
     * Gets the preferred connected controller that can report analog axes.
     * The most recently active axis-capable controller wins, then SDL's first
     * reported axis-capable controller is used as a fallback.
     */
    fun getFirstControllerIdWithAxes(): UInt? {
        lastActiveControllerId?.let { controllerId ->
            if (controllerStates[controllerId]?.axes?.isNotEmpty() == true) {
                return controllerId
            }
        }
        return controllerStates.entries.firstOrNull { (_, state) ->
            state.axes.isNotEmpty()
        }?.key
    }

    data class ControllerState(
        var axes: FloatArray,
        var buttons: BooleanArray,
        var hatStates: IntArray  // Store raw hat values (only used in JOYSTICK mode)
    ) {
        // Helper function to check if a hat direction is pressed
        fun isHatDirectionPressed(hatIndex: Int, direction: HatDirection): Boolean {
            return if (hatIndex < hatStates.size) {
                val hatValue = hatStates[hatIndex]
                direction.isPressed(hatValue)
            } else false
        }
    }

    fun init() {
        if (!initialized) {
            initialized = true
        useSDLEventContext {
                logger.info { "Controller mode: $mode - Subscribing to events" }
            subscribe(SDLEventContext.EventType.CONTROLLER, ::handleControllerEvent)
        }
        initControllers()
        } else {
            logger.warn { "ControllerInputEventSubscriber already initialized, skipping duplicate init" }
        }
    }

    private fun handleControllerEvent(event: SDL_Event) {
        when (mode) {
            ControllerMode.JOYSTICK -> handleJoystickEvent(event)
            ControllerMode.GAMEPAD -> handleGamepadEvent(event)
        }
    }

    // ========== JOYSTICK MODE HANDLERS ==========
    
    private fun handleJoystickEvent(event: SDL_Event) {
        when (event.type) {
            SDL_EVENT_JOYSTICK_BUTTON_DOWN -> handleJoystickButtonEvent(event, true)
            SDL_EVENT_JOYSTICK_BUTTON_UP -> handleJoystickButtonEvent(event, false)
            SDL_EVENT_JOYSTICK_AXIS_MOTION -> handleJoystickAxisEvent(event)
            SDL_EVENT_JOYSTICK_HAT_MOTION -> handleJoystickHatEvent(event)
            SDL_EVENT_JOYSTICK_ADDED -> handleJoystickAdded(event)
            SDL_EVENT_JOYSTICK_REMOVED -> handleJoystickRemoved(event)
        }
    }

    private fun handleJoystickButtonEvent(event: SDL_Event, isPressed: Boolean) {
        val joystickID = event.jbutton.which
        val button = event.jbutton.button.toInt()
        if (logger.isDebugEnabled()) {
            logger.debug { "Joystick Button ${if (isPressed) "Down" else "Up"} - ID: $joystickID, Button: $button" }
        }
        controllerStates[joystickID]?.buttons?.getOrNull(button)?.let {
            controllerStates[joystickID]?.buttons?.set(button, isPressed)
        }
        if (isPressed) {
            markControllerActive(joystickID)
        }
    }

    private fun handleJoystickAxisEvent(event: SDL_Event) {
        val joystickID = event.jaxis.which
        val axis = event.jaxis.axis.toInt()
        val value = event.jaxis.value.toFloat() / 32767.0f

        // Apply deadzone threshold
        val adjustedValue = if (abs(value) < deadzone) 0f else value

        if (logger.isDebugEnabled() && adjustedValue != 0f) {
            logger.debug { "Joystick Axis Motion - ID: $joystickID, Axis: $axis, Value: $adjustedValue" }
        }

        controllerStates[joystickID]?.axes?.getOrNull(axis)?.let {
            controllerStates[joystickID]?.axes?.set(axis, adjustedValue)
        }
        if (adjustedValue != 0f) {
            markControllerActive(joystickID)
        }
    }

    private fun handleJoystickHatEvent(event: SDL_Event) {
        val joystickID = event.jhat.which
        val hatID = event.jhat.hat.toInt()
        val hatValue = event.jhat.value.toInt()

        controllerStates[joystickID]?.let { state ->
            if (hatID < state.hatStates.size) {
                state.hatStates[hatID] = hatValue
                if (logger.isDebugEnabled()) {
                    logger.debug {
                        "Hat[$hatID] State: " +
                            "UP=${HatDirection.UP.isPressed(hatValue)}, " +
                            "RIGHT=${HatDirection.RIGHT.isPressed(hatValue)}, " +
                            "DOWN=${HatDirection.DOWN.isPressed(hatValue)}, " +
                            "LEFT=${HatDirection.LEFT.isPressed(hatValue)}"
                    }
                }
                if (hatValue != SDL_HAT_CENTERED.toInt()) {
                    markControllerActive(joystickID)
                }
            }
        }
    }

    private fun handleJoystickAdded(event: SDL_Event) {
        val instanceId = event.jdevice.which
        logger.info { "Joystick added (instanceId=$instanceId)" }

        // Already registered during init — skip
        if (controllerStates.containsKey(instanceId)) {
            logger.info { "Joystick $instanceId already registered, ignoring add event" }
            return
        }

        val joystick = SDL_OpenJoystick(instanceId)
        if (joystick == null) {
            logger.warn { "Failed to open joystick $instanceId: ${SDL_GetError()?.toKString()}" }
            return
        }
        
        val name = SDL_GetJoystickName(joystick)?.toKString() ?: "Unknown Controller"
        val numAxes = SDL_GetNumJoystickAxes(joystick)
        val numButtons = SDL_GetNumJoystickButtons(joystick)
        val numHats = SDL_GetNumJoystickHats(joystick)
        
        logger.info { "Opened joystick '$name' (Axes:$numAxes Buttons:$numButtons Hats:$numHats)" }
        
        // Skip phantom/virtual controllers (no axes AND no hats = likely virtual device)
        if (numAxes == 0 && numHats == 0) {
            logger.warn { "Skipping phantom controller '$name' (0 axes, 0 hats)" }
            SDL_CloseJoystick(joystick)
            return
        }
        
        // Create device key for deduplication
        val deviceKey = "$name:$numAxes:$numButtons:$numHats"
        if (seenDeviceKeys.contains(deviceKey)) {
            logger.warn { "Skipping duplicate controller '$name'" }
            SDL_CloseJoystick(joystick)
            return
        }
        
        val mapping = ControllerMapper.getMapping(name)
        logger.info { "Controller '$name' mapped to: ${mapping?.name ?: "None"}" }
        
        controllerStates[instanceId] = ControllerState(
            axes = FloatArray(numAxes),
            buttons = BooleanArray(numButtons),
            hatStates = IntArray(numHats)
        )
        
        if (mapping != null) {
            controllerMappings[instanceId] = mapping
        }
        
        openedJoysticks[instanceId] = joystick
        seenDeviceKeys.add(deviceKey)
        instanceToDeviceKey[instanceId] = deviceKey
    }

    private fun handleJoystickRemoved(event: SDL_Event) {
        val instanceId = event.jdevice.which
        logger.info { "Joystick removed (instanceId=$instanceId)" }
        
        // Close the SDL joystick resource
        openedJoysticks[instanceId]?.let { joystick ->
            SDL_CloseJoystick(joystick)
            logger.info { "Closed joystick (instanceId=$instanceId)" }
        }
        
        controllerStates.remove(instanceId)
        controllerMappings.remove(instanceId)
        openedJoysticks.remove(instanceId)
        instanceToDeviceKey.remove(instanceId)?.let { seenDeviceKeys.remove(it) }
        if (lastActiveControllerId == instanceId) {
            lastActiveControllerId = null
        }
    }

    // ========== GAMEPAD MODE HANDLERS ==========
    
    private fun handleGamepadEvent(event: SDL_Event) {
        when (event.type) {
            SDL_EVENT_GAMEPAD_BUTTON_DOWN -> handleGamepadButtonEvent(event, true)
            SDL_EVENT_GAMEPAD_BUTTON_UP -> handleGamepadButtonEvent(event, false)
            SDL_EVENT_GAMEPAD_AXIS_MOTION -> handleGamepadAxisEvent(event)
            SDL_EVENT_GAMEPAD_ADDED -> handleGamepadAdded(event)
            SDL_EVENT_GAMEPAD_REMOVED -> handleGamepadRemoved(event)
        }
    }

    private fun handleGamepadButtonEvent(event: SDL_Event, isPressed: Boolean) {
        val gamepadID = event.gbutton.which
        
        // Ignore events from controllers we filtered out as duplicates
        if (!controllerStates.containsKey(gamepadID)) {
            if (logger.isDebugEnabled()) {
                logger.debug { "Ignoring button event from filtered controller ID: $gamepadID" }
            }
            return
        }
        
        val button = event.gbutton.button.toInt()
        if (logger.isDebugEnabled()) {
            logger.debug { "Gamepad Button ${if (isPressed) "Down" else "Up"} - ID: $gamepadID, Button: $button" }
        }
        controllerStates[gamepadID]?.buttons?.getOrNull(button)?.let {
            controllerStates[gamepadID]?.buttons?.set(button, isPressed)
        }
        if (isPressed) {
            markControllerActive(gamepadID)
        }
    }

    private fun handleGamepadAxisEvent(event: SDL_Event) {
        val gamepadID = event.gaxis.which
        
        // Ignore events from controllers we filtered out as duplicates
        if (!controllerStates.containsKey(gamepadID)) {
            return  // Skip debug log for axis to reduce noise
        }
        
        val axis = event.gaxis.axis.toInt()
        val value = event.gaxis.value.toFloat() / 32767.0f

        // Apply deadzone threshold
        val adjustedValue = if (abs(value) < deadzone) 0f else value

        if (logger.isDebugEnabled() && adjustedValue != 0f) {
            logger.debug { "Gamepad Axis Motion - ID: $gamepadID, Axis: $axis, Value: $adjustedValue" }
        }

        controllerStates[gamepadID]?.axes?.getOrNull(axis)?.let {
            controllerStates[gamepadID]?.axes?.set(axis, adjustedValue)
        }
        if (adjustedValue != 0f) {
            markControllerActive(gamepadID)
        }
    }

    private fun handleGamepadAdded(event: SDL_Event) {
        val instanceId = event.gdevice.which
        logger.info { "Gamepad added event (instanceId=$instanceId)" }
        
        // Check if we already have this instance ID (from init scan)
        if (controllerStates.containsKey(instanceId)) {
            logger.info { "Gamepad $instanceId already registered (from init), ignoring hotplug event" }
            return
        }
        
        // Open only this specific device
        val gamepad = SDL_OpenGamepad(instanceId)
        if (gamepad == null) {
            logger.warn { "Failed to open gamepad $instanceId: ${SDL_GetError()?.toKString()}" }
            return
        }
        
        val name = SDL_GetGamepadName(gamepad)?.toKString() ?: "Unknown Gamepad"
        logger.info { "Opened gamepad '$name'" }
        
        // Deduplicate by name - SDL sometimes exposes same device via different backends
        // For SNES controllers, "SNES Controller" is usually the phantom/minimal version
        // while "Nintendo SNES Controller" is the full one
        val normalizedName = name.replace("Nintendo ", "").replace("Controller", "").trim()
        val deviceKey = "gamepad:$normalizedName"
        
        if (seenDeviceKeys.contains(deviceKey)) {
            logger.warn { "Skipping duplicate gamepad '$name' (normalized: $normalizedName) - not adding to controller states" }
            SDL_CloseGamepad(gamepad)  // Close the duplicate
            return  // Exit the function, don't add to controllerStates
        }
        
        // Gamepad API normalizes to fixed button/axis counts
        val numButtons = 21  // SDL_GAMEPAD_BUTTON_COUNT
        val numAxes = 6      // SDL_GAMEPAD_AXIS_COUNT
        
        val mapping = ControllerMapper.getMapping(name)
        logger.info { "Gamepad '$name' mapped to: ${mapping?.name ?: "None"}" }
        
        controllerStates[instanceId] = ControllerState(
            axes = FloatArray(numAxes),
            buttons = BooleanArray(numButtons),
            hatStates = IntArray(0)  // Gamepad API doesn't use hats (D-pad is mapped to buttons)
        )
        
        if (mapping != null) {
            controllerMappings[instanceId] = mapping
        }
        
        openedGamepads[instanceId] = gamepad
        seenDeviceKeys.add(deviceKey)
        logger.info { "✅ Gamepad '$name' (ID:$instanceId) successfully added via hotplug" }
    }

    private fun handleGamepadRemoved(event: SDL_Event) {
        val instanceId = event.gdevice.which
        logger.info { "Gamepad removed (instanceId=$instanceId)" }
        
        // Close the SDL gamepad resource
        openedGamepads[instanceId]?.let { gamepad ->
            SDL_CloseGamepad(gamepad)
            logger.info { "Closed gamepad (instanceId=$instanceId)" }
        }
        
        controllerStates.remove(instanceId)
        controllerMappings.remove(instanceId)
        openedGamepads.remove(instanceId)
        instanceToDeviceKey.remove(instanceId)?.let { seenDeviceKeys.remove(it) }
        if (lastActiveControllerId == instanceId) {
            lastActiveControllerId = null
        }
    }

    // ========== INITIALIZATION ==========

    private fun initControllers() {
        when (mode) {
            ControllerMode.JOYSTICK -> initJoystickControllers()
            ControllerMode.GAMEPAD -> initGamepadControllers()
        }
    }

    private fun initJoystickControllers() {
        memScoped {
            val countPtr = alloc<IntVar>()
            val joysticks = SDL_GetJoysticks(countPtr.ptr)
            val count = countPtr.value
            logger.info { "Joysticks found: $count" }

            joysticks?.let { joyArray ->
                for (i in 0 until count) {
                    val instanceId = joyArray[i]
                    SDL_OpenJoystick(instanceId)?.let { joystick ->
                        val name = SDL_GetJoystickName(joystick)?.toKString() ?: "Unknown Controller"
                        val numAxes = SDL_GetNumJoystickAxes(joystick)
                        val numButtons = SDL_GetNumJoystickButtons(joystick)
                        val numHats = SDL_GetNumJoystickHats(joystick)
                        
                        logger.info { 
                            "Joystick #$i - '$name' (ID:$instanceId Axes:$numAxes Buttons:$numButtons Hats:$numHats)"
                        }
                        
                        // Skip phantom/virtual controllers
                        if (numAxes == 0 && numHats == 0) {
                            logger.warn { "Skipping phantom controller '$name' (0 axes, 0 hats)" }
                            return@let
                        }
                        
                        // Check for duplicates
                        val deviceKey = "$name:$numAxes:$numButtons:$numHats"
                        if (seenDeviceKeys.contains(deviceKey)) {
                            logger.warn { "Skipping duplicate controller '$name'" }
                            SDL_CloseJoystick(joystick)
                            return@let
                        }
                        
                        val mapping = ControllerMapper.getMapping(name)
                        logger.info { "Controller '$name' mapped to: ${mapping?.name ?: "None"}" }

                        controllerStates[instanceId] = ControllerState(
                            axes = FloatArray(numAxes),
                            buttons = BooleanArray(numButtons),
                            hatStates = IntArray(numHats)
                        )

                        if (mapping != null) {
                            controllerMappings[instanceId] = mapping
                        }
                        
                        openedJoysticks[instanceId] = joystick
                        seenDeviceKeys.add(deviceKey)
                        instanceToDeviceKey[instanceId] = deviceKey
                    } ?: run {
                        logger.warn { "Failed to open joystick $i: ${SDL_GetError()?.toKString()}" }
                    }
                }
            }
        }
    }

    private fun initGamepadControllers() {
        memScoped {
            val countPtr = alloc<IntVar>()
            val gamepads = SDL_GetGamepads(countPtr.ptr)
            val count = countPtr.value
            logger.info { "Gamepads found: $count" }

            gamepads?.let { gamepadArray ->
                for (i in 0 until count) {
                    val instanceId = gamepadArray[i]
                    SDL_OpenGamepad(instanceId)?.let { gamepad ->
                        val name = SDL_GetGamepadName(gamepad)?.toKString() ?: "Unknown Gamepad"
                        
                        logger.info { "Gamepad #$i - '$name' (ID:$instanceId)" }
                        
                        // Deduplicate by normalized name
                        val normalizedName = name.replace("Nintendo ", "").replace("Controller", "").trim()
                        val deviceKey = "gamepad:$normalizedName"
                        
                        if (seenDeviceKeys.contains(deviceKey)) {
                            logger.warn { "Skipping duplicate gamepad '$name' (normalized: $normalizedName)" }
                            SDL_CloseGamepad(gamepad)
                            return@let
                        }
                        
                        // Gamepad API normalizes to fixed counts
                        val numButtons = 21  // SDL_GAMEPAD_BUTTON_COUNT
                        val numAxes = 6      // SDL_GAMEPAD_AXIS_COUNT
                        
                        val mapping = ControllerMapper.getMapping(name)
                        logger.info { "Gamepad '$name' mapped to: ${mapping?.name ?: "None"}" }

                        controllerStates[instanceId] = ControllerState(
                            axes = FloatArray(numAxes),
                            buttons = BooleanArray(numButtons),
                            hatStates = IntArray(0)  // No hats in gamepad mode
                        )

                        if (mapping != null) {
                            controllerMappings[instanceId] = mapping
                        }
                        
                        openedGamepads[instanceId] = gamepad
                        seenDeviceKeys.add(deviceKey)
                    } ?: run {
                        logger.warn { "Failed to open gamepad $i: ${SDL_GetError()?.toKString()}" }
                    }
                }
            }
        }
    }

    // ========== PUBLIC API (mode-agnostic) ==========

    fun getAxisValue(controllerId: UInt, axisIndex: Int): Float {
        return controllerStates[controllerId]?.axes?.getOrNull(axisIndex) ?: 0.0f
    }

    fun getAxisValue(axisIndex: Int): Float {
        for (state in controllerStates.values) {
            val axisState = state.axes.getOrElse(axisIndex) { 0.0f }
            if (axisState != 0.0f) return axisState
        }
        return 0.0f
    }

    fun isButtonPressed(button: Buttons): Boolean {
        for ((id, state) in controllerStates) {
            val mapping = controllerMappings[id] ?: continue
            if (isMappedButtonPressed(button, state, mapping)) return true
        }
        return false
    }

    /**
     * Checks if a hat direction is pressed on any connected controller (JOYSTICK mode only)
     */
    fun isHatDirectionPressed(
        hatIndex: Int,
        direction: HatDirection
    ): Boolean {
        for (state in controllerStates.values) {
            if (state.isHatDirectionPressed(hatIndex, direction)) return true
        }
        return false
    }

    fun isHatDirectionPressed(
        controllerId: UInt,
        hatIndex: Int,
        direction: HatDirection
    ): Boolean {
        return controllerStates[controllerId]?.isHatDirectionPressed(hatIndex, direction) ?: false
    }

    fun cleanup() {
        logger.info { "Cleaning up ControllerInputEventSubscriber" }
        
        // Close all opened SDL joystick resources
        openedJoysticks.forEach { (id, joystick) ->
            SDL_CloseJoystick(joystick)
            logger.info { "Closed joystick (ID:$id)" }
        }
        
        // Close all opened SDL gamepad resources
        openedGamepads.forEach { (id, gamepad) ->
            SDL_CloseGamepad(gamepad)
            logger.info { "Closed gamepad (ID:$id)" }
        }
        
        controllerStates.clear()
        controllerMappings.clear()
        seenDeviceKeys.clear()
        openedJoysticks.clear()
        openedGamepads.clear()
        lastActiveControllerId = null
        initialized = false
    }

    private fun markControllerActive(controllerId: UInt) {
        if (controllerStates[controllerId]?.axes?.isNotEmpty() == true) {
            lastActiveControllerId = controllerId
        }
    }
}
