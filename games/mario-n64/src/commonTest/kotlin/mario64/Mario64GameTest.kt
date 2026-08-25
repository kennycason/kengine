package mario64

import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderCommandBuffer
import com.kengine.render.RenderCommandType
import com.kengine.render.RenderContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mario64GameTest {
    @Test
    fun worldModelHasRenderableGeometry() {
        val world = Mario64ModelAssets.battlefield
        assertTrue(world.name.isNotEmpty())
        assertTrue(world.vertexCount > 0, "world should have vertices")
        assertTrue(world.triangleCount > 0, "world should have triangles")
        assertEquals(0, world.vertices.size % Mario64BakedWorld.VERTEX_STRIDE)
        assertEquals(0, world.triangles.size % Mario64BakedWorld.TRIANGLE_FIELD_COUNT)
        assertTrue(world.colors.isNotEmpty(), "world should have material colors")
    }

    @Test
    fun worldModelVertexCountMatchesDaeSource() {
        val world = Mario64ModelAssets.battlefield
        assertEquals(1623, world.vertexCount, "identical DAE vertices should be reused")
        assertEquals(1100, world.triangleCount, "duplicates and quantized degenerates should be removed")
    }

    @Test
    fun worldMaterialsPreserveN64RenderLayers() {
        val modes = Mario64ModelAssets.battlefield.materialModes
        assertEquals(18, modes.size)
        assertEquals(15, modes.count { it == Mario64BakedWorld.MATERIAL_OPAQUE })
        assertEquals(2, modes.count { it == Mario64BakedWorld.MATERIAL_MASKED })
        assertEquals(1, modes.count { it == Mario64BakedWorld.MATERIAL_TRANSLUCENT_DECAL })
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
            triangles = intArrayOf(0, 1, 2, StaticTriangleGrid3D.FLAG_FLOOR),
            cellOffsets = intArrayOf(0, 1),
            cellTriangleIndices = intArrayOf(0),
            metadata = intArrayOf(-4096, -192, -4096, 4096, 2147, 4096, 1, 1, 8192)
        )

        assertEquals(978, collision.groundHeightAt(-2048, -2048))
        assertEquals(0, collision.lastSupportTriangle)
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
    fun drawFrameProducesWorldCommand() {
        val game = Mario64Game()
        val render = RenderContext(512)

        render.beginFrame(320, 240)
        game.draw(render)

        assertTrue(render.commandCount > 0, "draw should produce render commands")
        assertTrue(containsType(render, RenderCommandType.DRAW_WORLD_3D), "should emit DRAW_WORLD_3D command")
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
            485 + Mario64Game.CAMERA_EYE_HEIGHT,
            worldCommandField(render, RenderCommandBuffer.FIELD_Y)
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
}
