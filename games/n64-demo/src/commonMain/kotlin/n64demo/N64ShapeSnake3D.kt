package n64demo

import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderContext

class N64ShapeSnake3D {
    var score: Int = 0
        private set

    var consumedThisFrame: Boolean = false
        private set

    private var headX = 0
    private var headZ = 0
    private var heading = 0
    private var cameraOrbit = 0
    private var rng = 0x4E643D
    private var segmentCount = 0
    private var historyHead = 0

    private val wireframe = N64Wireframe3D()
    private val segmentShapeTypes = IntArray(MAX_SEGMENTS)
    private val segmentColors = IntArray(MAX_SEGMENTS)
    private val segmentSizes = IntArray(MAX_SEGMENTS)
    private val segmentSpinOffsets = IntArray(MAX_SEGMENTS)
    private val historyX = IntArray(MAX_HISTORY)
    private val historyZ = IntArray(MAX_HISTORY)
    private val historyHeading = IntArray(MAX_HISTORY)
    private val pickupX = IntArray(PICKUP_COUNT)
    private val pickupZ = IntArray(PICKUP_COUNT)
    private val pickupShapeTypes = IntArray(PICKUP_COUNT)
    private val pickupColors = IntArray(PICKUP_COUNT)
    private val pickupPhases = IntArray(PICKUP_COUNT)

    init {
        reset()
    }

    fun reset() {
        score = 0
        consumedThisFrame = false
        headX = 0
        headZ = 0
        heading = 0
        cameraOrbit = 0
        rng = 0x4E643D
        segmentCount = STARTING_SEGMENTS
        historyHead = 0

        setSegment(0, ShapeType.DIAMOND, rgba(248, 244, 112), 210, 0)
        setSegment(1, ShapeType.CUBE, rgba(70, 218, 255), 184, 72)
        setSegment(2, ShapeType.PYRAMID, rgba(255, 86, 178), 174, 144)
        setSegment(3, ShapeType.DIAMOND, rgba(126, 255, 136), 164, 216)
        setSegment(4, ShapeType.CUBE, rgba(255, 164, 74), 154, 288)

        var historyIndex = 0
        while (historyIndex < MAX_HISTORY) {
            historyX[historyIndex] = 0
            historyZ[historyIndex] = -historyIndex * HISTORY_STEP
            historyHeading[historyIndex] = heading
            historyIndex += 1
        }

        var pickupIndex = 0
        while (pickupIndex < PICKUP_COUNT) {
            respawnPickup(pickupIndex)
            pickupIndex += 1
        }
    }

    fun update(input: InputState, frame: Int) {
        consumedThisFrame = false

        heading = wrapAngle(heading + input.axis(InputButton.LEFT, InputButton.RIGHT) * TURN_SPEED)

        if (input.isPressed(InputButton.L)) {
            cameraOrbit = wrapAngle(cameraOrbit - CAMERA_ORBIT_SPEED)
        }
        if (input.isPressed(InputButton.R)) {
            cameraOrbit = wrapAngle(cameraOrbit + CAMERA_ORBIT_SPEED)
        }

        val speed = when {
            input.isPressed(InputButton.A) || input.isPressed(InputButton.UP) -> BOOST_SPEED
            input.isPressed(InputButton.DOWN) -> SLOW_SPEED
            else -> CRUISE_SPEED
        }

        headX += trigMul(speed, sinAngle(heading))
        headZ += trigMul(speed, cosAngle(heading))
        keepHeadInsideArena()
        recordHistory()

        var pickupIndex = 0
        while (pickupIndex < PICKUP_COUNT) {
            val dx = pickupX[pickupIndex] - headX
            val dz = pickupZ[pickupIndex] - headZ
            if (dx * dx + dz * dz <= PICKUP_RADIUS * PICKUP_RADIUS) {
                consumePickup(pickupIndex)
            }
            pickupIndex += 1
        }

        if (frame % 480 == 0 && segmentCount > STARTING_SEGMENTS) {
            // Keeps the wireframe command count bounded for the current N64 2D command bridge.
            segmentCount -= 1
        }
    }

