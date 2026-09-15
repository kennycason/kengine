package mario64

import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderCommandBuffer
import com.kengine.render.RenderCommandType
import com.kengine.render.RenderContext
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mario64GameTest {
    @Test
    fun worldModelHasRenderableGeometry() {
        assertTrue(Mario64ModelAssets.BATTLEFIELD_NAME.isNotEmpty())
        assertTrue(Mario64ModelAssets.BATTLEFIELD_VERTEX_COUNT > 0, "world should have vertices")
        assertTrue(Mario64ModelAssets.BATTLEFIELD_TRIANGLE_COUNT > 0, "world should have triangles")
        assertTrue(Mario64ModelAssets.BATTLEFIELD_MATERIAL_COUNT > 0, "world should have material colors")
        assertTrue(Mario64ModelAssets.BATTLEFIELD_TEXTURE_COUNT > 0, "world should have textures")
        assertEquals(666, Mario64ModelAssets.MARIO_VERTEX_COUNT, "animated Mario vertices should be compacted")
        assertEquals(989, Mario64ModelAssets.MARIO_TRIANGLE_COUNT)
        assertEquals(5, Mario64ModelAssets.MARIO_MATERIAL_COUNT)
        assertEquals(5, Mario64ModelAssets.MARIO_TEXTURE_COUNT, "equivalent texture materials should merge")
        assertEquals(7, Mario64ModelAssets.MARIO_POSE_COUNT)
        assertEquals(4, Mario64ModelAssets.MARIO_STATIC_CLAMPED_TEXTURE_COUNT)
    }

    @Test
    fun worldModelVertexCountMatchesDaeSource() {
        assertEquals(1623, Mario64ModelAssets.BATTLEFIELD_VERTEX_COUNT, "identical DAE vertices should be reused")
        assertEquals(1100, Mario64ModelAssets.BATTLEFIELD_TRIANGLE_COUNT, "duplicates and quantized degenerates should be removed")
    }

    @Test
    fun worldMaterialsPreserveN64RenderLayers() {
        assertEquals(18, Mario64ModelAssets.BATTLEFIELD_MATERIAL_COUNT)
        assertEquals(15, Mario64ModelAssets.BATTLEFIELD_OPAQUE_MATERIAL_COUNT)
        assertEquals(2, Mario64ModelAssets.BATTLEFIELD_MASKED_MATERIAL_COUNT)
        assertEquals(1, Mario64ModelAssets.BATTLEFIELD_TRANSLUCENT_DECAL_MATERIAL_COUNT)
    }

    @Test
    fun collisionGridHasValidCompactData() {
        val collision = Mario64CollisionAssets.battlefield
        assertEquals(0, collision.vertices.size % StaticTriangleGrid3D.VERTEX_STRIDE)
        assertEquals(0, collision.triangles.size % StaticTriangleGrid3D.TRIANGLE_STRIDE)
        assertEquals(16, collision.gridWidth)
        assertEquals(16, collision.gridDepth)
        assertEquals(512, collision.cellSize)
        assertEquals(257, collision.cellOffsets.size)
        assertEquals(0, collision.cellOffsets.first())
        assertEquals(collision.cellTriangleIndices.size, collision.cellOffsets.last())
        assertTrue(collision.cellTriangleIndices.all { it in 0 until collision.triangleCount })

        var triangle = 0
        while (triangle < collision.triangleCount) {
            val base = triangle * StaticTriangleGrid3D.TRIANGLE_STRIDE
            val normalX = collision.triangles[base + StaticTriangleGrid3D.TRIANGLE_NORMAL_X]
            val normalY = collision.triangles[base + StaticTriangleGrid3D.TRIANGLE_NORMAL_Y]
            val normalZ = collision.triangles[base + StaticTriangleGrid3D.TRIANGLE_NORMAL_Z]
            assertTrue(abs(normalX) <= StaticTriangleGrid3D.NORMAL_SCALE)
            assertTrue(abs(normalY) <= StaticTriangleGrid3D.NORMAL_SCALE)
            assertTrue(abs(normalZ) <= StaticTriangleGrid3D.NORMAL_SCALE)
            if ((collision.triangles[base + StaticTriangleGrid3D.TRIANGLE_FLAGS] and
                    StaticTriangleGrid3D.FLAG_FLOOR) == 0
            ) {
                assertTrue(normalX != 0 || normalZ != 0, "wall must have a horizontal normal")
            }
            triangle += 1
        }
    }

    @Test
    fun groundQueryFindsExpectedSpawnSurface() {
        val collision = Mario64CollisionAssets.battlefield
        val groundY = collision.groundHeightAt(-3000, -3000)

        assertEquals(485, groundY)
        assertTrue(collision.lastCandidateCount in 1..66)
        assertTrue(collision.lastSupportTriangle >= 0)
        assertEquals(
            StaticTriangleGrid3D.NO_GROUND,
            collision.groundHeightAt(collision.maxX + 1, collision.maxZ + 1)
        )
    }

    @Test
    fun groundQueryScalesLargeSlopedTriangleWithoutOverflow() {
        val collision = StaticTriangleGrid3D(
            vertices = intArrayOf(
                -4096, -192, -4096,
                4096, 2147, -4096,
                -4096, 2147, 4096
            ),
            triangles = intArrayOf(
                0, 1, 2, StaticTriangleGrid3D.FLAG_FLOOR,
                0, -StaticTriangleGrid3D.NORMAL_SCALE, 0, 0
            ),
            cellOffsets = intArrayOf(0, 1),
            cellTriangleIndices = intArrayOf(0),
            metadata = intArrayOf(-4096, -192, -4096, 4096, 2147, 4096, 1, 1, 8192)
        )

        assertEquals(978, collision.groundHeightAt(-2048, -2048))
        assertEquals(0, collision.lastSupportTriangle)
    }

    @Test
    fun smallFloorProbeBridgesAQuantizedSeam() {
        val collision = createFloorAndWallGrid()

        assertEquals(
            StaticTriangleGrid3D.NO_GROUND,
            collision.groundHeightAt(-260, 0)
        )
        assertEquals(
            0,
            collision.groundHeightNear(-260, 0, probeDistance = 8)
        )
        assertTrue(collision.lastSupportTriangle >= 0)
    }

    @Test
    fun wallCirclePushesOutAndPreservesTangentMovement() {
        val collision = createFloorAndWallGrid()

        assertTrue(collision.resolveWalls(x = 40, sampleY = 60, z = 0, radius = 50))
        assertEquals(50, collision.lastResolvedX)
        assertEquals(0, collision.lastResolvedZ)
        assertTrue(collision.lastWallTriangle >= 0)

        val body = KinematicGroundBody3D(collision, spawnX = 80, spawnZ = -100)
        body.step(moveX = -60, moveZ = 80, jumpPressed = false)

        assertTrue(body.grounded)
        assertTrue(body.x >= 50, "body radius should remain outside the x=0 wall")
        assertEquals(-20, body.z, "movement tangent to the wall should be preserved")
        assertTrue(body.wallCollisionCount > 0)
    }

    @Test
    fun normalWalkIsSlowerAndBHoldsThePreviousPace() {
        val walkGame = Mario64Game()
        val walkInput = InputState()
        walkInput.set(InputButton.DPAD_UP)
        walkGame.update(walkInput)

        val runGame = Mario64Game()
        val runInput = InputState()
        runInput.set(InputButton.DPAD_UP)
        runInput.set(InputButton.B)
        runGame.update(runInput)

        val walkX = walkGame.bodyX + 3000
        val walkZ = walkGame.bodyZ + 3000
        val runX = runGame.bodyX + 3000
        val runZ = runGame.bodyZ + 3000
        val walkDistanceSquared = walkX * walkX + walkZ * walkZ
        val runDistanceSquared = runX * runX + runZ * runZ

        assertTrue(runDistanceSquared > walkDistanceSquared)
        assertTrue(maxOf(abs(runX), abs(runZ)) <= 24)
    }

    @Test
    fun analogStickMagnitudeControlsWalkingSpeed() {
        val halfGame = Mario64Game()
        val halfInput = InputState()
        halfInput.setLeftStick(0, InputState.ANALOG_AXIS_MAX / 2)
        halfGame.update(halfInput)

        val fullGame = Mario64Game()
        val fullInput = InputState()
        fullInput.setLeftStick(0, InputState.ANALOG_AXIS_MAX)
        fullGame.update(fullInput)

        val halfX = halfGame.bodyX + 3000
        val halfZ = halfGame.bodyZ + 3000
        val fullX = fullGame.bodyX + 3000
        val fullZ = fullGame.bodyZ + 3000
        val halfDistanceSquared = halfX * halfX + halfZ * halfZ
        val fullDistanceSquared = fullX * fullX + fullZ * fullZ

        assertTrue(halfDistanceSquared > 0)
        assertTrue(halfDistanceSquared < fullDistanceSquared)
    }

    @Test
    fun groundBodySpawnsJumpsAndLandsWithoutAllocationState() {
        val body = KinematicGroundBody3D(
            collision = Mario64CollisionAssets.battlefield,
            spawnX = -3000,
            spawnZ = -3000
        )

        assertEquals(485, body.y)
        assertTrue(body.grounded)
        assertTrue(body.supportTriangle >= 0)

        body.step(moveX = 0, moveZ = 0, jumpPressed = true)
        assertTrue(!body.grounded)
        assertTrue(body.y > 485)
        assertTrue(body.verticalVelocity > 0)

        var updates = 0
        while (!body.grounded && updates < 64) {
            body.step(moveX = 0, moveZ = 0, jumpPressed = false)
            updates += 1
        }
        assertTrue(body.grounded, "jump should land on the starting floor")
        assertEquals(485, body.y)
        assertEquals(0, body.verticalVelocity)
    }

    @Test
    fun leavingTheWorldFallsAndReturnsToSafeSpawn() {
        val body = KinematicGroundBody3D(
            collision = Mario64CollisionAssets.battlefield,
            spawnX = -3000,
            spawnZ = -3000
        )

        body.step(moveX = 9000, moveZ = 0, jumpPressed = false)
        assertTrue(!body.grounded)
        assertEquals(6000, body.x)

        var updates = 0
        while (!body.grounded && updates < 128) {
            body.step(moveX = 0, moveZ = 0, jumpPressed = false)
            updates += 1
        }
        assertTrue(body.grounded, "a far fall should return to the safe spawn")
        assertEquals(-3000, body.x)
        assertEquals(485, body.y)
        assertEquals(-3000, body.z)
    }

    @Test
    fun holdingJumpDoesNotStartAnotherJumpAfterLanding() {
        val game = Mario64Game()
        val input = InputState()
        input.set(InputButton.A)

        var updates = 0
        while (updates < 64) {
            game.update(input)
            updates += 1
        }

        assertTrue(game.isGrounded)
        assertEquals(485, game.bodyY)
        assertEquals(0, game.verticalVelocity)
    }

    @Test
    fun drawFrameProducesWorldAndMarioCommands() {
        val game = Mario64Game()
        val render = RenderContext(512)

        render.beginFrame(320, 240)
        game.draw(render)

        assertTrue(render.commandCount > 0, "draw should produce render commands")
        assertTrue(containsType(render, RenderCommandType.DRAW_WORLD_3D), "should emit DRAW_WORLD_3D command")
        assertTrue(containsType(render, RenderCommandType.DRAW_MESH_3D), "should emit Mario mesh command")
    }

    @Test
    fun drawWorldCommandContainsCorrectMeshId() {
        val game = Mario64Game()
        val render = RenderContext(512)

        render.beginFrame(320, 240)
        game.draw(render)

        var found = false
        var index = 0
        while (index < render.commandCount) {
            if (render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == RenderCommandType.DRAW_WORLD_3D) {
                val meshId = render.commandField(index, RenderCommandBuffer.FIELD_COLOR2)
                assertEquals(Mario64Game.BATTLEFIELD_MESH_ID, meshId, "mesh ID should match")
                found = true
            }
            index += 1
        }
        assertTrue(found, "should find DRAW_WORLD_3D command")
    }

    @Test
    fun horizontalCameraButtonsOrbitInMario64Direction() {
        val leftGame = Mario64Game()
        val leftInput = InputState()
        leftInput.set(InputButton.C_LEFT)
        leftGame.update(leftInput)

        val rightGame = Mario64Game()
        val rightInput = InputState()
        rightInput.set(InputButton.C_RIGHT)
        rightGame.update(rightInput)

        assertEquals(110, drawWorldField(leftGame, RenderCommandBuffer.FIELD_HEIGHT))
        assertEquals(146, drawWorldField(rightGame, RenderCommandBuffer.FIELD_HEIGHT))
    }

    @Test
    fun drawMarioCommandUsesPlayerTransform() {
        val game = Mario64Game()
        val render = RenderContext(512)

        render.beginFrame(320, 240)
        game.draw(render)

        assertEquals(
            Mario64Game.MARIO_MESH_ID,
            commandFieldForType(render, RenderCommandType.DRAW_MESH_3D, RenderCommandBuffer.FIELD_COLOR)
        )
        assertEquals(
            game.bodyX,
            commandFieldForType(render, RenderCommandType.DRAW_MESH_3D, RenderCommandBuffer.FIELD_X)
        )
        assertEquals(
            game.bodyY,
            commandFieldForType(render, RenderCommandType.DRAW_MESH_3D, RenderCommandBuffer.FIELD_Y)
        )
        assertEquals(
            game.bodyZ,
            commandFieldForType(render, RenderCommandType.DRAW_MESH_3D, RenderCommandBuffer.FIELD_WIDTH)
        )
        assertEquals(
            1000,
            commandFieldForType(render, RenderCommandType.DRAW_MESH_3D, RenderCommandBuffer.FIELD_COLOR2)
        )
    }

    @Test
    fun movementAndAirStateSelectBakedMarioPoses() {
        val walkGame = Mario64Game()
        val walkInput = InputState()
        walkInput.set(InputButton.DPAD_UP)
        walkGame.update(walkInput)
        assertEquals(Mario64Game.MARIO_WALK_A_MESH_ID, drawMarioMeshId(walkGame))
        repeat(4) { walkGame.update(walkInput) }
        assertEquals(Mario64Game.MARIO_WALK_B_MESH_ID, drawMarioMeshId(walkGame))

        val runGame = Mario64Game()
        val runInput = InputState()
        runInput.set(InputButton.DPAD_UP)
        runInput.set(InputButton.B)
        runGame.update(runInput)
        assertEquals(Mario64Game.MARIO_RUN_A_MESH_ID, drawMarioMeshId(runGame))
        repeat(2) { runGame.update(runInput) }
        assertEquals(Mario64Game.MARIO_RUN_B_MESH_ID, drawMarioMeshId(runGame))

        val jumpGame = Mario64Game()
        val jumpInput = InputState()
        jumpInput.set(InputButton.A)
        jumpGame.update(jumpInput)
        assertEquals(Mario64Game.MARIO_JUMP_MESH_ID, drawMarioMeshId(jumpGame))
        jumpInput.reset()
        while (jumpGame.verticalVelocity > 0) {
            jumpGame.update(jumpInput)
        }
        assertEquals(Mario64Game.MARIO_FALL_MESH_ID, drawMarioMeshId(jumpGame))
    }

    @Test
    fun startButtonResetsPosition() {
        val game = Mario64Game()
        val input = InputState()

        assertEquals(-3000, game.bodyX)
        assertEquals(485, game.bodyY)
        assertEquals(-3000, game.bodyZ)
        assertTrue(game.isGrounded)

        input.set(InputButton.DPAD_UP)
        game.update(input)
        game.update(input)
        game.update(input)

        input.reset()
        input.set(InputButton.START)
        game.update(input)

        assertEquals(-3000, game.bodyX)
        assertEquals(485, game.bodyY)
        assertEquals(-3000, game.bodyZ)
        assertEquals(0, game.verticalVelocity)
        assertTrue(game.isGrounded)
        assertTrue(game.supportTriangle >= 0)

        val render = RenderContext(512)
        render.beginFrame(320, 240)
        game.draw(render)

        assertEquals(0, render.droppedCommandCount)
        assertEquals(
            485 + Mario64Game.CAMERA_TARGET_HEIGHT -
                trigMul(Mario64Game.CAMERA_DISTANCE, sinAngle(-48)),
            worldCommandField(render, RenderCommandBuffer.FIELD_Y)
        )
    }

    private fun drawWorldField(game: Mario64Game, field: Int): Int {
        val render = RenderContext(512)
        render.beginFrame(320, 240)
        game.draw(render)
        return worldCommandField(render, field)
    }

    private fun drawMarioMeshId(game: Mario64Game): Int {
        val render = RenderContext(512)
        render.beginFrame(320, 240)
        game.draw(render)
        return commandFieldForType(
            render,
            RenderCommandType.DRAW_MESH_3D,
            RenderCommandBuffer.FIELD_COLOR
        )
    }

    private fun worldCommandField(render: RenderContext, field: Int): Int {
        var index = 0
        while (index < render.commandCount) {
            if (render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == RenderCommandType.DRAW_WORLD_3D) {
                return render.commandField(index, field)
            }
            index += 1
        }
        error("DRAW_WORLD_3D command not found")
    }

    private fun containsType(render: RenderContext, type: Int): Boolean {
        var index = 0
        while (index < render.commandCount) {
            if (render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == type) {
                return true
            }
            index += 1
        }
        return false
    }

    private fun commandFieldForType(render: RenderContext, type: Int, field: Int): Int {
        var index = 0
        while (index < render.commandCount) {
            if (render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == type) {
                return render.commandField(index, field)
            }
            index += 1
        }
        error("render command $type not found")
    }

    private fun createFloorAndWallGrid(): StaticTriangleGrid3D {
        return StaticTriangleGrid3D(
            vertices = intArrayOf(
                -256, 0, -256,
                256, 0, -256,
                -256, 0, 256,
                256, 0, 256,
                0, 200, -256,
                0, 200, 256,
                0, 0, -256,
                0, 0, 256
            ),
            triangles = intArrayOf(
                0, 1, 2, StaticTriangleGrid3D.FLAG_FLOOR,
                0, StaticTriangleGrid3D.NORMAL_SCALE, 0, 0,
                1, 3, 2, StaticTriangleGrid3D.FLAG_FLOOR,
                0, StaticTriangleGrid3D.NORMAL_SCALE, 0, 0,
                6, 4, 7, 0,
                StaticTriangleGrid3D.NORMAL_SCALE, 0, 0, 0,
                4, 5, 7, 0,
                StaticTriangleGrid3D.NORMAL_SCALE, 0, 0, 0
            ),
            cellOffsets = intArrayOf(0, 4),
            cellTriangleIndices = intArrayOf(0, 1, 2, 3),
            metadata = intArrayOf(-256, 0, -256, 256, 200, 256, 1, 1, 512)
        )
    }
}
