
import com.kengine.Game
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.map.tiled.TiledMapLoader
import com.kengine.math.Vec2
import com.kengine.particle.Effects
import com.kengine.particle.Particle
import com.kengine.sdl.getSDLContext
import com.kengine.sdl.useSDLContext
import com.kengine.ui.getViewContext
import com.kengine.ui.useView
import kotlin.random.Random

class HelloWorldGame : Game {
    private val bulbasaur = BulbasaurEntity()
    private val pidgies = List(size = 25) { PingPongPidgeyEntity() }
    private val scytherEntity = ScytherEntity()
    private val tiledMap = TiledMapLoader().loadMap("assets/maps/simple_map.tmj")
        .also { it.p.set(480.0, 0.0) }
    val particles = mutableListOf<Particle>()

    val profile = useView(
        id = "profileBar",
        x = 0.0,
        y = getSDLContext().screenHeight - 50.0,
        w = 300.0,
        h = 50.0,
        padding = 5.0,
        spacing = 5.0,
        bgColor = Color.red
    ) {
        view(
            id = "leftBox",
            w = 40.0,
            h = 40.0,
            bgColor = Color.orange
        )
        view(
            id = "middleBox",
            w = 200.0,
            h = 40.0,
            bgColor = Color.yellow
        )
        view(
            id = "rightBox",
            w = 40.0,
            h = 40.0,
            bgColor = Color.green,
            bgImage = Sprite.fromFilePath("assets/sprites/pokeball.bmp")
        )
    }

    init {
        getSDLContext().enableBlendedMode()

        repeat(250) {
            particles.add(
                Particle(
                    position = Vec2(250.0, 250.0),
                    velocity = Vec2(
                        (Random.nextDouble() - 0.5) * 200,
                        (Random.nextDouble() - 0.5) * 200
                    ),
                    color = Color(0xFFu, 0x77u, 0x0u, 0xFFu), // Orange burst
                    lifetime = Random.nextDouble() * 4.0,
                    behaviors = listOf(Effects.smoke, Effects.rainbow)
                )
            )
        }
    }

    override fun update() {
        pidgies.forEach {
            it.update()
        }
        scytherEntity.update()
        bulbasaur.update()

        particles.forEach { it.update() }
        particles.removeAll { it.age > it.lifetime }

    }

    override fun draw() {
        useSDLContext {
            // clear screen
            fillScreen(0xFFu, 0xFFu, 0xFFu)

            tiledMap.draw()

            getViewContext().render()

            useGeometryContext {
                // basic shapes
                drawRectangle(16, 16, 16, 16, 0xFFu, 0xFFu, 0xFFu, 0xFFu)
                fillRectangle(16 + 32, 16, 16, 16, 0xFFu, 0x00u, 0x00u, 0xFFu)
                drawCircle(16 + 80, 24, 16, 0x00u, 0xFFu, 0x00u, 0xFFu)
                fillCircle(16 + 128, 24, 16, 0x00u, 0x00u, 0xFFu, 0xFFu)

                // draw overlapping, transparent red, blue, and green circles.
                fillCircle(screenWidth / 2 - 32, screenWidth / 2 - 16, 64, 0xFFu, 0x00u, 0x00u, 0x77u)
                fillCircle(screenWidth / 2 + 32, screenWidth / 2 - 16, 64, 0x00u, 0xFFu, 0x00u, 0x77u)
                fillCircle(screenWidth / 2, screenWidth / 2 + 38, 64, 0x00u, 0x00u, 0xFFu, 0x77u)

                // draw patter with pixels
                var r = 0u
                var g = 100u
                var b = 200u
                for (x in 0 until 32) {
                    for (y in 0 until 32) {
                        r = (r + 1u) % 0xFFu
                        g = (g + 2u) % 0xFFu
                        b = (b + 3u) % 0xFFu
                        drawPixel((x * 2) + 32, (y * 2) + 150, r.toUByte(), g.toUByte(), b.toUByte(), 0xFFu)
                    }
                }

                drawLine(150, 50, 300, 50, 0xFFu, 0u, 0u, 0xFFu)
                drawLine(160, 150, 310, 20, 0x57u, 0x23u, 0x5Eu, 0xFFu)
            }

            particles.forEach { it.draw() }

            // draw sprite entities
            pidgies.forEach {
                it.draw()
            }
            scytherEntity.draw()
            bulbasaur.draw()

            flipScreen()
        }

    }

    override fun cleanup() {
        bulbasaur.cleanup()
        pidgies.forEach { it.cleanup() }
        particles.clear()
        particles.forEach { cleanup() }
        particles.clear()
        scytherEntity.cleanup()
        profile.cleanup()
    }

}