    fun draw(render: RenderContext, frame: Int) {
        wireframe.configure(
            render = render,
            targetX = headX,
            targetY = HEAD_Y,
            targetZ = headZ,
            yaw = wrapAngle(heading + cameraOrbit),
            pitch = CAMERA_PITCH,
            cameraDistance = CAMERA_DISTANCE,
            projectionDistance = PROJECTION_DISTANCE,
            centerX = render.width / 2,
            centerY = 140
        )

        wireframe.drawGroundGrid(ARENA_HALF_SIZE, GRID_STEP, rgba(52, 64, 92, 150), rgba(92, 122, 170, 210))
        wireframe.drawArenaBounds(ARENA_HALF_SIZE, rgba(255, 255, 255, 170))

        var pickupIndex = 0
        while (pickupIndex < PICKUP_COUNT) {
            val phase = pickupPhases[pickupIndex]
            val bob = trigMul(PICKUP_BOB_HEIGHT, sinAngle((frame * 11 + phase) and 1023))
            wireframe.drawShape(
                type = pickupShapeTypes[pickupIndex],
                centerX = pickupX[pickupIndex],
                centerY = PICKUP_Y + bob,
                centerZ = pickupZ[pickupIndex],
                size = PICKUP_SIZE,
                rotationX = wrapAngle(frame * 3 + phase),
                rotationY = wrapAngle(frame * 5),
                rotationZ = wrapAngle(frame * 4),
                color = pulseColor(pickupColors[pickupIndex], frame + phase)
            )
            pickupIndex += 1
        }

        var index = segmentCount - 1
        while (index >= 0) {
            val slot = historySlot(index * SEGMENT_HISTORY_SPACING)
            val sampleX = historyX[slot]
            val sampleZ = historyZ[slot]
            val sampleHeading = historyHeading[slot]
            val bob = trigMul(SEGMENT_BOB_HEIGHT, sinAngle((frame * 19 + index * 47) and 1023))
            val spin = wrapAngle(frame * (4 + index) + segmentSpinOffsets[index])
            val size = if (index == 0) segmentSizes[index] + 30 else segmentSizes[index]

            wireframe.drawShape(
                type = segmentShapeTypes[index],
                centerX = sampleX,
                centerY = HEAD_Y + bob,
                centerZ = sampleZ,
                size = size,
                rotationX = wrapAngle(spin / 2),
                rotationY = wrapAngle(sampleHeading + spin),
                rotationZ = wrapAngle(spin / 3),
                color = pulseColor(segmentColors[index], frame + index * 17)
            )

            if (index > 0) {
                val nextSlot = historySlot((index - 1) * SEGMENT_HISTORY_SPACING)
                wireframe.drawLine3D(
                    sampleX,
                    HEAD_Y - CONNECTOR_DROP,
                    sampleZ,
                    historyX[nextSlot],
                    HEAD_Y - CONNECTOR_DROP,
                    historyZ[nextSlot],
                    rgba(255, 255, 255, 78)
                )
            }
            index -= 1
        }
    }

    private fun recordHistory() {
        historyHead -= 1
        if (historyHead < 0) {
            historyHead = MAX_HISTORY - 1
        }
        historyX[historyHead] = headX
        historyZ[historyHead] = headZ
        historyHeading[historyHead] = heading
    }

    private fun historySlot(historyIndex: Int): Int {
        val bounded = clampInt(historyIndex, 0, MAX_HISTORY - 1)
        val slot = historyHead + bounded
        return if (slot >= MAX_HISTORY) slot - MAX_HISTORY else slot
    }

    private fun consumePickup(index: Int) {
        score += 100 + segmentCount * 7
        consumedThisFrame = true

        if (segmentCount < MAX_SEGMENTS) {
            setSegment(
                index = segmentCount,
                shapeType = pickupShapeTypes[index],
                color = pickupColors[index],
                size = 144 + (nextRandom8() % 56),
                spinOffset = nextRandomAngle()
            )
            segmentCount += 1
        } else {
            val replaceIndex = 1 + (nextRandom8() % (segmentCount - 1))
            setSegment(
                index = replaceIndex,
                shapeType = pickupShapeTypes[index],
                color = pickupColors[index],
                size = segmentSizes[replaceIndex],
                spinOffset = nextRandomAngle()
            )
        }

        respawnPickup(index)
    }

    private fun keepHeadInsideArena() {
        if (headX < -ARENA_HALF_SIZE) {
            headX = -ARENA_HALF_SIZE
            heading = wrapAngle(-heading)
        } else if (headX > ARENA_HALF_SIZE) {
            headX = ARENA_HALF_SIZE
            heading = wrapAngle(-heading)
        }

        if (headZ < -ARENA_HALF_SIZE) {
            headZ = -ARENA_HALF_SIZE
            heading = wrapAngle(N64Wireframe3D.ANGLE_FULL / 2 - heading)
        } else if (headZ > ARENA_HALF_SIZE) {
            headZ = ARENA_HALF_SIZE
            heading = wrapAngle(N64Wireframe3D.ANGLE_FULL / 2 - heading)
        }
    }

