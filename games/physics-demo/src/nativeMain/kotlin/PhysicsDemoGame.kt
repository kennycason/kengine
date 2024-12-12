
import com.kengine.Game
import com.kengine.geometry.useGeometryContext
import com.kengine.input.mouse.useMouseContext
import com.kengine.math.Rect
import com.kengine.math.Vec2
import com.kengine.physics.Body
import com.kengine.physics.BodyType
import com.kengine.physics.PhysicsWorld
import com.kengine.physics.Shape
import com.kengine.physics.usePhysicsContext
import com.kengine.sdl.useSDLContext
import kotlin.random.Random

class PhysicsDemoGame : Game {
    private val world = PhysicsWorld()
    private val balls = mutableListOf<PhysicsObject>()
    private val boxes = mutableListOf<PhysicsObject>()
    private val boundaries = mutableListOf<PhysicsObject>()

    init {
        useSDLContext {
            // Create boundaries
            createBoundary(0.0, screenHeight - 20.0, screenWidth.toDouble(), 20.0) // Ground
            createBoundary(-20.0, 0.0, 20.0, screenHeight.toDouble()) // Left wall
            createBoundary(screenWidth.toDouble(), 0.0, 20.0, screenHeight.toDouble()) // Right wall

            // Create some initial objects
            repeat(10) {
                createRandomBall()
                createRandomBox()
            }
        }
    }

    private fun createBoundary(x: Double, y: Double, width: Double, height: Double) {
        usePhysicsContext {
            val body = world.createBody(type = BodyType.STATIC)
            body.position = Vec2(x + width/2, y + height/2)
            val shape = world.createBox(body, Rect(0.0, 0.0, width, height))
            shape.friction = 0.5
            shape.elasticity = 0.5
            boundaries.add(PhysicsObject(body, shape))
        }
    }

    private fun createRandomBall() {
        useSDLContext {
            usePhysicsContext {
                val body = world.createBody(type = BodyType.DYNAMIC, mass = 1.0)
                body.position = Vec2(
                    x = Random.nextDouble(50.0, screenWidth - 50.0),
                    y = Random.nextDouble(0.0, 100.0)
                )

                val radius = Random.nextDouble(10.0, 20.0)
                val shape = world.createCircle(body, radius)
                shape.friction = 0.5
                shape.elasticity = 0.8

                balls.add(PhysicsObject(body, shape))
            }
        }
    }

    private fun createRandomBox() {
        useSDLContext {
            usePhysicsContext {
                val body = world.createBody(type = BodyType.DYNAMIC, mass = 1.0)
                body.position = Vec2(
                    x = Random.nextDouble(50.0, screenWidth - 50.0),
                    y = Random.nextDouble(0.0, 100.0)
                )

                val size = Random.nextDouble(20.0, 40.0)
                val shape = world.createBox(body, Rect(0.0, 0.0, size, size))
                shape.friction = 0.5
                shape.elasticity = 0.6

                boxes.add(PhysicsObject(body, shape))
            }
        }
    }

    override fun update() {
        usePhysicsContext {
            step(1.0/60.0)

            // Spawn new objects on mouse click
            useMouseContext {
                if (mouse.isLeftPressed()) {
                    createRandomBall()
                }
                if (mouse.isRightPressed()) {
                    createRandomBox()
                }
            }
        }
    }

    override fun draw() {
        useSDLContext {
            fillScreen(0x22u, 0x22u, 0x22u) // Dark background

            useGeometryContext {
                // Draw boundaries
                boundaries.forEach { obj ->
                    when (obj.shape) {
                        is Shape.Box -> {
                            val pos = obj.body.position
                            val rect = (obj.shape as Shape.Box).getRect()
                            fillRectangle(
                                pos.x.toInt() - (rect.w/2).toInt(),
                                pos.y.toInt() - (rect.h/2).toInt(),
                                rect.w.toInt(),
                                rect.h.toInt(),
                                0x88u, 0x88u, 0x88u, 0xFFu
                            )
                        }
                        else -> {} // We only use boxes for boundaries
                    }
                }

                // Draw balls
                balls.forEach { obj ->
                    when (obj.shape) {
                        is Shape.Circle -> {
                            val pos = obj.body.position
                            fillCircle(
                                pos.x.toInt(),
                                pos.y.toInt(),
                                obj.shape.radius.toInt(),
                                0xFFu, 0x44u, 0x44u, 0xFFu
                            )
                        }
                        else -> {} // Should never happen
                    }
                }

                // Draw boxes
                boxes.forEach { obj ->
                    when (obj.shape) {
                        is Shape.Box -> {
                            val pos = obj.body.position
                            val rect = (obj.shape as Shape.Box).getRect()
                            fillRectangle(
                                pos.x.toInt() - (rect.w/2).toInt(),
                                pos.y.toInt() - (rect.h/2).toInt(),
                                rect.w.toInt(),
                                rect.h.toInt(),
                                0x44u, 0x44u, 0xFFu, 0xFFu
                            )
                        }
                        else -> {} // Should never happen
                    }
                }
            }

            flipScreen()
        }
    }

    override fun cleanup() {
        // Physics context cleanup will handle the space
    }
}

// Helper class to keep body and shape together
private class PhysicsObject(
    val body: Body,
    val shape: Shape
)

// Extension to get the rect from a box shape
private fun Shape.Box.getRect(): Rect {
    // You'll need to add appropriate Chipmunk API calls to get the box dimensions
    // This is just a placeholder - implement based on available Chipmunk functions
    return Rect(0.0, 0.0, 20.0, 20.0)
}