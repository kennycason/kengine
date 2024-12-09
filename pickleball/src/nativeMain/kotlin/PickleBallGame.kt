
import com.kengine.Game
import com.kengine.geometry.useGeometryContext
import com.kengine.map.tiled.TiledMapLoader
import com.kengine.network.IPAddress
import com.kengine.network.NetworkConnection
import com.kengine.network.useNetworkContext
import com.kengine.sdl.useSDLContext

class PickleBallGame : Game {
//    private val pokeball =  PokeBall()

    override fun update() {
        // pokeball.update()
    }

    override fun draw() {
        useSDLContext {
            // clear screen
            fillScreen(0xFFu, 0xFFu, 0xFFu)

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

            }

            // draw sprite entities
         //   pokeball.draw()


            flipScreen()
        }
    }

    override fun cleanup() {
  //      pokeball.cleanup()
    }

}
