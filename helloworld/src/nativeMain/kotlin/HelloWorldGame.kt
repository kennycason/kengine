
import com.kengine.Game
import com.kengine.context.useContext
import com.kengine.geometry.GeometryContext
import com.kengine.sdl.SDLContext

class HelloWorldGame : Game {
    private val bulbasaur = BulbasaurEntity()
    private val pokeballs = List(size = 25) { BouncingPokeballEntity() }

    override fun update(elapsedSeconds: Double) {
        pokeballs.forEach {
            it.update(elapsedSeconds)
        }
        bulbasaur.update(elapsedSeconds)
    }

    override fun draw(elapsedSeconds: Double) {
        useContext(SDLContext.get()) {
            // clear screen
            fillScreen(0u, 0u, 0u)

            useContext(GeometryContext.get()) {
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

            // draw sprite entities
            pokeballs.forEach {
                it.draw(elapsedSeconds)
            }
            bulbasaur.draw(elapsedSeconds)

            flipScreen()
        }
    }

    override fun cleanup() {
        bulbasaur.cleanup()
        pokeballs.forEach { it.cleanup() }
    }

}
