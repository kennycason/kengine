import com.kengine.Game
import com.kengine.geometry.useGeometryContext
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.input.mouse.useMouseContext
import com.kengine.log.Logging
import com.kengine.math.Rect
import com.kengine.math.Vec2
import com.kengine.physics.Body
import com.kengine.physics.PhysicsObject
import com.kengine.physics.Shape
import com.kengine.physics.usePhysicsContext
import com.kengine.sdl.getSDLContext
import com.kengine.sdl.useSDLContext
import kotlin.random.Random

class PhysicsDemoGame : Game, Logging {
    private val screenWidth = getSDLContext().screenWidth.toDouble()
    private val screenHeight = getSDLContext().screenHeight.toDouble()

    init {
        usePhysicsContext {
            // Create boundaries
            createBoundary(0.0, screenHeight - 20.0, screenWidth, 20.0)  // Ground
            createBoundary(-20.0, 0.0, 20.0, screenHeight)               // Left wall
            createBoundary(screenWidth, 0.0, 20.0, screenHeight)         // Right wall

            // Create initial objects
            repeat(10) {
                createRandomBall()
                createRandomBox()
            }
        }
    }

    private fun createBoundary(x: Double, y: Double, width: Double, height: Double) {
        usePhysicsContext {
            val body = Body.createStatic()
            body.position = Vec2(x + width / 2, y + height / 2)

            val shape = Shape.Box(body, Rect(0.0, 0.0, width, height))
            shape.friction = 0.8
            shape.elasticity = 0.5

            addObject(PhysicsObject(body, shape))
        }
    }

    private fun createRandomBall() {
        usePhysicsContext {
            val radius = Random.nextDouble(10.0, 40.0)
            val mass = Random.nextDouble(10.0)
            val moment = mass * radius * radius / 2

            val body = Body.createDynamic(mass = mass, moment = moment)
            body.position = Vec2(
                x = Random.nextDouble(50.0, screenWidth - 50.0),
                y = Random.nextDouble(0.0, 100.0)
            )

            val shape = Shape.Circle(body, radius)
            shape.friction = 0.5
            shape.elasticity = 0.8

            addObject(PhysicsObject(body, shape))
        }
    }

    private fun createRandomBox() {
        usePhysicsContext {
            val size = Random.nextDouble(20.0, 40.0)
            val mass = Random.nextDouble(1.0, 5.0)
            val moment = mass * (size * size + size * size) / 12.0

            val body = Body.createDynamic(mass = mass, moment = moment)
            body.position = Vec2(
                x = Random.nextDouble(50.0, screenWidth - 50.0),
                y = Random.nextDouble(0.0, 100.0)
            )
            body.angularVelocity = Random.nextDouble(-2.0, 2.0)

            val shape = Shape.Box(body, Rect(0.0, 0.0, size, size))
            shape.friction = 0.5
            shape.elasticity = 0.6

            addObject(PhysicsObject(body, shape))
        }
    }

    override fun update() {
        usePhysicsContext {
            step(1.0 / 60.0)

            useMouseContext {
                if (mouse.isLeftPressed()) {
                    createRandomBall()
                }
                if (mouse.isRightPressed()) {
                    createRandomBox()
                }
            }

            useKeyboardContext {
                if (keyboard.isSpacePressed()) {
                    clearDynamicObjects()
                }
                if (keyboard.isUpPressed()) {
                    gravity = gravity.copy(y = gravity.y + 5)
                }
                if (keyboard.isDownPressed()) {
                    gravity = gravity.copy(y = gravity.y - 5)
                }
            }
        }
    }

    override fun draw() {
        useSDLContext {
            fillScreen(0x22u, 0x22u, 0x22u)
            useGeometryContext {
                usePhysicsContext {
                    // Draw static objects (boundaries)
                    getStaticObjects().forEach(::drawShape)
                    // Draw dynamic objects
                    getDynamicObjects().forEach(::drawShape)
                }
            }
            flipScreen()
        }
    }

    private fun drawShape(obj: PhysicsObject) {
        useGeometryContext {
            when (val shape = obj.shape) {
                is Shape.Circle -> {
                    val pos = obj.body.position
                    fillCircle(
                        pos.x.toInt(),
                        pos.y.toInt(),
                        shape.radius.toInt(),
                        0xFFu, 0x44u, 0x44u, 0xFFu
                    )
                }
                is Shape.Box -> {
                    val pos = obj.body.position
                    val rect = shape.rect
                    fillRectangle(
                        pos.x.toInt() - (rect.w / 2).toInt(),
                        pos.y.toInt() - (rect.h / 2).toInt(),
                        rect.w.toInt(),
                        rect.h.toInt(),
                        0x44u, 0x44u, 0xFFu, 0xFFu
                    )
                }
                is Shape.Segment -> TODO()
            }
        }
    }

    override fun cleanup() {
        usePhysicsContext {
            clearAll()
        }
    }
}