    private fun respawnPickup(index: Int) {
        var x: Int
        var z: Int
        var attempts = 0
        do {
            x = randomBetween(-ARENA_HALF_SIZE + WORLD_SCALE, ARENA_HALF_SIZE - WORLD_SCALE)
            z = randomBetween(-ARENA_HALF_SIZE + WORLD_SCALE, ARENA_HALF_SIZE - WORLD_SCALE)
            attempts += 1
        } while ((distanceSquaredToHead(x, z) < PICKUP_SAFE_DISTANCE_SQUARED) && attempts < 8)

        pickupX[index] = x
        pickupZ[index] = z
        pickupShapeTypes[index] = nextRandom8() % 3
        pickupColors[index] = PICKUP_COLORS[nextRandom8() % PICKUP_COLORS.size]
        pickupPhases[index] = nextRandomAngle()
    }

    private fun setSegment(index: Int, shapeType: Int, color: Int, size: Int, spinOffset: Int) {
        segmentShapeTypes[index] = shapeType
        segmentColors[index] = color
        segmentSizes[index] = size
        segmentSpinOffsets[index] = spinOffset
    }

    private fun distanceSquaredToHead(x: Int, z: Int): Int {
        val dx = x - headX
        val dz = z - headZ
        return dx * dx + dz * dz
    }

    private fun randomBetween(min: Int, max: Int): Int {
        val span = max - min + 1
        return min + (nextRandom16() % span)
    }

    private fun nextRandom16(): Int {
        rng = rng * 1_103_515_245 + 12_345
        return (rng ushr 8) and 0xFFFF
    }

    private fun nextRandom8(): Int = nextRandom16() and 0xFF

    private fun nextRandomAngle(): Int = nextRandom16() and (N64Wireframe3D.ANGLE_FULL - 1)

    private fun pulseColor(color: Int, seed: Int): Int {
        val amount = 18 + ((seed * 7) and 31)
        return rgba(
            red(color) + amount,
            green(color) + amount,
            blue(color) + amount,
            alpha(color)
        )
    }

    private fun wrapAngle(angle: Int): Int = angle and (N64Wireframe3D.ANGLE_FULL - 1)

    private companion object {
        const val WORLD_SCALE = N64Wireframe3D.WORLD_SCALE
        const val STARTING_SEGMENTS = 5
        const val MAX_SEGMENTS = 12
        const val PICKUP_COUNT = 3
        const val MAX_HISTORY = 180
        const val SEGMENT_HISTORY_SPACING = 9
        const val HISTORY_STEP = 27
        const val ARENA_HALF_SIZE = 7 * WORLD_SCALE
        const val GRID_STEP = 2 * WORLD_SCALE
        const val HEAD_Y = 159
        const val PICKUP_Y = 220
        const val PICKUP_SIZE = 184
        const val PICKUP_BOB_HEIGHT = 46
        const val SEGMENT_BOB_HEIGHT = 18
        const val CONNECTOR_DROP = 67
        const val PICKUP_RADIUS = 194
        const val PICKUP_SAFE_DISTANCE_SQUARED = 10 * WORLD_SCALE * WORLD_SCALE
        const val TURN_SPEED = 12
        const val CRUISE_SPEED = 27
        const val BOOST_SPEED = 42
        const val SLOW_SPEED = 14
        const val CAMERA_ORBIT_SPEED = 6
        const val CAMERA_PITCH = -100
        const val CAMERA_DISTANCE = 12 * WORLD_SCALE + WORLD_SCALE / 2
        const val PROJECTION_DISTANCE = 122

        val PICKUP_COLORS = intArrayOf(
            rgba(255, 75, 122),
            rgba(75, 232, 255),
            rgba(255, 214, 78),
            rgba(132, 255, 124),
            rgba(194, 116, 255),
            rgba(255, 142, 72)
        )
    }
}

internal fun rgba(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
    return clampColor(red) or
        (clampColor(green) shl 8) or
        (clampColor(blue) shl 16) or
        (clampColor(alpha) shl 24)
}

private fun clampColor(value: Int): Int {
    return when {
        value < 0 -> 0
        value > 255 -> 255
        else -> value
    }
}

private fun red(color: Int): Int = color and 0xFF

private fun green(color: Int): Int = (color shr 8) and 0xFF

private fun blue(color: Int): Int = (color shr 16) and 0xFF

private fun alpha(color: Int): Int = (color shr 24) and 0xFF
